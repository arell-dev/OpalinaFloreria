package com.floral.opalina.repository.memory;

import com.floral.opalina.model.Producto;
import com.floral.opalina.repository.ProductoRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("memory")
public class InMemoryProductoRepository implements ProductoRepository {
    private final List<Producto> productos = new ArrayList<>();

    @Override
    public synchronized List<Producto> buscarTodos() {
        return List.copyOf(productos);
    }

    @Override
    public synchronized List<Producto> buscarCatalogo(String busqueda, String categoria, String rangoPrecio) {
        String texto = normalizar(busqueda);

        return productos.stream()
                        .filter(Producto::isDisponible)
                        .filter(producto -> coincideCategoria(producto, categoria))

                        .filter(producto -> texto.isBlank() || contiene(producto.getNombre(), texto)  || contiene(producto.getDescripcion(), texto) || contiene(producto.getCategoria(), texto))

                        .filter(producto -> coincideConRango(producto, rangoPrecio))
                        .sorted(Comparator.comparing(Producto::getNombre))
                        .toList();
    }

    @Override
    public synchronized List<Producto> buscarParaAdministracion(String busqueda, String categoria, String disponibilidad) {
        String texto = normalizar(busqueda);

        return productos.stream()
                        .filter(producto -> texto.isBlank() || contiene(producto.getNombre(), texto) || contiene(producto.getDescripcion(), texto) || contiene(producto.getSku(), texto))
                        
                        .filter(producto -> coincideCategoria(producto, categoria))
                        .filter(producto -> coincideDisponibilidad(producto, disponibilidad))
                        .toList();
    }

    @Override
    public synchronized List<Producto> buscarParaApi(String busqueda, String categoria, Boolean disponible) {
        String texto = normalizar(busqueda);

        return productos.stream()
                        .filter(producto -> texto.isBlank() || contiene(producto.getNombre(), texto) || contiene(producto.getCategoria(), texto))

                        .filter(producto -> coincideCategoria(producto, categoria))
                        .filter(producto -> disponible == null || producto.isDisponible() == disponible)
                        .toList();
    }

    @Override
    public synchronized List<Producto> buscarDestacadosDisponibles() {
        return productos.stream().filter(Producto::isDestacado).filter(Producto::isDisponible).toList();
    }

    @Override
    public synchronized List<String> buscarCategorias() {
        return productos.stream().map(Producto::getCategoria).distinct().sorted().toList();
    }

    @Override
    public synchronized long contarTodos() {
        return productos.size();
    }

    @Override
    public synchronized long contarDisponibles() {
        return productos.stream().filter(producto -> producto.isDisponible() && producto.getStock() > 0).count();
    }

    @Override
    public synchronized long contarSinStock() {
        return productos.stream().filter(producto -> producto.getStock() == 0).count();
    }

    @Override
    public synchronized long contarDestacados() {
        return productos.stream().filter(Producto::isDestacado).count();
    }

    @Override
    public synchronized Optional<Producto> buscarPorId(String id) {
        return productos.stream().filter(producto -> producto.getId().equals(id)).findFirst();
    }

    @Override
    public synchronized boolean existeSku(String sku) {
        return productos.stream().anyMatch(producto -> producto.getSku().equalsIgnoreCase(sku));
    }

    @Override
    public synchronized boolean existeSkuEnOtroProducto(String sku, String productoId) {
        return productos.stream().anyMatch(producto -> !producto.getId().equals(productoId) && producto.getSku().equalsIgnoreCase(sku));
    }

    @Override
    public synchronized void guardar(Producto producto) {
        boolean skuDuplicado = existeSkuEnOtroProducto(producto.getSku(), producto.getId());
        if (skuDuplicado) {
            throw new IllegalArgumentException("Ya existe un producto con el SKU " + producto.getSku());
        }

        int indiceExistente = buscarIndice(producto.getId());
        if (indiceExistente >= 0) {
            productos.set(indiceExistente, producto);
            return;
        }

        productos.add(producto);
    }

    @Override
    public synchronized void eliminar(String id) {
        productos.removeIf(producto -> producto.getId().equals(id));
    }

    @Override
    public synchronized void descontarStock(Map<String, Integer> cantidades) {
        Map<Producto, Integer> ajustes = new LinkedHashMap<>();

        for (var entrada : cantidades.entrySet()) {
            Producto producto = buscarPorId(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

            int cantidad = entrada.getValue();
            if (!producto.isComprable() || cantidad < 1 || cantidad > producto.getStock()) {
                throw new IllegalArgumentException("Stock insuficiente de " + producto.getNombre());
            }
            ajustes.put(producto, cantidad);
        }

        ajustes.forEach(Producto::descontar);
    }

    private int buscarIndice(String id) {
        for (int indice = 0; indice < productos.size(); indice++) {
            if (productos.get(indice).getId().equals(id)) {
                return indice;
            }
        }
        return -1;
    }

    private boolean coincideConRango(Producto producto, String rangoPrecio) {
        if (rangoPrecio == null || rangoPrecio.isBlank() || rangoPrecio.equals("todos")) {
            return true;
        }

        BigDecimal precio = producto.getPrecio();
        return switch (rangoPrecio) {
            case "economico" -> precio.compareTo(new BigDecimal("60")) < 0;
            case "medio" -> precio.compareTo(new BigDecimal("60")) >= 0 && precio.compareTo(new BigDecimal("120")) < 0;
            case "premium" -> precio.compareTo(new BigDecimal("120")) >= 0 && precio.compareTo(new BigDecimal("200")) < 0;
            case "ceremonial" -> precio.compareTo(new BigDecimal("200")) >= 0;
            default -> true;
        };
    }

    private boolean coincideDisponibilidad(Producto producto, String disponibilidad) {
        return switch (disponibilidad == null ? "" : disponibilidad) {
            case "disponible" -> producto.isDisponible() && producto.getStock() > 0;
            case "sin-stock" -> producto.getStock() == 0;
            case "oculto" -> !producto.isDisponible();
            default -> true;
        };
    }

    private boolean coincideCategoria(Producto producto, String categoria) {
        return categoria == null || categoria.isBlank() || producto.getCategoria().equalsIgnoreCase(categoria);
    }

    private boolean contiene(String valor, String texto) {
        return valor.toLowerCase(Locale.ROOT).contains(texto);
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
}
