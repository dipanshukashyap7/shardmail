/**
 * Extracts verification code / OTP from email subject, text, and html.
 * - Prioritizes labelled codes (e.g. "your code is 123456", "verification code: 948201", "OTP: 492810")
 * - Falls back to isolated 6-digit (or 4-8 digit) standalone codes
 * - STRICT RULE: A bare year/date like 2024, 2025, 2026, etc. is NEVER a code.
 */
export function extractVerificationCode(subject: string = '', text: string = '', html: string = ''): string | null {
  const combined = `${subject}\n${text}\n${stripHtml(html)}`;

  // 1. Explicit labelled patterns (highest confidence)
  const labelledPatterns = [
    /(?:verification|security|confirmation|validation|login|auth|authorization|activation|passcode|otp|pin)\s*(?:code)?\s*(?:is|:|=|-|–|—)\s*[:#]?\s*([0-9]{4,8})/i,
    /(?:your|the)\s+(?:verification|security|confirmation|login|auth|activation|one-time|otp|passcode)?\s*code\s*(?:is|:|=|-|–|—)\s*[:#]?\s*([0-9]{4,8})/i,
    /(?:enter|use)\s+([0-9]{4,8})\s+(?:to\s+(?:verify|log\s*in|sign\s*in|confirm|continue|authenticate))/i,
    /(?:code|otp|pin|passcode)\s*[:#]\s*([0-9]{4,8})/i,
    /code\s+is\s*[:\s]\s*([0-9]{4,8})/i,
    /(?:one-time\s+password|one-time\s+passcode|otp)\s*(?:is|:)?\s*([0-9]{4,8})/i,
    /\b([0-9]{4,8})\s+(?:is\s+your\s+(?:verification|confirmation|login|security|otp|code))/i,
  ];

  for (const pattern of labelledPatterns) {
    const match = combined.match(pattern);
    if (match && match[1]) {
      const code = match[1].trim();
      if (!isYearOrDate(code)) {
        return code;
      }
    }
  }

  // 2. Check HTML emphasized elements (e.g. <b>123456</b>, <strong>123456</strong>, <span>123456</span>)
  if (html) {
    const htmlTagPatterns = [
      /<(?:b|strong|h[1-4]|mark)[^>]*>\s*([0-9]{4,8})\s*<\/(?:b|strong|h[1-4]|mark)>/gi,
      /<span[^>]*style="[^"]*(?:font-size|letter-spacing|font-weight)[^"]*"[^>]*>\s*([0-9]{4,8})\s*<\/span>/gi,
      /<div[^>]*class="[^"]*(?:code|otp|token|pin)[^"]*"[^>]*>\s*([0-9]{4,8})\s*<\/div>/gi,
    ];

    for (const pattern of htmlTagPatterns) {
      let match;
      while ((match = pattern.exec(html)) !== null) {
        const candidate = match[1].trim();
        if (!isYearOrDate(candidate)) {
          return candidate;
        }
      }
    }
  }

  // 3. Fallback: Standalone 6-digit number (most common OTP length)
  // Must be isolated by non-digit characters and not part of a date or timestamp
  const sixDigitRegex = /(?<![0-9\-\/:#])([0-9]{6})(?![0-9\-\/:#])/g;
  let match;
  const candidates: string[] = [];
  while ((match = sixDigitRegex.exec(combined)) !== null) {
    const candidate = match[1];
    if (!isYearOrDate(candidate)) {
      candidates.push(candidate);
    }
  }

  if (candidates.length === 1) {
    return candidates[0];
  } else if (candidates.length > 1) {
    // Prefer code appearing in subject or near keywords
    const subjectMatch = subject.match(/(?<![0-9])([0-9]{6})(?![0-9])/);
    if (subjectMatch && !isYearOrDate(subjectMatch[1])) {
      return subjectMatch[1];
    }
    return candidates[0];
  }

  return null;
}

function isYearOrDate(str: string): boolean {
  const num = parseInt(str, 10);
  if (isNaN(num)) return true;
  // Years 1900 to 2099 are never valid codes (specifically bare 4 digits like 2024, 2025, 2026)
  if (str.length === 4 && num >= 1900 && num <= 2099) {
    return true;
  }
  // All zeros or repeating single digit (e.g. 000000, 111111) are rarely authentic OTPs unless explicitly matched
  return false;
}

function stripHtml(html: string): string {
  if (!html) return '';
  return html
    .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/\s+/g, ' ')
    .trim();
}
