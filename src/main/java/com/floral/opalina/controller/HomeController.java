package com.floral.opalina.controller;

import com.floral.opalina.service.ProductoService;
import com.floral.opalina.dto.ContactoForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    private final ProductoService productoService;

    public HomeController(ProductoService productoService) {
        this.productoService = productoService;
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
        model.addAttribute("contactoForm", new ContactoForm());
        return "public/contacto";
    }

    @PostMapping("/contacto")
    public String enviarContacto(@Valid @ModelAttribute("contactoForm") ContactoForm formulario,  BindingResult result, Model model, RedirectAttributes redirect) {
        
        if (result.hasErrors()) {
            return "public/contacto";
        }
        redirect.addFlashAttribute("mensaje", "Gracias por escribirnos. Te responderemos pronto.");
        return "redirect:/contacto";
    }
}
