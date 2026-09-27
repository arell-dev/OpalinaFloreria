package com.floral.opalina.session;

import com.floral.opalina.model.UsuarioActual;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class UsuarioSession {
    private UsuarioActual usuario;

    public UsuarioActual getUsuario() {
        return usuario;
    }

    public boolean autenticado() {
        return usuario != null;
    }

    public void iniciar(UsuarioActual usu) {
        usuario = usu;
    }

    public void cerrar() {
        usuario = null;
    }
}
