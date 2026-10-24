package com.floral.opalina.controller;

import com.floral.opalina.domain.enums.Rol;
import com.floral.opalina.dto.ContactoForm;
import com.floral.opalina.service.ClienteService;
import com.floral.opalina.service.ContactoService;
import com.floral.opalina.service.ProductoService;
import com.floral.opalina.session.UsuarioSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final ContactoService contactoService;
    private final UsuarioSession usuarioSession;

    public HomeController(ProductoService productoService, ClienteService clienteService, ContactoService contactoService, UsuarioSession usuarioSession) {
        this.productoService = productoService;
        this.clienteService = clienteService;
        this.contactoService = contactoService;
        this.usuarioSession = usuarioSession;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("destacados", productoService.destacados());
        return "public/index";
    }

    @GetMapping("/nosotros")
    public String nosotros() {
        return "public/nosotros";
    }

    @GetMapping("/contacto")
    public String contacto(Model model) {
        ContactoForm formulario = new ContactoForm();
        var usuario = usuarioSession.getUsuario();

        if (usuario != null && usuario.rol() == Rol.CLIENTE) {
            clienteService.porId(usuario.id()).ifPresent(cliente -> {
                formulario.setNombre(cliente.getNombre());
                formulario.setEmail(cliente.getEmail());
                formulario.setTelefono(cliente.getTelefono());
            });
        }

        model.addAttribute("contactoForm", formulario);
        return "public/contacto";
    }

    @PostMapping("/contacto")
    public String enviarContacto(@Valid @ModelAttribute("contactoForm") ContactoForm formulario, BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "public/contacto";
        }
        String clienteId = usuarioSession.autenticado() ? usuarioSession.getUsuario().id() : null;
        contactoService.enviar(formulario, clienteId);
        redirect.addFlashAttribute("mensaje", "Gracias por escribirnos. Te responderemos pronto.");
        return "redirect:/contacto";
    }
}
