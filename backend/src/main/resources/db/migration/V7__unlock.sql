-- Level 3: the photo, and only if both ask for it.
--
-- Two moments instead of two flags: knowing WHEN each one asked is what will
-- let us check later whether the ladder is doing its job or people are just
-- rushing to the photo.
--
-- The levels themselves are not stored. They are computed from the messages, so
-- there is no counter that can drift away from what actually happened.

alter table conversation add column photo_wanted_by_a timestamptz;
alter table conversation add column photo_wanted_by_b timestamptz;
