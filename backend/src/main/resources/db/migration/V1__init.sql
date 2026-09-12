-- Primera migracion: solo deja constancia de que el esquema existe y de
-- que Flyway se ejecuta. Las tablas de verdad (personas, rondas, chats)
-- llegan en el paso 2.
create table app_info (
    clave      text primary key,
    valor      text        not null,
    creado_en  timestamptz not null default now()
);

insert into app_info (clave, valor) values ('esquema', 'v1');
