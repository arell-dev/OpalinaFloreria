package com.floral.opalina.controller;

import com.floral.opalina.model.ConfiguracionTienda;
import com.floral.opalina.service.ConfiguracionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/configuracion")
public class AdminConfiguracionController {
    private final ConfiguracionService configuracionService;

    public AdminConfiguracionController(ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping
    public String mostrar(Model model) {
        model.addAttribute("configuracion", configuracionService.obtener());
        return "admin/configuracion";
    }

    @PostMapping
    public String guardar(@ModelAttribute("configuracion") ConfiguracionTienda configuracion, RedirectAttributes redirect) {
        try {
            configuracionService.guardar(configuracion);
            redirect.addFlashAttribute("mensaje", "Configuración guardada.");
        } catch (IllegalArgumentException error) {
            redirect.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/admin/configuracion";
    }
}
