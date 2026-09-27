package com.floral.opalina.config;

import com.floral.opalina.service.AuthService;
import com.floral.opalina.service.ConfiguracionService;
import com.floral.opalina.session.CarritoSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class UiModelAdvice {
    private final AuthService authService;
    private final CarritoSession carrito;
    private final ConfiguracionService configuracionService;

    public UiModelAdvice(AuthService authService, CarritoSession carrito, ConfiguracionService configuracionService) {
        this.authService = authService;
        this.carrito = carrito;
        this.configuracionService = configuracionService;
    }

    @ModelAttribute("usuarioActual")
    public Object usuarioActual() {
        return authService.actual();
    }

    @ModelAttribute("productosDistintosCarrito")
    public int productosDistintosCarrito() {
        return carrito.cantidades().size();
    }

    @ModelAttribute("configuracionTienda")
    public Object configuracionTienda() {
        return configuracionService.obtener();
    }
}
