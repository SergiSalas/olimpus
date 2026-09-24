package com.sergisalas.olimpus.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.auth.FakeAuthWorld;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import com.sergisalas.olimpus.profile.domain.ModerationState;
import com.sergisalas.olimpus.profile.domain.Photo;
import com.sergisalas.olimpus.profile.domain.PhotoRepository;
import com.sergisalas.olimpus.profile.domain.PhotoStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Deleting an account. The thing worth guarding here is the photo: the database
 * cleans up after itself, the file on disk does not.
 */
class DeleteAccountTest {

    private FakeAuthWorld world;
    private Map<UUID, Photo> photoRows;
    private Map<String, byte[]> files;
    private List<String> deletedFiles;
    private DeleteAccount delete;
    private Account ana;

    @BeforeEach
    void setUp() {
        world = new FakeAuthWorld();
        photoRows = new HashMap<>();
        files = new HashMap<>();
        deletedFiles = new ArrayList<>();

        ana = world.accounts.save(Account.created(EmailAddress.of("ana@example.com"), world.now));

        PhotoRepository photos =
                new PhotoRepository() {
                    @Override
                    public Optional<Photo> findByAccountId(UUID accountId) {
                        return Optional.ofNullable(photoRows.get(accountId));
                    }

                    @Override
                    public void save(Photo photo) {
                        photoRows.put(photo.accountId(), photo);
                    }
                };

        PhotoStorage storage =
                new PhotoStorage() {
                    @Override
                    public String store(byte[] bytes, String contentType) {
                        String id = "file-" + files.size();
                        files.put(id, bytes);
                        return id;
                    }

                    @Override
                    public Optional<byte[]> read(String storageId) {
                        return Optional.ofNullable(files.get(storageId));
                    }

                    @Override
                    public void delete(String storageId) {
                        deletedFiles.add(storageId);
                        files.remove(storageId);
                    }
                };

        delete = new DeleteAccount(world.accounts, photos, storage);
    }

    private void giveAnaAPhoto() {
        photoRows.put(
                ana.id(),
                new Photo(
                        ana.id(), "file-0", "image/jpeg", 1024, ModerationState.APPROVED, world.now));
        files.put("file-0", new byte[] {1, 2, 3});
    }

    @Test
    void the_account_is_gone_and_cannot_be_found_again() {
        delete.execute(ana.id());

        assertThat(world.accounts.findById(ana.id())).isEmpty();
        assertThat(world.accounts.findByEmail(EmailAddress.of("ana@example.com"))).isEmpty();
    }

    @Test
    void the_photo_file_is_deleted_too_because_no_database_would_do_it() {
        giveAnaAPhoto();

        delete.execute(ana.id());

        assertThat(deletedFiles).containsExactly("file-0");
        assertThat(files).isEmpty();
    }

    @Test
    void deleting_an_account_without_a_photo_is_not_a_problem() {
        delete.execute(ana.id());

        assertThat(deletedFiles).isEmpty();
        assertThat(world.accounts.findById(ana.id())).isEmpty();
    }

    @Test
    void only_that_person_disappears() {
        Account leo =
                world.accounts.save(Account.created(EmailAddress.of("leo@example.com"), world.now));

        delete.execute(ana.id());

        assertThat(world.accounts.findById(leo.id())).isPresent();
    }
}
