import { Env, MessageRow, AliasRow, SettingRow } from './types';
import { parseRawEmail } from './mime';
import { extractVerificationCode } from './codeExtractor';

const CORS_HEADERS: Record<string, string> = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, x-auth-token',
  'Access-Control-Max-Age': '86400',
};

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...CORS_HEADERS,
    },
  });
}

async function timingSafeEqualStr(a: string, b: string): Promise<boolean> {
  const enc = new TextEncoder();
  const bufA = enc.encode(a);
  const bufB = enc.encode(b);
  if (bufA.byteLength !== bufB.byteLength) {
    return false;
  }
  return crypto.subtle.timingSafeEqual(bufA, bufB);
}

async function isAuthorized(request: Request, env: Env): Promise<boolean> {
  if (!env.ADMIN_TOKEN) {
    return false;
  }
  const token = request.headers.get('x-auth-token');
  if (!token) {
    return false;
  }
  return timingSafeEqualStr(token, env.ADMIN_TOKEN);
}

export default {
  // Cloudflare Email Routing Catch-all handler
  async email(message: ForwardableEmailMessage, env: Env, ctx: ExecutionContext): Promise<void> {
    try {
      const parsed = await parseRawEmail(message.raw);
      const id = crypto.randomUUID();
      const fromAddr = parsed.from || message.from;
      const toAddr = parsed.to || message.to;
      const subject = parsed.subject || '(No Subject)';
      const text = parsed.text || '';
      const html = parsed.html || '';
      const code = extractVerificationCode(subject, text, html);
      const date = parsed.date || new Date().toUTCString();
      const createdAt = new Date().toISOString();

      await env.DB.prepare(
        `INSERT INTO messages (id, from_addr, to_addr, subject, text, html, code, date, created_at, read)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)`
      )
        .bind(id, fromAddr, toAddr, subject, text, html, code, date, createdAt)
        .run();
    } catch (err: unknown) {
      console.error('Failed to process incoming email:', err);
    }
  },

  // HTTP API handler
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }

    const url = new URL(request.url);
    const pathname = url.pathname;

    // Public health check
    if (pathname === '/health' && request.method === 'GET') {
      try {
        const msgCountRes = await env.DB.prepare('SELECT COUNT(*) as count FROM messages').first<{ count: number }>();
        const aliasCountRes = await env.DB.prepare('SELECT COUNT(*) as count FROM aliases').first<{ count: number }>();
        const domainSetting = await env.DB.prepare("SELECT value FROM settings WHERE key = 'domain'").first<{ value: string }>();
        const domain = domainSetting?.value || '';
        return jsonResponse({
          ok: true,
          messages: msgCountRes?.count ?? 0,
          aliases: aliasCountRes?.count ?? 0,
          domain,
          resendConfigured: Boolean(env.RESEND_API_KEY),
          time: new Date().toISOString(),
        });
      } catch (err: unknown) {
        return jsonResponse(
          {
            ok: false,
            error: err instanceof Error ? err.message : String(err),
            resendConfigured: Boolean(env.RESEND_API_KEY),
            time: new Date().toISOString(),
          },
          500
        );
      }
    }

    // Public login
    if (pathname === '/api/login' && request.method === 'POST') {
      try {
        const body = (await request.json()) as { password?: string };
        const password = body.password || '';
        if (env.ADMIN_TOKEN && (await timingSafeEqualStr(password, env.ADMIN_TOKEN))) {
          return jsonResponse({ ok: true });
        }
        return jsonResponse({ ok: false, error: 'Invalid password' }, 401);
      } catch {
        return jsonResponse({ ok: false, error: 'Malformed request' }, 400);
      }
    }

    // Protected /api/* endpoints
    if (pathname.startsWith('/api/')) {
      const authorized = await isAuthorized(request, env);
      if (!authorized) {
        return jsonResponse({ ok: false, error: 'Unauthorized' }, 401);
      }

      // GET /api/messages?search=&to=
      if (pathname === '/api/messages' && request.method === 'GET') {
        const search = url.searchParams.get('search')?.trim() || '';
        const to = url.searchParams.get('to')?.trim() || '';

        let query = 'SELECT id, from_addr, to_addr, subject, text, code, date, created_at, read FROM messages';
        const params: unknown[] = [];
        const conditions: string[] = [];

        if (to) {
          conditions.push('(to_addr = ? OR to_addr LIKE ?)');
          params.push(to, `${to}%`);
        }

        if (search) {
          conditions.push('(subject LIKE ? OR from_addr LIKE ? OR to_addr LIKE ? OR text LIKE ?)');
          const searchPattern = `%${search}%`;
          params.push(searchPattern, searchPattern, searchPattern, searchPattern);
        }

        if (conditions.length > 0) {
          query += ' WHERE ' + conditions.join(' AND ');
        }

        query += ' ORDER BY created_at DESC LIMIT 200';

        const { results } = await env.DB.prepare(query).bind(...params).all<MessageRow>();

        const messages = (results || []).map((row) => {
          let code = row.code;
          if (!code && (row.text || row.subject)) {
            code = extractVerificationCode(row.subject, row.text, '');
          }
          return {
            id: row.id,
            from_addr: row.from_addr,
            to_addr: row.to_addr,
            subject: row.subject,
            text: (row.text || '').slice(0, 500),
            code,
            date: row.date,
            created_at: row.created_at,
            read: row.read,
          };
        });

        return jsonResponse({ ok: true, messages });
      }

      // Match /api/messages/:id
      const messageIdMatch = pathname.match(/^\/api\/messages\/([^/]+)$/);
      if (messageIdMatch) {
        const id = decodeURIComponent(messageIdMatch[1]);

        if (request.method === 'GET') {
          const row = await env.DB.prepare('SELECT * FROM messages WHERE id = ?').bind(id).first<MessageRow>();
          if (!row) {
            return jsonResponse({ ok: false, error: 'Message not found' }, 404);
          }

          // Mark as read
          if (row.read === 0) {
            await env.DB.prepare('UPDATE messages SET read = 1 WHERE id = ?').bind(id).run();
            row.read = 1;
          }

          // Re-validate code
          if (!row.code) {
            row.code = extractVerificationCode(row.subject, row.text, row.html);
          }

          return jsonResponse({ ok: true, message: row });
        }

        if (request.method === 'DELETE') {
          await env.DB.prepare('DELETE FROM messages WHERE id = ?').bind(id).run();
          return jsonResponse({ ok: true });
        }
      }

      // /api/aliases
      if (pathname === '/api/aliases') {
        if (request.method === 'GET') {
          const { results } = await env.DB.prepare('SELECT * FROM aliases ORDER BY created_at DESC').all<AliasRow>();
          return jsonResponse({ ok: true, aliases: results || [] });
        }

        if (request.method === 'POST') {
          try {
            const body = (await request.json()) as { prefix?: string };
            const prefix = (body.prefix || '').trim().toLowerCase();

            if (!prefix || !/^[a-z0-9._-]+$/i.test(prefix)) {
              return jsonResponse(
                { ok: false, error: 'Prefix may only contain letters, numbers, dots, hyphens, and underscores' },
                400
              );
            }

            const domainSetting = await env.DB.prepare("SELECT value FROM settings WHERE key = 'domain'").first<{ value: string }>();
            const domain = domainSetting?.value?.trim();
            if (!domain) {
              return jsonResponse({ ok: false, error: 'Set your domain in the app first.' }, 400);
            }
            const address = `${prefix}@${domain}`;

            // Check if already exists
            const existing = await env.DB.prepare('SELECT * FROM aliases WHERE prefix = ?').bind(prefix).first<AliasRow>();
            if (existing) {
              return jsonResponse({ ok: true, alias: existing });
            }

            const id = crypto.randomUUID();
            const createdAt = new Date().toISOString();

            await env.DB.prepare('INSERT INTO aliases (id, prefix, address, created_at) VALUES (?, ?, ?, ?)')
              .bind(id, prefix, address, createdAt)
              .run();

            return jsonResponse({
              ok: true,
              alias: {
                id,
                prefix,
                address,
                created_at: createdAt,
              },
            });
          } catch {
            return jsonResponse({ ok: false, error: 'Malformed request' }, 400);
          }
        }
      }

      // Match /api/aliases/:id
      const aliasIdMatch = pathname.match(/^\/api\/aliases\/([^/]+)$/);
      if (aliasIdMatch && request.method === 'DELETE') {
        const id = decodeURIComponent(aliasIdMatch[1]);
        await env.DB.prepare('DELETE FROM aliases WHERE id = ?').bind(id).run();
        return jsonResponse({ ok: true });
      }

      // /api/settings
      if (pathname === '/api/settings') {
        if (request.method === 'GET') {
          const { results } = await env.DB.prepare('SELECT * FROM settings').all<SettingRow>();
          const settings: Record<string, string> = {};
          for (const row of results || []) {
            settings[row.key] = row.value;
          }
          if (!settings.domain) {
            settings.domain = '';
          }
          if (!settings.catchAll) {
            settings.catchAll = '1';
          }
          return jsonResponse({ ok: true, settings });
        }

        if (request.method === 'POST') {
          try {
            const body = (await request.json()) as Record<string, string>;
            for (const [key, value] of Object.entries(body)) {
              await env.DB.prepare(
                'INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = ?'
              )
                .bind(key, String(value), String(value))
                .run();
            }
            return jsonResponse({ ok: true });
          } catch {
            return jsonResponse({ ok: false, error: 'Malformed request' }, 400);
          }
        }
      }

      // POST /api/test-inject
      if (pathname === '/api/test-inject' && request.method === 'POST') {
        try {
          let toAddr = '';
          try {
            const body = (await request.json()) as { to?: string };
            if (body && body.to) {
              toAddr = body.to.trim();
            }
          } catch {
            // body is optional
          }

          if (!toAddr) {
            const domainSetting = await env.DB.prepare("SELECT value FROM settings WHERE key = 'domain'").first<{ value: string }>();
            const domain = domainSetting?.value?.trim() || 'example.com';
            toAddr = `test@${domain}`;
          }

          const id = crypto.randomUUID();
          const fromAddr = 'service@example.com';
          const subject = 'Test verification code';
          const text = 'Hello,\n\nTest — your code is 123456\n\nThanks!';
          const html = '<div style="font-family:sans-serif;padding:16px;"><h3>ShardMail Test</h3><p>Hello,</p><p>Test — your code is <b>123456</b></p><p>Thanks!</p></div>';
          const code = '123456';
          const date = new Date().toUTCString();
          const createdAt = new Date().toISOString();

          await env.DB.prepare(
            `INSERT INTO messages (id, from_addr, to_addr, subject, text, html, code, date, created_at, read)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)`
          )
            .bind(id, fromAddr, toAddr, subject, text, html, code, date, createdAt)
            .run();

          return jsonResponse({
            ok: true,
            id,
            message: {
              id,
              from_addr: fromAddr,
              to_addr: toAddr,
              subject,
              text,
              code,
              date,
              created_at: createdAt,
              read: 0,
            },
          });
        } catch (err: unknown) {
          return jsonResponse({ ok: false, error: err instanceof Error ? err.message : String(err) }, 500);
        }
      }

      // POST /api/send
      if (pathname === '/api/send' && request.method === 'POST') {
        if (!env.RESEND_API_KEY) {
          return jsonResponse(
            { ok: false, error: 'RESEND_API_KEY secret is not configured on this worker' },
            400
          );
        }

        try {
          const body = (await request.json()) as {
            to?: string;
            from?: string;
            subject?: string;
            text?: string;
            html?: string;
          };

          const domainSetting = await env.DB.prepare("SELECT value FROM settings WHERE key = 'domain'").first<{ value: string }>();
          const domain = domainSetting?.value?.trim() || 'example.com';
          const from = body.from || `noreply@${domain}`;

          const resendResponse = await fetch('https://api.resend.com/emails', {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${env.RESEND_API_KEY}`,
              'Content-Type': 'application/json',
              'User-Agent': 'shardmail/1.0',
            },
            body: JSON.stringify({
              from,
              to: body.to,
              subject: body.subject || '(No Subject)',
              text: body.text,
              html: body.html,
            }),
          });

          const resendData = await resendResponse.json();
          if (!resendResponse.ok) {
            return jsonResponse({ ok: false, error: resendData }, resendResponse.status);
          }

          return jsonResponse({ ok: true, data: resendData });
        } catch (err: unknown) {
          return jsonResponse({ ok: false, error: err instanceof Error ? err.message : String(err) }, 500);
        }
      }
    }

    return jsonResponse({ ok: false, error: 'Not found' }, 404);
  },
};
