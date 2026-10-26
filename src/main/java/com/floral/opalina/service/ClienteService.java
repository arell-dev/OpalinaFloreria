package com.floral.opalina.service;

import com.floral.opalina.exception.ConflictoNegocioException;
import com.floral.opalina.domain.Cliente;
import com.floral.opalina.domain.Credencial;
import com.floral.opalina.domain.enums.Rol;
import com.floral.opalina.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ClienteService {
    private final ClienteRepository repositorio;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository repositorio, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Cliente> listar() {
        return repositorio.buscarTodos();
    }

    public Optional<Cliente> porId(Long id) {
        return repositorio.buscarPorId(id);
    }

    public boolean esAdministrador(Long id) {
        return repositorio.esAdministrador(id);
    }

    public Optional<Cliente> porEmail(String email) {
        return repositorio.buscarPorEmail(email);
    }

    @Transactional
    public Cliente registrar(String nombre, String email, String telefono, String clave) {
        email = email.trim().toLowerCase(Locale.ROOT);

        if (repositorio.buscarPorEmail(email).isPresent()) {
            throw new ConflictoNegocioException("Ya existe una cuenta con ese correo");
        }

        Cliente cliente = repositorio.guardar(new Cliente(null, nombre, email, telefono, "", "", LocalDateTime.now()));
        repositorio.guardarCredencial(new Credencial(cliente.getId(), email, passwordEncoder.encode(clave), Rol.CLIENTE));

        return cliente;
    }

    @Transactional
    public Cliente actualizar(Long id, String nombre, String telefono, String direccion, String referencia) {
        Cliente actual = repositorio.buscarPorId(id)
                                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Cliente actualizado = actual.actualizar(nombre, telefono, direccion, referencia);
        repositorio.guardar(actualizado);
        return actualizado;
    }
}
