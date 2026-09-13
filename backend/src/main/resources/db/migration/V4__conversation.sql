-- The daily round: one new conversation a day per person.
--
-- Each pair stores where it came from (origin) and how promising it looked
-- (score). Those two fields are what will let us check, with beta data, whether
-- the pairs the algorithm picks do better than random ones.

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

    constraint conversation_distinct_accounts check (account_a <> account_b)
);

create index conversation_a_idx on conversation (account_a, round_date);
create index conversation_b_idx on conversation (account_b, round_date);
create index conversation_date_idx on conversation (round_date);

-- Blocks and reports. It exists already, even though the screen to use it
-- comes later, because the hard filter of every round reads it.
create table block (
    blocker    uuid        not null references account (id) on delete cascade,
    blocked    uuid        not null references account (id) on delete cascade,
    reason     text,
    created_at timestamptz not null default now(),

    primary key (blocker, blocked),
    constraint block_distinct_accounts check (blocker <> blocked)
);
