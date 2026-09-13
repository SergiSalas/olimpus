package com.sergisalas.olimpus.profile.domain;

import java.util.Optional;

/**
 * Port: where the bytes of a photo live.
 *
 * <p>One of the swappable pieces. A folder on disk while developing; object
 * storage inside the EU for the beta. Nothing above this line changes when that
 * happens.
 */
public interface PhotoStorage {

    /** @return the id needed to read those bytes back */
    String store(byte[] bytes, String contentType);

    Optional<byte[]> read(String storageId);

    void delete(String storageId);
}
