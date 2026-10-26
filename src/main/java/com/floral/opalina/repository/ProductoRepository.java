package com.floral.opalina.repository;

import com.floral.opalina.domain.Producto;
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

    Optional<Producto> buscarPorId(Long id);

    Optional<Producto> buscarPorSku(String sku);

    boolean existeSku(String sku);

    boolean existeSkuEnOtroProducto(String sku, Long productoId);

    Producto guardar(Producto producto);

    void eliminar(Long id);

    void descontarStock(Map<Long, Integer> cantidades);

    void reponerStock(Map<Long, Integer> cantidades);

    boolean apareceEnPedidos(Long productoId);
}
