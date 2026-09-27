package com.floral.opalina.controller;

import com.floral.opalina.dto.LoginForm;
import com.floral.opalina.dto.RegistroForm;
import com.floral.opalina.service.AuthService;
import com.floral.opalina.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final AuthService authService;
    private final ClienteService clienteService;

    public AuthController(AuthService authService, ClienteService clienteService) {
        this.authService = authService;
        this.clienteService = clienteService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String returnTo, Model model) {
        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("returnTo", returnTo == null ? "" : returnTo);
        return "auth/login";
    }

    @PostMapping("/login")
    public String ingresar(@Valid @ModelAttribute("loginForm") LoginForm form, BindingResult result, @RequestParam(required = false) String returnTo, Model model) {

        if (result.hasErrors()) {
            model.addAttribute("returnTo", returnTo);
            return "auth/login";
        }
        if (!authService.ingresar(form.getEmail(), form.getClave())) {
            model.addAttribute("error", "Correo o clave incorrectos.");
            model.addAttribute("returnTo", returnTo);
            return "auth/login";
        }

        if (returnTo != null && returnTo.startsWith("/") && !returnTo.startsWith("//")) {
            return "redirect:" + returnTo;
        }

        return authService.esAdmin() ? "redirect:/admin/pedidos" : "redirect:/cuenta/perfil";
    }

    @PostMapping("/logout")
    public String salir() {
        authService.salir();
        return "redirect:/";
    }

    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("registroForm", new RegistroForm());
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registroForm") RegistroForm form, BindingResult result, Model model, RedirectAttributes redirect) {

        if (result.hasErrors()) {
            return "auth/registro";
        }
        
        try {
            clienteService.registrar(form.getNombre(), form.getEmail(), form.getTelefono(), form.getClave());
            redirect.addFlashAttribute("mensaje", "Cuenta creada. Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (IllegalArgumentException error) {
            model.addAttribute("error", error.getMessage());
            return "auth/registro";
        }
    }
}
