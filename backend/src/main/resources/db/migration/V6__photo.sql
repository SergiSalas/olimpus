-- The photo, asked for at sign-up and shown to nobody until level 3.
--
-- Only the metadata is here. The bytes live behind PhotoStorage (a folder on
-- disk while developing, object storage inside the EU for the beta), so moving
-- them never touches a rule.

create table photo (
    account_id   uuid        primary key references account (id) on delete cascade,
    storage_id   text        not null,
    content_type text        not null,
    size_bytes   bigint      not null,
    moderation   text        not null,
    uploaded_at  timestamptz not null,

    constraint photo_size check (size_bytes > 0),
    constraint photo_type check (content_type in ('image/jpeg', 'image/png'))
);
