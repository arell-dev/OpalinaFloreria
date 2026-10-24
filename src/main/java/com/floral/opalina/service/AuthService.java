package com.floral.opalina.service;

import com.floral.opalina.domain.UsuarioActual;
import com.floral.opalina.domain.enums.Rol;
import com.floral.opalina.repository.ClienteRepository;
import com.floral.opalina.session.UsuarioSession;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {
    private final ClienteRepository repositorio;
    private final UsuarioSession usuarioSession;
    private final PasswordEncoder passwordEncoder;

    public AuthService(ClienteRepository repositorio, UsuarioSession usuarioSession, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.usuarioSession = usuarioSession;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean ingresar(String email, String clave) {
        var credencial = repositorio.credencial(email);
        if (credencial.isEmpty() || !passwordEncoder.matches(clave, credencial.get().claveHash())) {
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
