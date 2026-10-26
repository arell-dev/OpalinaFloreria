package com.floral.opalina.controller;

import com.floral.opalina.domain.Producto;
import com.floral.opalina.dto.AdminProductoForm;
import com.floral.opalina.service.AlmacenamientoImagenProductoService;
import com.floral.opalina.service.ProductoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/productos")
public class AdminProductoController {
    private final ProductoService productos;
    private final AlmacenamientoImagenProductoService almacenamientoImagen;

    public AdminProductoController(ProductoService productos, AlmacenamientoImagenProductoService almacenamientoImagen) {
        this.productos = productos;
        this.almacenamientoImagen = almacenamientoImagen;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q, @RequestParam(required = false) String categoria, @RequestParam(required = false) String disponibilidad, Model model) {
        prepararListado(model, q, categoria, disponibilidad);
        model.addAttribute("productoForm", new AdminProductoForm());
        return "admin/productos";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("productoForm") AdminProductoForm form, BindingResult result, Model model, RedirectAttributes redirect) {
        if (form.getImagen() == null || form.getImagen().isEmpty()) {
            result.rejectValue("imagen", "imagen.required", "Selecciona una imagen para el producto.");
        }
        if (result.hasErrors()) {
            prepararListado(model, null, null, null);
            model.addAttribute("abrirModalProducto", true);
            return "admin/productos";
        }

        String imagenUrl = null;
        try {
            imagenUrl = almacenamientoImagen.guardar(form.getImagen());
            productos.crear(form.toProducto(imagenUrl));
            redirect.addFlashAttribute("mensaje", "Producto creado.");
            return "redirect:/admin/productos";
        } catch (IllegalArgumentException error) {
            almacenamientoImagen.eliminarSiFueSubida(imagenUrl);
            prepararListado(model, null, null, null);
            model.addAttribute("error", error.getMessage());
            model.addAttribute("abrirModalProducto", true);
            return "admin/productos";
        } catch (RuntimeException error) {
            almacenamientoImagen.eliminarSiFueSubida(imagenUrl);
            throw error;
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable String id, Model model) {
        Producto producto = productos.buscarPorId(id).orElseThrow();
        model.addAttribute("producto", producto);
        model.addAttribute("productoForm", fromProducto(producto));
        return "admin/producto-detalle";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable String id, @Valid @ModelAttribute("productoForm") AdminProductoForm form, BindingResult result, Model model, RedirectAttributes redirect) {
        Producto productoActual = productos.buscarPorId(id).orElseThrow();
        if (result.hasErrors()) {
            model.addAttribute("producto", productoActual);
            model.addAttribute("abrirModalProducto", true);
            return "admin/producto-detalle";
        }

        String imagenAnterior = productoActual.getImagen();
        String imagenNueva = null;
        try {
            String imagenUrl = imagenAnterior;
            if (form.getImagen() != null && !form.getImagen().isEmpty()) {
                imagenNueva = almacenamientoImagen.guardar(form.getImagen());
                imagenUrl = imagenNueva;
            }
            productos.actualizar(id, form.toProducto(imagenUrl));
            if (imagenNueva != null) {
                almacenamientoImagen.eliminarSiFueSubida(imagenAnterior);
            }
            redirect.addFlashAttribute("mensaje", "Producto actualizado.");
            return "redirect:/admin/productos/{id}";
        } catch (IllegalArgumentException error) {
            almacenamientoImagen.eliminarSiFueSubida(imagenNueva);
            model.addAttribute("producto", productoActual);
            model.addAttribute("error", error.getMessage());
            model.addAttribute("abrirModalProducto", true);
            return "admin/producto-detalle";
        } catch (RuntimeException error) {
            almacenamientoImagen.eliminarSiFueSubida(imagenNueva);
            throw error;
        }
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, InvalidParameterException.class})
    public String imagenDemasiadoGrande(HttpServletRequest request, Model model) {
        model.addAttribute("error", "La imagen no puede superar los 5 MB.");
        String uri = request.getRequestURI();
        if (uri.endsWith("/admin/productos") || uri.endsWith("/admin/productos/")) {
            prepararListado(model, null, null, null);
            model.addAttribute("productoForm", new AdminProductoForm());
            model.addAttribute("abrirModalProducto", true);
            return "admin/productos";
        }

        String id = uri.substring(uri.lastIndexOf('/') + 1);
        Producto producto = productos.buscarPorId(id).orElseThrow();
        model.addAttribute("producto", producto);
        model.addAttribute("productoForm", fromProducto(producto));
        model.addAttribute("abrirModalProducto", true);
        return "admin/producto-detalle";
    }

    @PostMapping("/{id}/disponibilidad")
    public String cambiarDisponibilidad(@PathVariable String id, @RequestParam boolean disponible, RedirectAttributes redirect) {
        productos.cambiarDisponibilidad(id, disponible);
        redirect.addFlashAttribute("mensaje", "Disponibilidad actualizada.");
        return "redirect:/admin/productos";
    }

    private AdminProductoForm fromProducto(Producto producto) {
        AdminProductoForm form = new AdminProductoForm();
        form.setNombre(producto.getNombre());
        form.setDescripcion(producto.getDescripcion());
        form.setCategoria(producto.getCategoria());
        form.setPrecio(producto.getPrecio());
        form.setStock(producto.getStock());
        form.setDisponible(producto.isDisponible());
        form.setDestacado(producto.isDestacado());
        form.setSku(producto.getSku());
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
