package com.floral.opalina.persistence.jpa.adapter;

import com.floral.opalina.persistence.jpa.entity.ProductoJpaEntity;
import com.floral.opalina.persistence.jpa.springdata.ProductoJpaRepository;

import com.floral.opalina.exception.ConflictoNegocioException;
import com.floral.opalina.exception.RecursoNoEncontradoException;
import com.floral.opalina.domain.Producto;
import com.floral.opalina.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class ProductoPersistenceAdapter implements ProductoRepository {
    private final EntityManager entityManager;
    private final ProductoJpaRepository productosJpa;

    public ProductoPersistenceAdapter(EntityManager entityManager, ProductoJpaRepository productosJpa) {
        this.entityManager = entityManager;
        this.productosJpa = productosJpa;
    }

    @Override
    public List<Producto> buscarTodos() {
        return productosJpa.findAllByOrderByNombreAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Producto> buscarCatalogo(String busqueda, String categoria, String rangoPrecio) {
        StringBuilder jpql = new StringBuilder("select p from ProductoJpaEntity p where p.disponible = true");
        Map<String, Object> parametros = new HashMap<>();
        agregarBusqueda(jpql, parametros, busqueda, true);
        agregarCategoria(jpql, parametros, categoria);

        switch (rangoPrecio == null ? "todos" : rangoPrecio) {
            case "economico" -> jpql.append(" and p.precio < 60");
            case "medio" -> jpql.append(" and p.precio >= 60 and p.precio < 120");
            case "premium" -> jpql.append(" and p.precio >= 120 and p.precio < 200");
            case "ceremonial" -> jpql.append(" and p.precio >= 200");
            default -> { }
        }

        jpql.append(" order by p.nombre");
        return buscarProductos(jpql.toString(), parametros);
    }

    @Override
    public List<Producto> buscarParaAdministracion(String busqueda, String categoria, String disponibilidad) {
        StringBuilder jpql = new StringBuilder("select p from ProductoJpaEntity p where 1 = 1");

        Map<String, Object> parametros = new HashMap<>();
        agregarBusqueda(jpql, parametros, busqueda, false);
        agregarCategoria(jpql, parametros, categoria);

        switch (disponibilidad == null ? "" : disponibilidad) {
            case "disponible" -> jpql.append(" and p.disponible = true and p.stock > 0");
            case "sin-stock" -> jpql.append(" and p.stock = 0");
            case "oculto" -> jpql.append(" and p.disponible = false");
            default -> { }
        }

        jpql.append(" order by p.nombre");
        return buscarProductos(jpql.toString(), parametros);
    }

    @Override
    public List<Producto> buscarParaApi(String busqueda, String categoria, Boolean disponible) {
        StringBuilder jpql = new StringBuilder("select p from ProductoJpaEntity p where 1 = 1");

        Map<String, Object> parametros = new HashMap<>();
        agregarBusqueda(jpql, parametros, busqueda, true);
        agregarCategoria(jpql, parametros, categoria);

        if (disponible != null) {
            jpql.append(" and p.disponible = :disponible");
            parametros.put("disponible", disponible);
        }

        jpql.append(" order by p.nombre");
        return buscarProductos(jpql.toString(), parametros);
    }

    @Override
    public List<Producto> buscarDestacadosDisponibles() {
        String consulta = "select p from ProductoJpaEntity p "
                + "where p.destacado = true and p.disponible = true "
                + "order by p.nombre";
        return entityManager.createQuery(consulta, ProductoJpaEntity.class)
                .getResultList().stream().map(this::toDomain).toList();
    }

    @Override
    public List<String> buscarCategorias() {
        String consulta = "select distinct p.categoria from ProductoJpaEntity p "
                        + "order by p.categoria";
        return entityManager.createQuery(consulta, String.class).getResultList();
    }

    @Override
    public long contarTodos() {
        return entityManager.createQuery("select count(p) from ProductoJpaEntity p", Long.class).getSingleResult();
    }

    @Override
    public long contarDisponibles() {
        String consulta = "select count(p) from ProductoJpaEntity p "
                        + "where p.disponible = true and p.stock > 0";
        return entityManager.createQuery(consulta, Long.class).getSingleResult();
    }

    @Override
    public long contarSinStock() {
        return entityManager.createQuery("select count(p) from ProductoJpaEntity p where p.stock = 0", Long.class).getSingleResult();
    }

    @Override
    public long contarDestacados() {
        return entityManager.createQuery("select count(p) from ProductoJpaEntity p where p.destacado = true", Long.class).getSingleResult();
    }

    @Override
    public Optional<Producto> buscarPorId(String id) {
        return productosJpa.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existeSku(String sku) {
        return productosJpa.existsBySkuIgnoreCase(sku);
    }

    @Override
    public boolean existeSkuEnOtroProducto(String sku, String productoId) {
        return productosJpa.existsBySkuIgnoreCaseAndIdNot(sku, productoId);
    }

    @Override
    public void guardar(Producto producto) {
        ProductoJpaEntity entity = entityManager.find(ProductoJpaEntity.class, producto.getId());

        if (entity == null) {
            entity = new ProductoJpaEntity();
            entity.setId(producto.getId());
        }

        entity.setNombre(producto.getNombre());
        entity.setDescripcion(producto.getDescripcion());
        entity.setCategoria(producto.getCategoria());
        entity.setSku(producto.getSku());
        entity.setImagen(producto.getImagen());
        entity.setPrecio(producto.getPrecio());
        entity.setStock(producto.getStock());
        entity.setDisponible(producto.isDisponible());
        entity.setDestacado(producto.isDestacado());
        productosJpa.save(entity);

        ManejoErroresJpa.comprobarIntegridad(entityManager, "El SKU ya está registrado por otro producto");
    }

    @Override
    public void eliminar(String id) {
        String mensaje = "No se puede eliminar un producto que aparece en pedidos. Desactívalo para conservar el historial.";

        if (apareceEnPedidos(id)) {
            throw new ConflictoNegocioException(mensaje);
        }

        ProductoJpaEntity entity = productosJpa.buscarPorIdParaActualizar(id).orElse(null);

        if (entity != null && apareceEnPedidos(id)) {
            throw new ConflictoNegocioException(mensaje);
        }

        if (entity != null) {
            entityManager.remove(entity);
            entityManager.flush();
        }
    }

    @Override
    public void descontarStock(Map<String, Integer> cantidades) {
        for (Map.Entry<String, Integer> linea : cantidades.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            String productoId = linea.getKey();
            int cantidad = linea.getValue();

            if (cantidad < 1) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
            }

            int filasActualizadas = productosJpa.descontarStockDisponible(productoId, cantidad);
            if (filasActualizadas == 0) {
                ProductoJpaEntity producto = productosJpa.findById(productoId)
                                                         .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + productoId));

                throw new IllegalArgumentException("Stock insuficiente de " + producto.getNombre() + ". Disponible: " + producto.getStock());
            }
        }
    }

    @Override
    public void reponerStock(Map<String, Integer> cantidades) {
        for (String id : cantidades.keySet().stream().sorted().toList()) {
            ProductoJpaEntity producto = productosJpa.buscarPorIdParaActualizar(id).orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + id));

            int cantidad = cantidades.get(id);
            Math.addExact(producto.getStock(), cantidad);
            productosJpa.incrementarStock(id, cantidad);

            entityManager.refresh(producto, LockModeType.PESSIMISTIC_WRITE);
        }
    }

    @Override
    public boolean apareceEnPedidos(String productoId) {
        return entityManager.createQuery("select count(i) from ItemPedidoJpaEntity i where i.producto.id = :id", Long.class).setParameter("id", productoId).getSingleResult() > 0;
    }

    private Producto toDomain(ProductoJpaEntity entity) {
        return new Producto(entity.getId(), entity.getNombre(), valor(entity.getDescripcion()), entity.getCategoria(), entity.getPrecio(), entity.getStock(), entity.isDisponible(), entity.isDestacado(), entity.getSku(), valor(entity.getImagen()));
    }

    private List<Producto> buscarProductos(String jpql, Map<String, Object> parametros) {
        TypedQuery<ProductoJpaEntity> query = entityManager.createQuery(jpql, ProductoJpaEntity.class);

        parametros.forEach(query::setParameter);

        return query.getResultList().stream().map(this::toDomain).toList();
    }

    private void agregarBusqueda(StringBuilder jpql, Map<String, Object> parametros, String busqueda, boolean sinSku) {
        if (busqueda == null || busqueda.isBlank()) {
            return;
        }

        jpql.append(" and (lower(p.nombre) like :texto or lower(p.descripcion) like :texto or lower(p.categoria) like :texto");

        if (!sinSku) {
            jpql.append(" or lower(p.sku) like :texto");
        }

        jpql.append(")");
        parametros.put("texto", "%" + busqueda.trim().toLowerCase(Locale.ROOT) + "%");
    }

    private void agregarCategoria(StringBuilder jpql, Map<String, Object> parametros, String categoria) {
        if (categoria != null && !categoria.isBlank()) {
            jpql.append(" and lower(p.categoria) = :categoria");
            parametros.put("categoria", categoria.trim().toLowerCase(Locale.ROOT));
        }
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
