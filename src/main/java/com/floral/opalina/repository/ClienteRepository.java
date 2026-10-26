package com.floral.opalina.repository;

import com.floral.opalina.domain.Cliente;
import com.floral.opalina.domain.Credencial;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository {
    List<Cliente> buscarTodos();

    Optional<Cliente> buscarPorId(Long id);

    Optional<Cliente> buscarPorEmail(String email);

    Cliente guardar(Cliente cliente);

    boolean esAdministrador(Long id);

    void guardarCredencial(Credencial credencial);

    Optional<Credencial> credencial(String email);
}
