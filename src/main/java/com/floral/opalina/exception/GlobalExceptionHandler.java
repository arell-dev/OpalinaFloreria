package com.floral.opalina.exception;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException error) {
        Map<String, List<String>> errores = new LinkedHashMap<>();

        error.getBindingResult().getFieldErrors().forEach(errorCampo -> {
            errores.computeIfAbsent(errorCampo.getField(), campo -> new ArrayList<>())
                   .add(errorCampo.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(Map.of("status", 400, "errores", errores));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> reglaNegocio(IllegalArgumentException error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(Map.of("status", 400, "error", error.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             .body(Map.of("status", 404, "error", error.getMessage()));
    }

    @ExceptionHandler(ConflictoNegocioException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoNegocioException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                             .body(Map.of("status", 409, "error", error.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                             .body(Map.of("status", 409, "error", "La operación entra en conflicto con datos existentes."));
    }
}
