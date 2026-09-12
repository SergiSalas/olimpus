package com.sergisalas.olimpus.config;

import com.sergisalas.olimpus.auth.adapter.in.NotAuthenticatedException;
import com.sergisalas.olimpus.auth.domain.InvalidEmailException;
import com.sergisalas.olimpus.auth.domain.InvalidLoginCodeException;
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegal(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
    }
}
