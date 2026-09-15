package com.sergisalas.olimpus.auth.application;

import com.sergisalas.olimpus.auth.domain.AccountRepository;
import com.sergisalas.olimpus.profile.domain.Photo;
import com.sergisalas.olimpus.profile.domain.PhotoRepository;
import com.sergisalas.olimpus.profile.domain.PhotoStorage;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case: deleting an account, for good.
 *
 * <p>Everything goes: the sign-up, the photo, the conversations, the messages,
 * the sessions and where the phone was reachable. The database takes care of
 * most of it by itself (every table hangs off the account), but <b>the photo
 * bytes live outside the database</b> and nothing would delete them, so they are
 * deleted here first. A file nothing points to any more is the worst kind of
 * leftover: invisible and still a person's face.
 *
 * <p>Order matters: the photo first. If deleting the account failed afterwards,
 * we would be left with a profile without a photo, which is recoverable. The
 * other way round leaves an orphan file that nobody would ever find.
 */
public class DeleteAccount {

    private static final Logger log = LoggerFactory.getLogger(DeleteAccount.class);

    private final AccountRepository accounts;
    private final PhotoRepository photos;
    private final PhotoStorage storage;

    public DeleteAccount(
            AccountRepository accounts, PhotoRepository photos, PhotoStorage storage) {
        this.accounts = accounts;
        this.photos = photos;
        this.storage = storage;
    }

    public void execute(UUID accountId) {
        photos.findByAccountId(accountId).map(Photo::storageId).ifPresent(storage::delete);
        accounts.delete(accountId);

        // Worth a line in the log: it is the only trace that will remain, and
        // there is no id in it that points back to anybody.
        log.info("An account has been deleted at its owner's request.");
    }
}
