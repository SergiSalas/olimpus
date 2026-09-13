package com.sergisalas.olimpus.config;

import com.sergisalas.olimpus.auth.adapter.in.NotAuthenticatedException;
import com.sergisalas.olimpus.auth.domain.InvalidLoginCodeException;
import com.sergisalas.olimpus.chat.domain.ChatClosedException;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.profile.domain.ProfileNotFoundException;
import com.sergisalas.olimpus.profile.domain.UnderageException;
import com.sergisalas.olimpus.shared.adapter.Messages;
import com.sergisalas.olimpus.shared.domain.UserFacingError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns domain errors into HTTP responses with a plain message, in the user's
 * language, that the phone can show as it is.
 */
@RestControllerAdvice
public class ApiErrors {

    public record ApiError(String error) {}

    private final Messages messages;

    public ApiErrors(Messages messages) {
        this.messages = messages;
    }

    /**
     * Always the same message, without saying whether the code did not exist,
     * expired or simply did not match: details would help whoever is guessing.
     */
    @ExceptionHandler(InvalidLoginCodeException.class)
    public ResponseEntity<ApiError> code(InvalidLoginCodeException e) {
        return respond(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(NotAuthenticatedException.class)
    public ResponseEntity<ApiError> auth(NotAuthenticatedException e) {
        return respond(HttpStatus.UNAUTHORIZED, e);
    }

    /** 404 and not 403: it does not even confirm that the conversation exists. */
    @ExceptionHandler(NotYourConversationException.class)
    public ResponseEntity<ApiError> notYours(NotYourConversationException e) {
        return respond(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ChatClosedException.class)
    public ResponseEntity<ApiError> closed(ChatClosedException e) {
        return respond(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ApiError> noProfile(ProfileNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(UnderageException.class)
    public ResponseEntity<ApiError> underage(UnderageException e) {
        return respond(HttpStatus.FORBIDDEN, e);
    }

    /**
     * Broken rules the person can fix get their own message. Anything else is a
     * bug-level invariant whose details are no use to the user.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegal(IllegalArgumentException e) {
        String text =
                e instanceof UserFacingError error
                        ? messages.of(error)
                        : messages.get("error.invalid-request");
        return ResponseEntity.badRequest().body(new ApiError(text));
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, UserFacingError error) {
        return ResponseEntity.status(status).body(new ApiError(messages.of(error)));
    }
}
