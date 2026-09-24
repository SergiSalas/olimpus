package com.sergisalas.olimpus.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Optional<Account> findByEmail(EmailAddress email);

    Optional<Account> findById(UUID id);

    Account save(Account account);

    /**
     * Wipes the account and everything hanging off it. The photo bytes live
     * outside the database and are not covered by this.
     */
    void delete(UUID accountId);
}
