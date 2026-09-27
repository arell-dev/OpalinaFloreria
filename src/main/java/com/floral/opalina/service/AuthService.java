package com.floral.opalina.service;

import com.floral.opalina.model.UsuarioActual;
import com.floral.opalina.model.enums.Rol;
import com.floral.opalina.repository.ClienteRepository;
import com.floral.opalina.session.UsuarioSession;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final ClienteRepository repositorio;
    private final UsuarioSession usuarioSession;

    public AuthService(ClienteRepository repositorio, UsuarioSession usuarioSession) {
        this.repositorio = repositorio;
        this.usuarioSession = usuarioSession;
    }

    public boolean ingresar(String email, String clave) {
        var credencial = repositorio.credencial(email);
        if (credencial.isEmpty() || !credencial.get().claveTemporal().equals(clave)) {
            return false;
        }

        var cliente = repositorio.buscarPorId(credencial.get().clienteId()).orElse(null);
        if (cliente == null) {
            return false;
        }

        usuarioSession.iniciar(new UsuarioActual(cliente.getId(), cliente.getNombre(), credencial.get().rol()));
        return true;
    }

    public void salir() {
        usuarioSession.cerrar();
    }

    public UsuarioActual actual() {
        return usuarioSession.getUsuario();
    }

    public boolean esAdmin() {
        return usuarioSession.autenticado() && usuarioSession.getUsuario().rol() == Rol.ADMIN;
    }
}
