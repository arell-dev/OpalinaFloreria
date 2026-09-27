package com.floral.opalina.service;

import com.floral.opalina.model.Cliente;
import com.floral.opalina.model.Credencial;
import com.floral.opalina.model.enums.Rol;
import com.floral.opalina.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ClienteService {
    private final ClienteRepository repositorio;

    public ClienteService(ClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Cliente> listar() {
        return repositorio.buscarTodos();
    }

    public Optional<Cliente> porId(String id) {
        return repositorio.buscarPorId(id);
    }

    public Optional<Cliente> porEmail(String email) {
        return repositorio.buscarPorEmail(email);
    }

    public Cliente registrar(String nombre, String email, String telefono, String clave) {
        if (repositorio.buscarPorEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }

        String id = "CLI-" + UUID.randomUUID().toString().substring(0, 8);
        Cliente cliente = new Cliente(id, nombre, email, telefono, "", "", LocalDateTime.now());
        repositorio.guardar(cliente);
        repositorio.guardarCredencial(new Credencial(id, email, clave, Rol.CLIENTE));
        return cliente;
    }

    public Cliente actualizar(String id, String nombre, String telefono, String direccion, String referencia) {
        Cliente actual = repositorio.buscarPorId(id)
                                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
                                    
        Cliente actualizado = actual.actualizar(nombre, telefono, direccion, referencia);
        repositorio.guardar(actualizado);
        return actualizado;
    }
}
