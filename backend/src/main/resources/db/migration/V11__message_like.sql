-- A heart on a message. Only one kind, and only the person who received the
-- message can give it, so a single column is enough: when it was liked, or
-- null while it is not.
alter table message add column liked_at timestamptz;
