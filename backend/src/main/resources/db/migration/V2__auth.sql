-- Identity: log in with an email and a one-time code.
-- Neither the codes nor the session tokens are stored in clear, only their hash.

create table account (
    id         uuid        primary key,
    email      text        not null unique,
    created_at timestamptz not null default now()
);

-- One live code per email. Requesting another replaces the previous one.
-- Requesting a code does not create an account: there may be emails here that
-- never become accounts, and they go away on their own when they expire.
create table login_code (
    email         text        primary key,
    code_hash     text        not null,
    expires_at    timestamptz not null,
    attempts_left int         not null,
    created_at    timestamptz not null default now()
);

create table session (
    token_hash text        primary key,
    account_id uuid        not null references account (id) on delete cascade,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    revoked_at timestamptz
);

create index session_account_idx on session (account_id);
