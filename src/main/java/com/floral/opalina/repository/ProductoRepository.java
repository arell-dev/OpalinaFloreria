package com.floral.opalina.repository;

import com.floral.opalina.model.Producto;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductoRepository {
    List<Producto> buscarTodos();

    List<Producto> buscarCatalogo(String busqueda, String categoria, String rangoPrecio);

    List<Producto> buscarParaAdministracion(String busqueda, String categoria, String disponibilidad);

    List<Producto> buscarParaApi(String busqueda, String categoria, Boolean disponible);

    List<Producto> buscarDestacadosDisponibles();

    List<String> buscarCategorias();

    long contarTodos();

    long contarDisponibles();

    long contarSinStock();

    long contarDestacados();

    Optional<Producto> buscarPorId(String id);

    boolean existeSku(String sku);

    boolean existeSkuEnOtroProducto(String sku, String productoId);

    void guardar(Producto producto);

    void eliminar(String id);

    void descontarStock(Map<String, Integer> cantidades);
}
