package com.floral.opalina.repository;

import com.floral.opalina.model.*;
import java.util.*;

public interface ClienteRepository {
    List<Cliente> buscarTodos();

    Optional<Cliente> buscarPorId(String id);

    Optional<Cliente> buscarPorEmail(String email);

    void guardar(Cliente cliente);

    void guardarCredencial(Credencial credencial);

    Optional<Credencial> credencial(String email);
}
