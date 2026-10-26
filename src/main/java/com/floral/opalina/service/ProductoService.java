package com.floral.opalina.service;

import com.floral.opalina.domain.Producto;
import com.floral.opalina.repository.ProductoRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.floral.opalina.exception.ConflictoNegocioException;
import com.floral.opalina.exception.RecursoNoEncontradoException;

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

    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.buscarPorId(id);
    }

    public Optional<Producto> buscarPorSku(String sku) {
        return productoRepository.buscarPorSku(sku);
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

    @Transactional
    public Producto crear(Producto producto) {
        if (productoRepository.existeSku(producto.getSku())) {
            throw new ConflictoNegocioException("Ya existe un producto con el SKU " + producto.getSku());
        }

        Producto nuevoProducto = new Producto(
                null,
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria(),
                producto.getPrecio(),
                producto.getStock(),
                producto.isDisponible(),
                producto.isDestacado(),
                producto.getSku(),
                producto.getImagen());

        return productoRepository.guardar(nuevoProducto);
    }

    @Transactional
    public Producto actualizar(Long id, Producto cambios) {
        Producto producto = productoRepository.buscarPorId(id)
                                              .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));

        if (productoRepository.existeSkuEnOtroProducto(cambios.getSku(), id)) {
            throw new ConflictoNegocioException("Ya existe un producto con el SKU " + cambios.getSku());
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

    @Transactional
    public void cambiarDisponibilidad(Long id, boolean disponible) {
        Producto producto = productoRepository.buscarPorId(id)
                                              .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));

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

    @Transactional
    public void eliminar(Long id) {
        if (productoRepository.buscarPorId(id).isEmpty()) {
            throw new RecursoNoEncontradoException("Producto no encontrado");
        }
        productoRepository.eliminar(id);
    }

    public void descontarStock(Map<Long, Integer> cantidades) {
        productoRepository.descontarStock(cantidades);
    }

    public void reponerStock(Map<Long, Integer> cantidades) {
        productoRepository.reponerStock(cantidades);
    }
}
