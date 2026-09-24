-- The bio becomes answers to set questions.
--
-- A free box asking you to describe yourself gets filled with a shrug, and in
-- practice nobody was filling it: the sign-up never even asked for it, so every
-- bio in this table is the empty default. Dropping the column loses no data.

alter table profile drop column bio;

-- Three answers, the same three the sign-up demands. The position keeps the
-- order the person chose, so their profile reads the way they wrote it.
create table profile_prompt (
    account_id uuid not null references profile (account_id) on delete cascade,
    position   int  not null,
    question   text not null,
    answer     text not null,

    primary key (account_id, position),
    -- The same question answered three times would leave level 2 as empty as
    -- the bio this replaces.
    unique (account_id, question),
    constraint profile_prompt_position check (position between 0 and 2),
    constraint profile_prompt_answer check (length(answer) between 1 and 200)
);

-- Gender in two pieces: the word the person uses, shown at level 3, and the
-- box matching works with, which stays a four-value hard filter.
alter table profile add column gender_label text not null default '';

-- Level 3, both optional.
alter table profile add column occupation text not null default '';
alter table profile add column from_place text not null default '';

-- Y los registros que ya existían se van.
--
-- Un perfil sin preguntas no es un perfil válido: el dominio exige las tres, y
-- lo exige en el constructor, así que una fila así no se puede ni leer. Aquí
-- había tres caminos y dos eran peores. Inventarles las respuestas era escribir
-- en nombre de otro un texto que un desconocido lee en el nivel 2 como suyo.
-- Aflojar la regla para que un perfil pueda existir a medias era romper lo
-- único que garantiza que todo lo que se lee de alguien lo ha dicho esa persona.
--
-- Queda borrarlos. Esto es anterior a la beta y lo que hay son registros de
-- prueba; quien vuelva, se registra de nuevo. Si algún día esta migración
-- llegara a correr con gente de verdad dentro, este borrado NO es la respuesta:
-- entonces tocaría partirla en dos y dejar que cada uno añada sus preguntas la
-- próxima vez que entre.
--
-- Se lleva por delante, en cascada, las conversaciones de esa gente.
delete from profile p
where not exists (
    select 1 from profile_prompt pp where pp.account_id = p.account_id
);

-- Y con ellos sus conversaciones.
--
-- No se van solas: conversation cuelga de account, no de profile, así que
-- borrar el perfil deja la conversación en pie apuntando a alguien que ya no
-- tiene nada que enseñar. De ahí salen mensajes y niveles por cascada.
--
-- Las cuentas se quedan: entrar sin registro es un estado normal en esta app, y
-- quien vuelva se encuentra el registro por hacer en vez de la puerta cerrada.
-- Los bloqueos también se quedan, que para eso están: a un bloqueo no se le da
-- la vuelta volviéndose a registrar.
delete from conversation c
where not exists (select 1 from profile p where p.account_id = c.account_a)
   or not exists (select 1 from profile p where p.account_id = c.account_b);
