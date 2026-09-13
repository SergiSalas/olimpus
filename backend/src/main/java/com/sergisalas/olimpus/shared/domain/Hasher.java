package com.sergisalas.olimpus.shared.domain;

/**
 * Port: turn a secret into something that is safe to store.
 *
 * <p>Neither the email codes nor the session tokens are stored as they are: if
 * someone ever read the database, nothing inside would let them log in.
 */
public interface Hasher {

    String hash(String secret);
}
