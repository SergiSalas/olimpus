-- The chat of the day.
--
-- Messages live in their own table, but the conversation keeps how many each
-- side has written: that count decides whether the conversation took off and,
-- later on, whether it earns the next unlock level. Without it every query
-- would have to count messages.

-- The shared interest the opening question is about, chosen when the round
-- runs so both people see the same one. Null when they share nothing. Only the
-- interest is stored, not the sentence: the wording depends on the reader's
-- language.
alter table conversation add column icebreaker_interest text;

create table message (
    id              uuid        primary key,
    conversation_id uuid        not null references conversation (id) on delete cascade,
    sender          uuid        not null references account (id) on delete cascade,
    text            text        not null,
    sent_at         timestamptz not null,

    constraint message_not_blank check (length(trim(text)) > 0)
);

create index message_conversation_idx on message (conversation_id, sent_at);
