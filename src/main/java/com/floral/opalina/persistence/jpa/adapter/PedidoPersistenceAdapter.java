package com.floral.opalina.persistence.jpa.adapter;

import com.floral.opalina.persistence.jpa.entity.CambioEstadoPedidoJpaEntity;
import com.floral.opalina.persistence.jpa.entity.ClienteJpaEntity;
import com.floral.opalina.persistence.jpa.entity.ConfiguracionTiendaJpaEntity;
import com.floral.opalina.persistence.jpa.entity.DestinatarioJpaEntity;
import com.floral.opalina.persistence.jpa.entity.ItemPedidoJpaEntity;
import com.floral.opalina.persistence.jpa.entity.PedidoJpaEntity;
import com.floral.opalina.persistence.jpa.entity.ProductoJpaEntity;
import com.floral.opalina.persistence.jpa.springdata.PedidoJpaRepository;

import com.floral.opalina.exception.RecursoNoEncontradoException;
import com.floral.opalina.domain.CambioEstado;
import com.floral.opalina.domain.Entrega;
import com.floral.opalina.domain.ItemPedido;
import com.floral.opalina.domain.Pedido;
import com.floral.opalina.domain.enums.EstadoPedido;
import com.floral.opalina.repository.PedidoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class PedidoPersistenceAdapter implements PedidoRepository {
    private final EntityManager entityManager;
    private final PedidoJpaRepository pedidosJpa;
    private final AccesoConfiguracionJpa configuracionJpaAcceso;

    public PedidoPersistenceAdapter(EntityManager entityManager, PedidoJpaRepository pedidosJpa, AccesoConfiguracionJpa configuracionJpaAcceso) {
        this.entityManager = entityManager;
        this.pedidosJpa = pedidosJpa;
        this.configuracionJpaAcceso = configuracionJpaAcceso;
    }

    @Override
    public List<Pedido> buscarTodos() {
        return pedidosJpa.findAllByOrderByCreadoEnDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Pedido> buscarTodosOrdenados() {
        return buscarTodos();
    }

    @Override
    public List<Pedido> buscarPorEstado(EstadoPedido estado) {
        String consulta = "select p from PedidoJpaEntity p "
                        + "where (:estado is null or p.estado = :estado) "
                        + "order by p.creadoEn desc";

        return pedidos(consulta, "estado", estado);
    }

    @Override
    public List<Pedido> buscarParaReporte(EstadoPedido estado, LocalDateTime desde, LocalDateTime hastaExclusivo) {
        String consulta = "select p from PedidoJpaEntity p "
                        + "where (:estado is null or p.estado = :estado) "
                        + "and (:desde is null or p.creadoEn >= :desde) "
                        + "and (:hastaExclusivo is null or p.creadoEn < :hastaExclusivo) "
                        + "order by p.creadoEn desc";

        TypedQuery<PedidoJpaEntity> query = entityManager.createQuery(consulta, PedidoJpaEntity.class).setParameter("estado", estado)
              .setParameter("desde", desde)
              .setParameter("hastaExclusivo", hastaExclusivo);

        return query.getResultList().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Pedido> buscarParaAdministracion(EstadoPedido estado, String busqueda) {
        StringBuilder jpql = new StringBuilder(
                "select p from PedidoJpaEntity p "
                + "join p.cliente c left join p.destinatario d "
                + "where (:estado is null or p.estado = :estado)");

        if (busqueda != null && !busqueda.isBlank()) {
            jpql.append(" and (lower(p.codigo) like :texto "
                       + "or lower(c.nombre) like :texto "
                       + "or lower(p.emailContacto) like :texto "
                       + "or lower(d.nombre) like :texto)");
        }

        jpql.append(" order by p.creadoEn desc");

        TypedQuery<PedidoJpaEntity> query = entityManager.createQuery(jpql.toString(), PedidoJpaEntity.class).setParameter("estado", estado);

        if (busqueda != null && !busqueda.isBlank()) {
            query.setParameter("texto", "%" + busqueda.trim().toLowerCase(Locale.ROOT) + "%");
        }
        return query.getResultList().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Pedido> buscarPorCliente(Long clienteId, EstadoPedido estado) {
        String consulta = "select p from PedidoJpaEntity p "
                        + "where p.cliente.id = :clienteId "
                        + "and (:estado is null or p.estado = :estado) "
                        + "order by p.creadoEn desc";

        TypedQuery<PedidoJpaEntity> query = entityManager.createQuery(consulta, PedidoJpaEntity.class).setParameter("clienteId", clienteId)
              .setParameter("estado", estado);

        return query.getResultList().stream().map(this::toDomain).toList();
    }

    @Override
    public long contar(EstadoPedido estado) {
        return entityManager.createQuery("select count(p) from PedidoJpaEntity p where (:estado is null or p.estado = :estado)", Long.class)
                .setParameter("estado", estado).getSingleResult();
    }

    @Override
    public BigDecimal totalVentas(EstadoPedido estado) {
        String consulta = "select p from PedidoJpaEntity p "
                + "where (:estado is null or p.estado = :estado) "
                + "and p.estado <> :cancelado";

        List<PedidoJpaEntity> resultados = entityManager.createQuery(consulta, PedidoJpaEntity.class).setParameter("estado", estado)
              .setParameter("cancelado", EstadoPedido.CANCELADO)
              .getResultList();

        return resultados.stream().map(this::toDomain).map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Optional<Pedido> buscarPorId(Long id) {
        return pedidosJpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Pedido> buscarPorIdParaActualizar(Long id) {
        return pedidosJpa.buscarPorIdParaActualizar(id).map(this::toDomain);
    }

    @Override
    public Optional<Pedido> buscarPorCodigo(String codigo) {
        return pedidosJpa.findByCodigo(codigo).map(this::toDomain);
    }

    @Override
    public String generarCodigo() {
        ConfiguracionTiendaJpaEntity config = configuracionJpaAcceso.obtenerParaActualizar();
        config.setUltimoNumeroPedido(config.getUltimoNumeroPedido() + 1);
        return "OP-" + Year.now() + "-" + String.format(Locale.ROOT, "%04d", config.getUltimoNumeroPedido());
    }

    @Override
    public Pedido guardar(Pedido pedido) {
        PedidoJpaEntity entity = null;
        if (pedido.getId() != null) {
            entity = pedidosJpa.findById(pedido.getId()).orElse(null);
        }

        if (entity == null) {
            entity = crearPedido(pedido);
            return toDomain(pedidosJpa.saveAndFlush(entity));
        }

        entity.setEstado(pedido.getEstado());
        entity.setEstadoPago(pedido.getEstadoPago());
        int historialPersistido = entity.getHistorial().size();
        List<CambioEstado> cambios = pedido.getHistorial();

        for (int indice = historialPersistido; indice < cambios.size(); indice++) {
            CambioEstado cambio = cambios.get(indice);
            CambioEstadoPedidoJpaEntity nuevoCambio = new CambioEstadoPedidoJpaEntity();
            nuevoCambio.setPedido(entity);
            nuevoCambio.setEstado(cambio.estado());
            nuevoCambio.setFecha(cambio.fecha());
            entity.getHistorial().add(nuevoCambio);
        }

        return toDomain(entity);
    }

    private PedidoJpaEntity crearPedido(Pedido pedido) {
        ClienteJpaEntity cliente = entityManager.find(ClienteJpaEntity.class, pedido.getClienteId());

        if (cliente == null) {
            throw new RecursoNoEncontradoException("No se encontró el cliente del pedido");
        }

        PedidoJpaEntity entity = new PedidoJpaEntity();
        entity.setCodigo(pedido.getCodigo());
        entity.setCliente(cliente);
        entity.setEmailContacto(pedido.getEmail());
        entity.setEstado(pedido.getEstado());
        entity.setEstadoPago(pedido.getEstadoPago());
        entity.setModalidadEntrega(pedido.getEntrega().modalidad());
        entity.setMetodoPago(pedido.getEntrega().metodoPago());
        entity.setFechaSolicitada(pedido.getEntrega().fecha());
        entity.setHoraSolicitada(pedido.getEntrega().hora());
        entity.setCostoDelivery(pedido.getCostoDelivery());
        entity.setCreadoEn(pedido.getCreadoEn());

        DestinatarioJpaEntity destinatario = new DestinatarioJpaEntity();
        destinatario.setPedido(entity);
        destinatario.setNombre(pedido.getNombreCliente());
        destinatario.setTelefono(pedido.getTelefono());
        destinatario.setDireccion(pedido.getEntrega().direccion());
        destinatario.setReferencia(pedido.getEntrega().referencia());
        destinatario.setDedicatoria(pedido.getEntrega().notas());
        entity.setDestinatario(destinatario);

        for (ItemPedido item : pedido.getItems()) {
            ProductoJpaEntity producto = entityManager.find(ProductoJpaEntity.class, item.productoId());

            if (producto == null) {
                throw new RecursoNoEncontradoException("No se encontró el producto " + item.productoId());
            }

            ItemPedidoJpaEntity linea = new ItemPedidoJpaEntity();
            linea.setPedido(entity);
            linea.setProducto(producto);
            linea.setNombreProducto(item.nombreProducto());
            linea.setCategoria(item.categoria());
            linea.setImagen(item.imagen());
            linea.setCantidad(item.cantidad());
            linea.setPrecioUnitario(item.precioUnitario());
            entity.getItems().add(linea);
        }

        for (CambioEstado cambio : pedido.getHistorial()) {
            CambioEstadoPedidoJpaEntity registro = new CambioEstadoPedidoJpaEntity();
            registro.setPedido(entity);
            registro.setEstado(cambio.estado());
            registro.setFecha(cambio.fecha());
            entity.getHistorial().add(registro);
        }

        return entity;
    }

    private Pedido toDomain(PedidoJpaEntity entity) {
        DestinatarioJpaEntity destinatario = entity.getDestinatario();

        List<ItemPedido> items = entity.getItems().stream().map(item -> {
            BigDecimal totalLinea = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            Long productoId = item.getProductoId();
            return new ItemPedido(
                    productoId,
                    item.getNombreProducto(),
                    item.getCategoria(),
                    item.getImagen(),
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    totalLinea);
        }).toList();

        BigDecimal subtotal = items.stream().map(ItemPedido::totalLinea).reduce(BigDecimal.ZERO, BigDecimal::add);

        Entrega entrega = new Entrega(
                entity.getModalidadEntrega(),
                valor(destinatario.getDireccion()),
                valor(destinatario.getReferencia()),
                entity.getFechaSolicitada(),
                entity.getHoraSolicitada(),
                valor(destinatario.getDedicatoria()),
                entity.getMetodoPago());

        List<CambioEstado> historial = entity.getHistorial().stream()
                                             .map(cambio -> new CambioEstado(cambio.getEstado(), cambio.getFecha())).toList();
        return new Pedido(entity.getId(), entity.getCodigo(), entity.getCliente().getId(), destinatario.getNombre(), entity.getEmailContacto(), destinatario.getTelefono(),
                items, entrega, subtotal, entity.getCostoDelivery(), subtotal.add(entity.getCostoDelivery()), entity.getEstado(),
                entity.getEstadoPago(), entity.getCreadoEn(), historial);
    }

    private List<Pedido> pedidos(String jpql, String nombreParametro, Object valorParametro) {
        TypedQuery<PedidoJpaEntity> query = entityManager.createQuery(jpql, PedidoJpaEntity.class);

        if (nombreParametro != null) {
            query.setParameter(nombreParametro, valorParametro);
        }

        return query.getResultList().stream().map(this::toDomain).toList();
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
