-- Migration: 0001_initial.sql
CREATE TABLE IF NOT EXISTS messages (
    id TEXT PRIMARY KEY,
    from_addr TEXT,
    to_addr TEXT,
    subject TEXT,
    text TEXT,
    html TEXT,
    code TEXT,
    date TEXT,
    created_at TEXT,
    read INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_to_addr ON messages(to_addr);

CREATE TABLE IF NOT EXISTS aliases (
    id TEXT PRIMARY KEY,
    prefix TEXT UNIQUE,
    address TEXT,
    created_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_aliases_prefix ON aliases(prefix);

CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,
    value TEXT
);

INSERT OR IGNORE INTO settings (key, value) VALUES ('catchAll', '1');
