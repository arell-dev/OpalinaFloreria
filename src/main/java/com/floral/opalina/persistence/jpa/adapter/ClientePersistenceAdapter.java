package com.floral.opalina.persistence.jpa.adapter;

import com.floral.opalina.persistence.jpa.entity.ClienteJpaEntity;
import com.floral.opalina.persistence.jpa.entity.CredencialJpaEntity;
import com.floral.opalina.persistence.jpa.springdata.ClienteJpaRepository;
import com.floral.opalina.persistence.jpa.springdata.CredencialJpaRepository;

import com.floral.opalina.exception.RecursoNoEncontradoException;
import com.floral.opalina.domain.Cliente;
import com.floral.opalina.domain.Credencial;
import com.floral.opalina.domain.enums.Rol;
import com.floral.opalina.repository.ClienteRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class ClientePersistenceAdapter implements ClienteRepository {
    private final EntityManager entityManager;
    private final ClienteJpaRepository clientesJpa;
    private final CredencialJpaRepository credencialesJpa;

    public ClientePersistenceAdapter(EntityManager entityManager, ClienteJpaRepository clientesJpa, CredencialJpaRepository credencialesJpa) {
        this.entityManager = entityManager;
        this.clientesJpa = clientesJpa;
        this.credencialesJpa = credencialesJpa;
    }

    @Override
    public List<Cliente> buscarTodos() {
        return clientesJpa.findAllByOrderByNombreAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Cliente> buscarPorId(Long id) {
        return clientesJpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Cliente> buscarPorEmail(String email) {
        return clientesJpa.findByEmailIgnoreCase(email.trim()).map(this::toDomain);
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        ClienteJpaEntity entity = cliente.getId() == null ? null : entityManager.find(ClienteJpaEntity.class, cliente.getId());
        if (entity == null) {
            entity = new ClienteJpaEntity();
        }
        entity.setNombre(cliente.getNombre());
        entity.setEmail(cliente.getEmail().trim().toLowerCase(Locale.ROOT));
        entity.setTelefono(cliente.getTelefono());
        entity.setDireccion(cliente.getDireccion());
        entity.setReferencia(cliente.getReferencia());
        entity.setCreadoEn(cliente.getCreadoEn());
        entity = clientesJpa.saveAndFlush(entity);
        ManejoErroresJpa.comprobarIntegridad(entityManager, "Ya existe una cuenta con ese correo");
        return toDomain(entity);
    }

    @Override
    public boolean esAdministrador(Long id) {
        return credencialesJpa.findById(id)
                              .map(credencial -> credencial.getRol() == Rol.ADMIN)
                              .orElse(false);
    }

    @Override
    public void guardarCredencial(Credencial credencial) {
        ClienteJpaEntity cliente = clientesJpa.findById(credencial.clienteId()).orElse(null);
        if (cliente == null) {
            throw new RecursoNoEncontradoException("No existe el cliente asociado a la credencial");
        }
        CredencialJpaEntity entity = credencialesJpa.findById(credencial.clienteId()).orElse(null);
        boolean nueva = entity == null;
        if (entity == null) {
            entity = new CredencialJpaEntity();
            entity.setClienteId(credencial.clienteId());
            entity.setCliente(cliente);
            entity.setCreadoEn(LocalDateTime.now());
        }
        entity.setClaveHash(credencial.claveHash());
        entity.setRol(credencial.rol());
        entity.setActualizadoEn(LocalDateTime.now());
        if (nueva) {
            entityManager.persist(entity);
        }
    }

    @Override
    public Optional<Credencial> credencial(String email) {
        return credencialesJpa.buscarPorCorreo(email.trim())
                              .map(entity -> new Credencial(entity.getClienteId(), entity.getCliente().getEmail(), entity.getClaveHash(), entity.getRol()));
    }

    private Cliente toDomain(ClienteJpaEntity entity) {
        return new Cliente(
                entity.getId(),
                entity.getNombre(),
                entity.getEmail(),
                valor(entity.getTelefono()),
                valor(entity.getDireccion()),
                valor(entity.getReferencia()),
                entity.getCreadoEn());
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
