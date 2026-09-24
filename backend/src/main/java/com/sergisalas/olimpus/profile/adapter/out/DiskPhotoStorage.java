package com.sergisalas.olimpus.profile.adapter.out;

import com.sergisalas.olimpus.profile.domain.PhotoStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Development version: photos in a folder next to the backend.
 *
 * <p>Good enough for one machine and useless for the beta, where a restart on
 * another server would lose every photo. Replacing it means writing another
 * class that implements the same port.
 */
@Component
public class DiskPhotoStorage implements PhotoStorage {

    private static final Logger log = LoggerFactory.getLogger(DiskPhotoStorage.class);

    private final Path folder;

    public DiskPhotoStorage(@Value("${olimpus.photos.dir:data/photos}") String dir) {
        this.folder = Path.of(dir);
        try {
            Files.createDirectories(folder);
            log.info("Photos are kept in {}", folder.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot create the photo folder", e);
        }
    }

    @Override
    public String store(byte[] bytes, String contentType) {
        String storageId = UUID.randomUUID() + extensionFor(contentType);
        try {
            Files.write(folder.resolve(storageId), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write the photo", e);
        }
        return storageId;
    }

    @Override
    public Optional<byte[]> read(String storageId) {
        Path file = safeResolve(storageId);
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the photo", e);
        }
    }

    @Override
    public void delete(String storageId) {
        try {
            Files.deleteIfExists(safeResolve(storageId));
        } catch (IOException e) {
            log.warn("Could not delete photo {}: {}", storageId, e.getMessage());
        }
    }

    /** An id is a file name, never a path: "../../etc/passwd" is not a photo. */
    private Path safeResolve(String storageId) {
        Path file = folder.resolve(storageId).normalize();
        if (!file.startsWith(folder.normalize())) {
            throw new IllegalArgumentException("that storage id points outside the folder");
        }
        return file;
    }

    private static String extensionFor(String contentType) {
        return contentType.equals("image/png") ? ".png" : ".jpg";
    }
}
