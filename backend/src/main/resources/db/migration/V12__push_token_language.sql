-- The language each phone has the app in. Notifications are sent without the
-- phone asking for anything, so there is no request to read the language from:
-- it is kept here, next to the token, when the phone registers it. Per phone
-- and not per account, because the same account can be on two phones.
alter table push_token add column language text not null default 'es';
