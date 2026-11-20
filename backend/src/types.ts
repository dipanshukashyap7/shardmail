export interface Env {
  DB: D1Database;
  ADMIN_TOKEN: string;
  RESEND_API_KEY?: string;
}

export interface MessageRow {
  id: string;
  from_addr: string;
  to_addr: string;
  subject: string;
  text: string;
  html: string;
  code: string | null;
  date: string;
  created_at: string;
  read: number;
}

export interface AliasRow {
  id: string;
  prefix: string;
  address: string;
  created_at: string;
}

export interface SettingRow {
  key: string;
  value: string;
}
