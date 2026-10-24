package com.floral.opalina.repository;

import com.floral.opalina.domain.Cliente;
import com.floral.opalina.domain.Credencial;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository {
    List<Cliente> buscarTodos();

    Optional<Cliente> buscarPorId(String id);

    Optional<Cliente> buscarPorEmail(String email);

    void guardar(Cliente cliente);

    void guardarCredencial(Credencial credencial);

    Optional<Credencial> credencial(String email);
}
