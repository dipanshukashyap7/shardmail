/**
 * Lightweight, zero-dependency MIME parser designed for Cloudflare Workers.
 * Low CPU, streaming-friendly, handles:
 * - RFC 5322 header parsing & folding
 * - RFC 2047 encoded words (=?UTF-8?B?...?= and =?UTF-8?Q?...?=)
 * - Multipart (alternative, mixed, related) with boundary parsing
 * - Content-Transfer-Encoding (base64, quoted-printable, 7bit/8bit)
 * - Charset decoding (UTF-8, ISO-8859-1, ASCII)
 */

export interface ParsedEmail {
  subject: string;
  from: string;
  to: string;
  date: string;
  text: string;
  html: string;
}

export async function parseRawEmail(rawStream: ReadableStream<Uint8Array>): Promise<ParsedEmail> {
  const arrayBuffer = await new Response(rawStream).arrayBuffer();
  const rawText = new TextDecoder('utf-8', { fatal: false, ignoreBOM: false }).decode(arrayBuffer);
  return parseMimeString(rawText);
}

export function parseMimeString(raw: string): ParsedEmail {
  const headerEndIndex = findHeaderEnd(raw);
  const headerSection = headerEndIndex !== -1 ? raw.slice(0, headerEndIndex) : raw;
  const bodySection = headerEndIndex !== -1 ? raw.slice(headerEndIndex).replace(/^(\r\n|\r|\n)/, '') : '';

  const headers = parseHeaders(headerSection);

  const subject = decodeHeader(headers.get('subject') || '(No Subject)');
  const from = decodeHeader(headers.get('from') || '');
  const to = decodeHeader(headers.get('to') || '');
  const date = headers.get('date') || new Date().toUTCString();
  const contentType = headers.get('content-type') || 'text/plain';
  const transferEncoding = headers.get('content-transfer-encoding') || '';

  const bodies = extractBodies(bodySection, contentType, transferEncoding);

  return {
    subject,
    from,
    to,
    date,
    text: bodies.text.trim(),
    html: bodies.html.trim()
  };
}

function findHeaderEnd(raw: string): number {
  let idx = raw.indexOf('\r\n\r\n');
  if (idx !== -1) return idx + 4;
  idx = raw.indexOf('\n\n');
  if (idx !== -1) return idx + 2;
  return -1;
}

function parseHeaders(headerSection: string): Map<string, string> {
  const headers = new Map<string, string>();
  const lines = headerSection.split(/\r?\n/);
  let currentKey = '';
  let currentValue = '';

  for (const line of lines) {
    if (/^[ \t]/.test(line)) {
      // Continuation of previous header
      if (currentKey) {
        currentValue += ' ' + line.trim();
      }
    } else {
      if (currentKey) {
        headers.set(currentKey.toLowerCase(), currentValue.trim());
      }
      const colonIdx = line.indexOf(':');
      if (colonIdx !== -1) {
        currentKey = line.slice(0, colonIdx).trim().toLowerCase();
        currentValue = line.slice(colonIdx + 1).trim();
      } else {
        currentKey = '';
        currentValue = '';
      }
    }
  }

  if (currentKey) {
    headers.set(currentKey.toLowerCase(), currentValue.trim());
  }

  return headers;
}

interface BodyResult {
  text: string;
  html: string;
}

function extractBodies(body: string, contentType: string, transferEncoding: string): BodyResult {
  const lowerContentType = contentType.toLowerCase();

  // Check for multipart
  if (lowerContentType.includes('multipart/')) {
    const boundaryMatch = contentType.match(/boundary=(?:"([^"]+)"|([^;\s]+))/i);
    if (boundaryMatch) {
      const boundary = boundaryMatch[1] || boundaryMatch[2];
      return parseMultipart(body, boundary);
    }
  }

  // Single part
  const decodedBody = decodeContent(body, transferEncoding, contentType);
  if (lowerContentType.includes('text/html')) {
    return { text: '', html: decodedBody };
  } else {
    return { text: decodedBody, html: '' };
  }
}

function parseMultipart(body: string, boundary: string): BodyResult {
  const result: BodyResult = { text: '', html: '' };
  const delimiter = `--${boundary}`;
  const parts = body.split(delimiter);

  for (let i = 1; i < parts.length; i++) {
    const part = parts[i];
    if (part.startsWith('--')) {
      // End boundary
      break;
    }

    const trimmed = part.replace(/^(\r\n|\r|\n)/, '');
    const headerEnd = findHeaderEnd(trimmed);
    if (headerEnd === -1) continue;

    const partHeaderSection = trimmed.slice(0, headerEnd);
    const partBodySection = trimmed.slice(headerEnd).replace(/^(\r\n|\r|\n)/, '');

    const partHeaders = parseHeaders(partHeaderSection);
    const partContentType = partHeaders.get('content-type') || 'text/plain';
    const partEncoding = partHeaders.get('content-transfer-encoding') || '';

    const subBodies = extractBodies(partBodySection, partContentType, partEncoding);
    if (subBodies.text && !result.text) {
      result.text = subBodies.text;
    }
    if (subBodies.html && !result.html) {
      result.html = subBodies.html;
    }
  }

  return result;
}

function decodeContent(rawContent: string, encoding: string, contentType: string): string {
  const enc = encoding.toLowerCase().trim();
  let decoded = rawContent;

  if (enc === 'base64') {
    try {
      const cleanB64 = rawContent.replace(/[\r\n\s]/g, '');
      const binaryString = atob(cleanB64);
      const bytes = new Uint8Array(binaryString.length);
      for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }
      const charset = extractCharset(contentType) || 'utf-8';
      decoded = new TextDecoder(charset, { fatal: false, ignoreBOM: false }).decode(bytes);
    } catch {
      decoded = rawContent;
    }
  } else if (enc === 'quoted-printable') {
    decoded = decodeQuotedPrintable(rawContent, extractCharset(contentType) || 'utf-8');
  }

  return decoded;
}

function decodeQuotedPrintable(input: string, charset: string = 'utf-8'): string {
  // Remove soft line breaks (= followed by CRLF or LF)
  const normalized = input.replace(/=\r?\n/g, '');
  // Collect bytes
  const bytes: number[] = [];
  let i = 0;
  while (i < normalized.length) {
    if (normalized[i] === '=' && i + 2 < normalized.length && /[0-9A-Fa-f]{2}/.test(normalized.slice(i + 1, i + 3))) {
      bytes.push(parseInt(normalized.slice(i + 1, i + 3), 16));
      i += 3;
    } else {
      bytes.push(normalized.charCodeAt(i));
      i++;
    }
  }

  try {
    return new TextDecoder(charset, { fatal: false, ignoreBOM: false }).decode(new Uint8Array(bytes));
  } catch {
    return new TextDecoder('utf-8', { fatal: false, ignoreBOM: false }).decode(new Uint8Array(bytes));
  }
}

function extractCharset(contentType: string): string | null {
  const match = contentType.match(/charset=(?:"([^"]+)"|([^;\s]+))/i);
  return match ? (match[1] || match[2]).toLowerCase() : null;
}

/**
 * Decodes RFC 2047 headers like =?UTF-8?B?...?= or =?UTF-8?Q?...?=
 */
export function decodeHeader(header: string): string {
  if (!header) return '';
  return header.replace(/=\?([^?]+)\?([BQbq])\?([^?]+)\?=/g, (_, charset, encoding, text) => {
    try {
      const enc = encoding.toUpperCase();
      if (enc === 'B') {
        const bin = atob(text);
        const bytes = new Uint8Array(bin.length);
        for (let i = 0; i < bin.length; i++) {
          bytes[i] = bin.charCodeAt(i);
        }
        return new TextDecoder(charset, { fatal: false, ignoreBOM: false }).decode(bytes);
      } else if (enc === 'Q') {
        // Q encoding replaces _ with space, and =XX with byte
        const qReplaced = text.replace(/_/g, ' ');
        return decodeQuotedPrintable(qReplaced, charset);
      }
    } catch {
      return text;
    }
    return text;
  });
}
