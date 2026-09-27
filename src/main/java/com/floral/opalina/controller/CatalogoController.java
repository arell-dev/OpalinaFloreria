package com.floral.opalina.controller;

import com.floral.opalina.service.ProductoService;
import com.floral.opalina.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CatalogoController {
    private final ProductoService productoService;

    public CatalogoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(required = false) String q, @RequestParam(required = false) String categoria,  @RequestParam(required = false) String precio,  Model model) {
        model.addAttribute("productos", productoService.listar(q, categoria, precio));
        model.addAttribute("categorias", productoService.categorias());
        model.addAttribute("busqueda", q == null ? "" : q);
        model.addAttribute("categoriaSeleccionada", categoria == null ? "" : categoria);
        model.addAttribute("precioSeleccionado", precio == null ? "todos" : precio);
        return "public/catalogo";
    }

    @GetMapping("/productos/{id}")
    public String detalle(@PathVariable String id, Model model) {
        var producto = productoService.buscarPorId(id)
                                      .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el producto " + id));
                                      
        model.addAttribute("producto", producto);
        return "public/producto";
    }
}
