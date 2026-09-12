-- Identidad: entrar con email y un codigo de un solo uso.
-- Ni los codigos ni las llaves de sesion se guardan en claro, solo su huella.

create table account (
    id         uuid        primary key,
    email      text        not null unique,
    created_at timestamptz not null default now()
);

-- Un codigo vivo por email. Pedir otro sustituye al anterior.
-- Pedir codigo no crea cuenta: aqui puede haber emails que nunca lleguen a ser
-- cuenta, y se limpian solos al caducar.
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
