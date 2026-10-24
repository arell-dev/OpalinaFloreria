package com.floral.opalina.controller;

import com.floral.opalina.domain.enums.ModalidadEntrega;
import com.floral.opalina.dto.CheckoutForm;
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
    public String checkout(@RequestParam(required = false) String confirmado, Model model, RedirectAttributes redirect) {
        var pedidoConfirmado = confirmado == null ? null : pedidoService.porId(confirmado)
                .filter(pedido -> pedido.getClienteId().equals(usuario.getUsuario().id()))
                .orElse(null);

        if (pedidoConfirmado == null && carrito.vacio()) {
            redirect.addFlashAttribute("error", "Tu carrito está vacío. Agrega al menos un producto para iniciar un pedido.");
            return "redirect:/carrito";
        }

        CheckoutForm form = new CheckoutForm();
        var cliente = clientes.porId(usuario.getUsuario().id()).orElseThrow();

        form.setNombreReceptor(cliente.getNombre());
        form.setTelefono(cliente.getTelefono());
        form.setDireccion(cliente.getDireccion());
        form.setReferencia(cliente.getReferencia());
        model.addAttribute("checkoutForm", form);
        cargarCheckout(model, form);

        if (pedidoConfirmado != null) {
            model.addAttribute("pedidoConfirmado", pedidoConfirmado);
        }
        return "tienda/pedido";
    }

    @PostMapping("/pedido/confirmar")
    public String confirmar(@Valid @ModelAttribute("checkoutForm") CheckoutForm form, BindingResult result, Model model, RedirectAttributes redirect) {
        if (carrito.vacio()) {
            redirect.addFlashAttribute("error", "Tu carrito está vacío. Agrega al menos un producto para iniciar un pedido.");
            return "redirect:/carrito";
        }

        if (form.getModalidad() == ModalidadEntrega.DELIVERY && (form.getDireccion() == null || form.getDireccion().isBlank())) {
            result.rejectValue("direccion", "NotBlank", "Ingresa la dirección completa para el delivery.");
        }

        if (result.hasErrors()) {
            cargarCheckout(model, form);
            return "tienda/pedido";
        }
        
        try {
            var pedido = pedidoService.crear(form);
            return "redirect:/pedido?confirmado=" + pedido.getId();
        } catch (IllegalArgumentException error) {
            if (carrito.vacio()) {
                redirect.addFlashAttribute("error", error.getMessage());
                return "redirect:/carrito";
            }

            model.addAttribute("error", error.getMessage());
            cargarCheckout(model, form);
            return "tienda/pedido";
        }
    }

    @GetMapping("/confirmacion")
    public String compatibilidad() {
        return "redirect:/cuenta/pedidos";
    }

    private void cargarCheckout(Model model, CheckoutForm form) {
        boolean recojo = form.getModalidad() == ModalidadEntrega.RECOJO;
        var subtotal = carrito.subtotal();
        var delivery = carrito.delivery(recojo);

        model.addAttribute("items", carrito.items());
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("delivery", delivery);
        model.addAttribute("deliveryCost", carrito.delivery(false));
        model.addAttribute("total", subtotal.add(delivery));
        model.addAttribute("pickup", recojo);
    }
}
