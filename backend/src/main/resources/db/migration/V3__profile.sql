-- El registro: las diez preguntas.
-- La ubicacion se guarda redondeada a dos decimales (algo mas de un km): con
-- eso la distancia sirve para emparejar y no senala la casa de nadie.

create table profile (
    account_id         uuid        primary key references account (id) on delete cascade,
    nickname           text        not null,
    bio                text        not null default '',
    birth_date         date        not null,
    gender             text        not null,
    seeking            text[]      not null,
    age_min            int         not null,
    age_max            int         not null,
    max_distance_km    int         not null,
    latitude           double precision not null,
    longitude          double precision not null,
    sociability        int         not null,
    conversation_depth int         not null,
    intent             text        not null,
    interests          text[]      not null,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),

    constraint profile_age_range check (age_min >= 18 and age_max <= 99 and age_min <= age_max),
    constraint profile_scales check (
        sociability between 1 and 5 and conversation_depth between 1 and 5),
    constraint profile_interests check (
        array_length(interests, 1) between 5 and 8)
);

-- Los idiomas en su propia tabla: son varios por persona y con nivel.
create table profile_language (
    account_id uuid not null references profile (account_id) on delete cascade,
    code       text not null,
    level      text not null,
    primary key (account_id, code)
);
