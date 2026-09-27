package com.floral.opalina.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException error) {
        var errores = error.getBindingResult().getFieldErrors().stream()
                                              .map(e -> Map.of("campo", e.getField(), "mensaje", e.getDefaultMessage())).toList();

        return ResponseEntity.badRequest().body(Map.of("status", 400, "errores", errores));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> reglaNegocio(IllegalArgumentException error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(Map.of("status", 400, "error", error.getMessage()));
    }
}
