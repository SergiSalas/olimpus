-- First migration: it only records that the schema exists and that Flyway
-- runs. The real tables (people, rounds, chats) come in later steps.
create table app_info (
    name       text primary key,
    value      text        not null,
    created_at timestamptz not null default now()
);

insert into app_info (name, value) values ('schema', 'v1');
