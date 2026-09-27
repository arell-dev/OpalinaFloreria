package com.floral.opalina.controller;

import com.floral.opalina.dto.CheckoutForm;
import com.floral.opalina.model.enums.MetodoPago;
import com.floral.opalina.model.enums.ModalidadEntrega;
import com.floral.opalina.service.CarritoService;
import com.floral.opalina.service.ClienteService;
import com.floral.opalina.service.PedidoService;
import com.floral.opalina.session.UsuarioSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PedidoController {
    private final PedidoService pedidoService;
    private final CarritoService carrito;
    private final ClienteService clientes;
    private final UsuarioSession usuario;

    public PedidoController(PedidoService pedidoService, CarritoService carrito, ClienteService clientes, UsuarioSession usuario) {
        this.pedidoService = pedidoService;
        this.carrito = carrito;
        this.clientes = clientes;
        this.usuario = usuario;
    }

    @GetMapping("/pedido")
    public String checkout(@RequestParam(required = false) String confirmado, Model model) {
        CheckoutForm form = new CheckoutForm();
        var cliente = clientes.porId(usuario.getUsuario().id()).orElseThrow();

        form.setNombreReceptor(cliente.getNombre());
        form.setEmail(cliente.getEmail());
        form.setTelefono(cliente.getTelefono());
        form.setDireccion(cliente.getDireccion());
        form.setReferencia(cliente.getReferencia());
        model.addAttribute("checkoutForm", form);
        model.addAttribute("items", carrito.items());
        model.addAttribute("subtotal", carrito.subtotal());
        model.addAttribute("delivery", carrito.delivery(false));
        model.addAttribute("total", carrito.total(false));
        model.addAttribute("modalidades", ModalidadEntrega.values());
        model.addAttribute("metodosPago", MetodoPago.values());

        if (confirmado != null) {
            pedidoService.porId(confirmado).filter(p -> p.getClienteId().equals(usuario.getUsuario().id()))
                                           .ifPresent(p -> model.addAttribute("pedidoConfirmado", p));
        }
        return "tienda/pedido";
    }

    @PostMapping("/pedido/confirmar")
    public String confirmar(@Valid @ModelAttribute("checkoutForm") CheckoutForm form, BindingResult result, Model model, RedirectAttributes redirect) {

        if (form.getModalidad() == ModalidadEntrega.DELIVERY && (form.getDireccion() == null || form.getDireccion().isBlank())) {
            result.rejectValue("direccion", "NotBlank", "Ingresa la dirección completa para el delivery.");
        }

        if (result.hasErrors()) {
            cargarCheckout(model);
            return "tienda/pedido";
        }
        
        try {
            var pedido = pedidoService.crear(form);
            return "redirect:/pedido?confirmado=" + pedido.getId();
        } catch (IllegalArgumentException error) {
            model.addAttribute("error", error.getMessage());
            cargarCheckout(model);
            return "tienda/pedido";
        }
    }

    @GetMapping("/confirmacion")
    public String compatibilidad() {
        return "redirect:/cuenta/pedidos";
    }

    private void cargarCheckout(Model model) {
        model.addAttribute("items", carrito.items());
        model.addAttribute("subtotal", carrito.subtotal());
        model.addAttribute("delivery", carrito.delivery(false));
        model.addAttribute("total", carrito.total(false));
        model.addAttribute("modalidades", ModalidadEntrega.values());
        model.addAttribute("metodosPago", MetodoPago.values());
    }
}
