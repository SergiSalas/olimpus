-- La ronda diaria: una conversacion nueva al dia por persona.
--
-- Se guarda de donde salio cada pareja (origin) y cuanto prometia (score).
-- Esos dos campos son los que permitiran comprobar, con datos de la beta, si
-- las parejas que elige el algoritmo funcionan mejor que las del azar.

create table conversation (
    id              uuid        primary key,
    round_date      date        not null,
    round_kind      text        not null,
    account_a       uuid        not null references account (id) on delete cascade,
    account_b       uuid        not null references account (id) on delete cascade,
    origin          text        not null,
    score           double precision not null,
    opens_at        timestamptz not null,
    closes_at       timestamptz not null,
    state           text        not null,
    messages_from_a int         not null default 0,
    messages_from_b int         not null default 0,
    created_at      timestamptz not null default now(),

    constraint conversation_distintos check (account_a <> account_b)
);

create index conversation_a_idx on conversation (account_a, round_date);
create index conversation_b_idx on conversation (account_b, round_date);
create index conversation_date_idx on conversation (round_date);

-- Bloqueos y reportes. Existe desde ya, aunque la pantalla para usarlo llegue
-- despues, porque el filtro duro del reparto lo consulta en cada ronda.
create table block (
    blocker    uuid        not null references account (id) on delete cascade,
    blocked    uuid        not null references account (id) on delete cascade,
    reason     text,
    created_at timestamptz not null default now(),

    primary key (blocker, blocked),
    constraint block_distintos check (blocker <> blocked)
);
