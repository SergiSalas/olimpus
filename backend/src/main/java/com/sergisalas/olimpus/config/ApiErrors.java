package com.sergisalas.olimpus.config;

import com.sergisalas.olimpus.auth.adapter.in.NotAuthenticatedException;
import com.sergisalas.olimpus.auth.domain.InvalidEmailException;
import com.sergisalas.olimpus.auth.domain.InvalidLoginCodeException;
import com.sergisalas.olimpus.chat.domain.ChatClosedException;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.profile.domain.ProfileNotFoundException;
import com.sergisalas.olimpus.profile.domain.UnderageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los errores del dominio a respuestas HTTP con un mensaje en claro
 * que el movil puede ensenar tal cual.
 */
@RestControllerAdvice
public class ApiErrors {

    public record ApiError(String error) {}

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ApiError> email(InvalidEmailException e) {
        return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
    }

    /**
     * Siempre el mismo mensaje, sin decir si el codigo no existia, si caduco o
     * si simplemente no coincide: dar detalles ayudaria a quien prueba codigos.
     */
    @ExceptionHandler(InvalidLoginCodeException.class)
    public ResponseEntity<ApiError> code(InvalidLoginCodeException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError("El código no es válido o ha caducado. Pide uno nuevo."));
    }

    @ExceptionHandler(NotAuthenticatedException.class)
    public ResponseEntity<ApiError> auth(NotAuthenticatedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(e.getMessage()));
    }

    /** Se responde 404 y no 403: no se confirma siquiera que esa conversacion exista. */
    @ExceptionHandler(NotYourConversationException.class)
    public ResponseEntity<ApiError> notYours(NotYourConversationException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(ChatClosedException.class)
    public ResponseEntity<ApiError> closed(ChatClosedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ApiError> noProfile(ProfileNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(UnderageException.class)
    public ResponseEntity<ApiError> underage(UnderageException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegal(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
    }
}
