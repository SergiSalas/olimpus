package com.sergisalas.olimpus.config;

import com.sergisalas.olimpus.shared.adapter.Messages;
import com.sergisalas.olimpus.shared.domain.UserFacingError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns domain errors into HTTP responses with a plain message, in the user's
 * language, that the phone can show as it is.
 *
 * <p>There is <b>one</b> handler for everything a person can cause, and it reads
 * the kind off the error itself. The previous version had one handler per
 * exception class, and twice a new error shipped without its line here and came
 * out as a 500 in front of a person. A list you have to remember to update is a
 * bug waiting to happen; this cannot forget.
 */
@RestControllerAdvice
public class ApiErrors {

    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

    public record ApiError(String error) {}

    private final Messages messages;

    public ApiErrors(Messages messages) {
        this.messages = messages;
    }

    /**
     * Anything that implements {@link UserFacingError}, whatever its class and
     * wherever it was thrown from.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handle(RuntimeException e) {
        if (e instanceof UserFacingError error) {
            return ResponseEntity.status(statusFor(error.kind()))
                    .body(new ApiError(messages.of(error)));
        }

        if (e instanceof IllegalArgumentException) {
            // An invariant only a bug could break. Its details are no use to the
            // person, but they are to us.
            log.warn("Invalid request: {}", e.toString());
            return ResponseEntity.badRequest()
                    .body(new ApiError(messages.get("error.invalid-request")));
        }

        // Not ours to explain: let Spring answer 500 and log the stack trace.
        throw e;
    }

    private static HttpStatus statusFor(UserFacingError.Kind kind) {
        return switch (kind) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_AUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case NOT_ALLOWED -> HttpStatus.FORBIDDEN;
                // 404 and not 403: it does not even confirm that the thing exists.
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case WRONG_MOMENT -> HttpStatus.CONFLICT;
        };
    }
}
