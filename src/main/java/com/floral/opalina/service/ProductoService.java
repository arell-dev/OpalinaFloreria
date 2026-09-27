package com.floral.opalina.service;

import com.floral.opalina.model.Producto;
import com.floral.opalina.repository.ProductoRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProductoService {
    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listar(String busqueda, String categoria) {
        return listar(busqueda, categoria, "todos");
    }

    public List<Producto> listar(String busqueda, String categoria, String rangoPrecio) {
        return productoRepository.buscarCatalogo(busqueda, categoria, rangoPrecio);
    }

    public List<Producto> buscarParaAdministracion(String busqueda, String categoria, String disponibilidad) {
        return productoRepository.buscarParaAdministracion(busqueda, categoria, disponibilidad);
    }

    public List<Producto> buscarParaApi(String busqueda, String categoria, Boolean disponible) {
        return productoRepository.buscarParaApi(busqueda, categoria, disponible);
    }

    public List<Producto> destacados() {
        return productoRepository.buscarDestacadosDisponibles();
    }

    public List<String> categorias() {
        return productoRepository.buscarCategorias();
    }

    public Optional<Producto> buscarPorId(String id) {
        return productoRepository.buscarPorId(id);
    }

    public List<Producto> listarTodos() {
        return productoRepository.buscarTodos();
    }

    public long contarTodos() {
        return productoRepository.contarTodos();
    }

    public long contarDisponibles() {
        return productoRepository.contarDisponibles();
    }

    public long contarSinStock() {
        return productoRepository.contarSinStock();
    }

    public long contarDestacados() {
        return productoRepository.contarDestacados();
    }

    public synchronized Producto crear(Producto producto) {
        if (productoRepository.existeSku(producto.getSku())) {
            throw new IllegalArgumentException("Ya existe un producto con el SKU " + producto.getSku());
        }

        Producto nuevoProducto = new Producto(
                UUID.randomUUID().toString(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria(),
                producto.getPrecio(),
                producto.getStock(),
                producto.isDisponible(),
                producto.isDestacado(),
                producto.getSku(),
                producto.getImagen());

        productoRepository.guardar(nuevoProducto);
        return nuevoProducto;
    }

    public synchronized Producto actualizar(String id, Producto cambios) {
        Producto producto = productoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        if (productoRepository.existeSkuEnOtroProducto(cambios.getSku(), id)) {
            throw new IllegalArgumentException("Ya existe un producto con el SKU " + cambios.getSku());
        }

        producto.actualizar(
                cambios.getNombre(),
                cambios.getDescripcion(),
                cambios.getCategoria(),
                cambios.getPrecio(),
                cambios.getStock(),
                cambios.isDisponible(),
                cambios.isDestacado(),
                cambios.getSku(),
                cambios.getImagen());
        productoRepository.guardar(producto);
        return producto;
    }

    public synchronized void cambiarDisponibilidad(String id, boolean disponible) {
        Producto producto = productoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        producto.actualizar(
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria(),
                producto.getPrecio(),
                producto.getStock(),
                disponible,
                producto.isDestacado(),
                producto.getSku(),
                producto.getImagen());
        productoRepository.guardar(producto);
    }

    public void eliminar(String id) {
        if (productoRepository.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("Producto no encontrado");
        }
        productoRepository.eliminar(id);
    }

    public void descontarStock(Map<String, Integer> cantidades) {
        productoRepository.descontarStock(cantidades);
    }
}
