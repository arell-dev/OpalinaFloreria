package com.floral.opalina.repository.memory;

import com.floral.opalina.model.*;
import com.floral.opalina.repository.ClienteRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@Profile("memory")
public class InMemoryClienteRepository implements ClienteRepository {
    private final List<Cliente> clientes = new ArrayList<>();
    private final List<Credencial> credenciales = new ArrayList<>();

    public synchronized List<Cliente> buscarTodos() {
        return List.copyOf(clientes);
    }

    public synchronized Optional<Cliente> buscarPorId(String clienteId) {
        return clientes.stream().filter(cliente -> cliente.getId().equals(clienteId)).findFirst();
    }

    public synchronized Optional<Cliente> buscarPorEmail(String correo) {
        return clientes.stream().filter(cliente -> cliente.getEmail().equalsIgnoreCase(correo)).findFirst();
    }

    public synchronized void guardar(Cliente cliente) {
        clientes.removeIf(clienteGuardado -> clienteGuardado.getId().equals(cliente.getId()));
        clientes.add(cliente);
    }

    public synchronized void guardarCredencial(Credencial credencial) {
        credenciales.removeIf(credencialGuardada -> credencialGuardada.email().equalsIgnoreCase(credencial.email()));
        credenciales.add(credencial);
    }

    public synchronized Optional<Credencial> credencial(String correo) {
        return credenciales.stream().filter(credencial -> credencial.email().equalsIgnoreCase(correo)).findFirst();
    }
}
