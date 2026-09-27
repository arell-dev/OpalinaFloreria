package com.floral.opalina.controller;

import com.floral.opalina.service.CarritoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CarritoController {
    private final CarritoService carrito;

    public CarritoController(CarritoService carrito) {
        this.carrito = carrito;
    }

    @GetMapping("/carrito")
    public String mostrar(Model model) {
        cargarCarrito(model);
        return "tienda/carrito";
    }

    @PostMapping("/carrito/items")
    public String agregar(@RequestParam String productoId, @RequestParam(defaultValue = "1") int cantidad, RedirectAttributes redirect) {
        try {
            carrito.agregar(productoId, cantidad);
            redirect.addFlashAttribute("mensaje", "Producto agregado al carrito.");
        } catch (IllegalArgumentException error) {
            redirect.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/items/{productoId}/cantidad")
    public String cambiarCantidad(@PathVariable String productoId, @RequestParam(required = false) Integer cantidad, @RequestParam(required = false) Integer ajuste, RedirectAttributes redirect) {
        
        try {
            if (ajuste != null) {
                carrito.ajustar(productoId, ajuste);
            } else if (cantidad != null) {
                carrito.cambiar(productoId, cantidad);
            } else {
                throw new IllegalArgumentException("Indica la cantidad del producto.");
            }
        } catch (IllegalArgumentException error) {
            redirect.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/items/{productoId}/eliminar")
    public String quitar(@PathVariable String productoId) {
        carrito.quitar(productoId);
        return "redirect:/carrito";
    }

    private void cargarCarrito(Model model) {
        model.addAttribute("items", carrito.items());
        model.addAttribute("subtotal", carrito.subtotal());
        model.addAttribute("delivery", carrito.delivery(false));
        model.addAttribute("total", carrito.total(false));
    }
}
