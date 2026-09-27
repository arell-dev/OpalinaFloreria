package com.floral.opalina.controller;

import com.floral.opalina.service.ReporteService;
import com.floral.opalina.model.enums.EstadoPedido;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/reportes")
public class AdminReporteController {
    private final ReporteService reportes;

    public AdminReporteController(ReporteService reportes) {
        this.reportes = reportes;
    }

    @GetMapping
    public String mostrar(@RequestParam(required = false) EstadoPedido estado, Model model) {
        model.addAttribute("resumen", reportes.resumen(estado));
        model.addAttribute("productosVendidos", reportes.productosVendidos(estado));
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("estados", EstadoPedido.values());
        return "admin/reportes";
    }
}
