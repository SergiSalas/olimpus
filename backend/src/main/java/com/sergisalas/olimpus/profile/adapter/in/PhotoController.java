package com.sergisalas.olimpus.profile.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.profile.application.UploadPhoto;
import com.sergisalas.olimpus.profile.application.ViewPhoto;
import com.sergisalas.olimpus.profile.domain.ModerationState;
import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile/photo")
public class PhotoController {

    public record PhotoResponse(boolean uploaded, ModerationState moderation, Instant uploadedAt) {}

    private final UploadPhoto uploadPhoto;
    private final ViewPhoto viewPhoto;

    public PhotoController(UploadPhoto uploadPhoto, ViewPhoto viewPhoto) {
        this.uploadPhoto = uploadPhoto;
        this.viewPhoto = viewPhoto;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhotoResponse upload(@CurrentAccount Account account, @RequestParam MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (bytes.length == 0) {
            throw new RuleViolationException("photo.empty");
        }

        var photo = uploadPhoto.execute(account.id(), bytes, file.getContentType());
        return new PhotoResponse(true, photo.moderation(), photo.uploadedAt());
    }

    /**
     * Your own photo. There is no endpoint yet for anyone else's: that arrives
     * with level 3, and it will be a check, never a shareable link.
     */
    @GetMapping
    public ResponseEntity<byte[]> myPhoto(@CurrentAccount Account account) {
        ViewPhoto.Bytes photo = viewPhoto.ownPhoto(account.id());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                // Never in a shared cache: it is one person's photo.
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(photo.content());
    }
}
