package com.floral.opalina.service;

import com.floral.opalina.dto.ContactoForm;
import com.floral.opalina.domain.ConsultaContacto;
import com.floral.opalina.repository.ContactoRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactoService {
    private final ContactoRepository contactos;

    public ContactoService(ContactoRepository contactos) {
        this.contactos = contactos;
    }

    @Transactional
    public void enviar(ContactoForm formulario, Long clienteId) {
        contactos.guardar(new ConsultaContacto(
                        clienteId,
                        formulario.getNombre().trim(),
                        formulario.getEmail().trim().toLowerCase(Locale.ROOT),
                        normalizar(formulario.getTelefono()),
                        normalizar(formulario.getAsunto()),
                        formulario.getMensaje().trim(),
                        LocalDateTime.now())
            );
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
