package com.floral.opalina.persistence.jpa.adapter;

import com.floral.opalina.exception.ConflictoNegocioException;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

final class ManejoErroresJpa {
    private ManejoErroresJpa() {
    }

    static void comprobarIntegridad(EntityManager entityManager, String mensaje) {
        try {
            entityManager.flush();
        } catch (RuntimeException error) {
            Throwable causa = error;
            while (causa != null) {
                if (causa instanceof ConstraintViolationException || causa instanceof DataIntegrityViolationException) {
                    throw new ConflictoNegocioException(mensaje);
                }
                causa = causa.getCause();
            }
            throw error;
        }
    }
}
