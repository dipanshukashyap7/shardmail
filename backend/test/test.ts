import { extractVerificationCode } from '../src/codeExtractor';
import { parseMimeString, decodeHeader } from '../src/mime';

function assert(condition: boolean, msg: string) {
  if (!condition) {
    throw new Error(`Assertion failed: ${msg}`);
  }
  console.log(`✓ ${msg}`);
}

console.log('--- Testing Code Extractor ---');
assert(extractVerificationCode('Your verification code is 123456') === '123456', 'Labelled code extraction 1');
assert(extractVerificationCode('Security Alert', 'Please enter 789012 to verify your identity') === '789012', 'Labelled code extraction 2');
assert(extractVerificationCode('Login OTP: 654321') === '654321', 'OTP label');
assert(extractVerificationCode('Your code: 4920') === '4920', '4 digit code');
assert(extractVerificationCode('Happy New Year 2026', 'Welcome to 2026') === null, 'Year 2026 is NEVER a code');
assert(extractVerificationCode('Copyright 2024-2025 All rights reserved') === null, 'Years 2024-2025 are rejected');
assert(extractVerificationCode('Subject', 'Here is your one-time code: 987654. Do not share.') === '987654', 'Code surrounded by text');
assert(extractVerificationCode('Test', '', '<div>Your code is <b>847291</b></div>') === '847291', 'HTML b tag code');

console.log('--- Testing MIME Parser ---');
const sampleEmail = `From: Test Sender <sender@example.com>
To: user@example.com
Subject: =?UTF-8?B?VGVzdCBWZXJpZmljYXRpb24=?=
Date: Thu, 19 Nov 2026 21:00:00 +0000
Content-Type: multipart/alternative; boundary="boundary-12345"

--boundary-12345
Content-Type: text/plain; charset="utf-8"
Content-Transfer-Encoding: quoted-printable

Hello User,

Your verification code is 556677.

Cheers!
--boundary-12345
Content-Type: text/html; charset="utf-8"
Content-Transfer-Encoding: base64

PHA+SGVsbG8gVXNlciw8L3A+PHA+WW91ciB2ZXJpZmljYXRpb24gY29kZSBpcyA8Yj41NTY2Nzc8L2I+LjwvcD4=
--boundary-12345--
`;

const parsed = parseMimeString(sampleEmail);
assert(parsed.subject === 'Test Verification', `Subject decoded correctly: ${parsed.subject}`);
assert(parsed.from.includes('sender@example.com'), `From header parsed: ${parsed.from}`);
assert(parsed.to.includes('user@example.com'), `To header parsed: ${parsed.to}`);
assert(parsed.text.includes('556677'), `Plain text body parsed: ${parsed.text}`);
assert(parsed.html.includes('556677'), `HTML body decoded: ${parsed.html}`);

const codeFromParsed = extractVerificationCode(parsed.subject, parsed.text, parsed.html);
assert(codeFromParsed === '556677', `Code extracted from parsed email: ${codeFromParsed}`);

console.log('All backend tests passed successfully!');
