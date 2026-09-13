-- El chat del dia.
--
-- Los mensajes van aparte, pero la conversacion guarda cuantos ha escrito cada
-- lado: esa cuenta es la que decide si la conversacion arranco y, mas adelante,
-- si se gana el siguiente nivel de desbloqueo. Sin ella habria que contar
-- mensajes en cada consulta.

alter table conversation add column icebreaker text;

create table message (
    id              uuid        primary key,
    conversation_id uuid        not null references conversation (id) on delete cascade,
    sender          uuid        not null references account (id) on delete cascade,
    text            text        not null,
    sent_at         timestamptz not null,

    constraint message_no_vacio check (length(trim(text)) > 0)
);

create index message_conversation_idx on message (conversation_id, sent_at);
