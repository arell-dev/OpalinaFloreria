package com.floral.opalina.config;

import com.floral.opalina.model.enums.Rol;
import com.floral.opalina.session.UsuarioSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccesoInterceptor implements HandlerInterceptor {
    private final UsuarioSession usuarioSession;

    public AccesoInterceptor(UsuarioSession usuarioSession) {
        this.usuarioSession = usuarioSession;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (!usuarioSession.autenticado()) {
            response.sendRedirect("/login?returnTo=" + path);
            return false;
        }

        boolean esRutaDeCliente = path.startsWith("/cuenta") || path.startsWith("/pedido");
        if (esRutaDeCliente && usuarioSession.getUsuario().rol() != Rol.CLIENTE) {
            response.sendRedirect("/admin/pedidos");
            return false;
        }

        if (path.startsWith("/admin") && usuarioSession.getUsuario().rol() != Rol.ADMIN) {
            response.sendRedirect("/cuenta/perfil?error=acceso");
            return false;
        }
        
        return true;
    }
}
