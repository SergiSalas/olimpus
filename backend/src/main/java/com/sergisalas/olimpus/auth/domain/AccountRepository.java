package com.sergisalas.olimpus.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Optional<Account> findByEmail(EmailAddress email);

    Optional<Account> findById(UUID id);

    Account save(Account account);
}
