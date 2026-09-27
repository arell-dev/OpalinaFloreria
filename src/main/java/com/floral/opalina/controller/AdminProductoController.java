package com.floral.opalina.controller;

import com.floral.opalina.dto.ProductoForm;
import com.floral.opalina.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/admin/productos")
public class AdminProductoController {
    private final ProductoService productos;

    public AdminProductoController(ProductoService productos) {
        this.productos = productos;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,  @RequestParam(required = false) String categoria, @RequestParam(required = false) String disponibilidad, Model model) {

        prepararListado(model, q, categoria, disponibilidad);
        model.addAttribute("productoForm", new ProductoForm());
        return "admin/productos";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("productoForm") ProductoForm form, BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararListado(model, null, null, null);
            return "admin/productos";
        }

        try {
            productos.crear(form.toProducto());
            redirect.addFlashAttribute("mensaje", "Producto creado.");
            return "redirect:/admin/productos";
        } catch (IllegalArgumentException error) {
            prepararListado(model, null, null, null);
            model.addAttribute("error", error.getMessage());
            return "admin/productos";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable String id, Model model) {
        var producto = productos.buscarPorId(id).orElseThrow();
        model.addAttribute("producto", producto);
        model.addAttribute("productoForm", fromProducto(producto));
        return "admin/producto-detalle";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable String id, @Valid @ModelAttribute("productoForm") ProductoForm form, BindingResult result, Model model, RedirectAttributes redirect) {

        if (result.hasErrors()) {
            model.addAttribute("producto", productos.buscarPorId(id).orElseThrow());
            return "admin/producto-detalle";
        }

        try {
            productos.actualizar(id, form.toProducto());
            redirect.addFlashAttribute("mensaje", "Producto actualizado.");
            return "redirect:/admin/productos/{id}";
        } catch (IllegalArgumentException error) {
            model.addAttribute("producto", productos.buscarPorId(id).orElseThrow());
            model.addAttribute("error", error.getMessage());
            return "admin/producto-detalle";
        }
    }

    @PostMapping("/{id}/disponibilidad")
    public String cambiarDisponibilidad(@PathVariable String id, @RequestParam boolean disponible, RedirectAttributes redirect) {
        productos.cambiarDisponibilidad(id, disponible);
        redirect.addFlashAttribute("mensaje", "Disponibilidad actualizada.");
        return "redirect:/admin/productos";
    }

    private ProductoForm fromProducto(com.floral.opalina.model.Producto producto) {
        ProductoForm form = new ProductoForm();
        form.setNombre(producto.getNombre());
        form.setDescripcion(producto.getDescripcion());
        form.setCategoria(producto.getCategoria());
        form.setPrecio(producto.getPrecio());
        form.setStock(producto.getStock());
        form.setDisponible(producto.isDisponible());
        form.setDestacado(producto.isDestacado());
        form.setSku(producto.getSku());
        form.setImagen(producto.getImagen());
        return form;
    }

    private void prepararListado(Model model, String q, String categoria, String disponibilidad) {
        var productosVisibles = productos.buscarParaAdministracion(q, categoria, disponibilidad);
        model.addAttribute("productos", productosVisibles);
        model.addAttribute("consulta", q == null ? "" : q);
        model.addAttribute("categoriaSeleccionada", categoria == null ? "" : categoria);
        model.addAttribute("disponibilidadSeleccionada", disponibilidad == null ? "" : disponibilidad);
        model.addAttribute("categorias", productos.categorias());
        model.addAttribute("totalProductos", productos.contarTodos());
        model.addAttribute("productosDisponibles", productos.contarDisponibles());
        model.addAttribute("productosSinStock", productos.contarSinStock());
        model.addAttribute("productosDestacados", productos.contarDestacados());
    }
}
