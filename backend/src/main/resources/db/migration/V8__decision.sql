-- The end of the day: "do you want to keep getting to know this person?".
--
-- Each answer is stored on its own side and is never shown to the other, not
-- even afterwards. If they do not match, nobody learns who said no, which is
-- what makes saying no cost nothing.
--
-- A mutual yes does not create a new row: the same conversation becomes
-- CONNECTED, so the whole history stays and the chat simply never closes.

alter table conversation add column decision_by_a text;
alter table conversation add column decision_by_b text;

create index conversation_connected_idx
    on conversation (account_a, account_b)
    where state = 'CONNECTED';
