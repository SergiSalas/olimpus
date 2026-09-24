-- Where to reach each person's phone.
--
-- One person can have several (the same account on two phones), and a token can
-- move from one account to another when a phone changes hands, so the token
-- itself is the key.

create table push_token (
    token      text        primary key,
    account_id uuid        not null references account (id) on delete cascade,
    updated_at timestamptz not null default now()
);

create index push_token_account_idx on push_token (account_id);
