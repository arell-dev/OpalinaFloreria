package com.floral.opalina.persistence.jpa.adapter;

import com.floral.opalina.persistence.jpa.entity.ClienteJpaEntity;
import com.floral.opalina.persistence.jpa.entity.ConsultaContactoJpaEntity;
import com.floral.opalina.persistence.jpa.springdata.ContactoJpaRepository;

import com.floral.opalina.domain.ConsultaContacto;
import com.floral.opalina.repository.ContactoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class ContactoPersistenceAdapter implements ContactoRepository {
    private final EntityManager entityManager;
    private final ContactoJpaRepository contactosJpa;

    public ContactoPersistenceAdapter(EntityManager entityManager, ContactoJpaRepository contactosJpa) {
        this.entityManager = entityManager;
        this.contactosJpa = contactosJpa;
    }

    @Override
    public void guardar(ConsultaContacto consulta) {
        ConsultaContactoJpaEntity entity = new ConsultaContactoJpaEntity();
        if (consulta.clienteId() != null) {
            entity.setCliente(entityManager.find(ClienteJpaEntity.class, consulta.clienteId()));
        }
        entity.setNombre(consulta.nombre());
        entity.setEmail(consulta.email());
        entity.setTelefono(consulta.telefono());
        entity.setAsunto(consulta.asunto());
        entity.setMensaje(consulta.mensaje());
        entity.setEstado("PENDIENTE");
        entity.setCreadoEn(consulta.creadoEn());
        contactosJpa.save(entity);
    }
}
