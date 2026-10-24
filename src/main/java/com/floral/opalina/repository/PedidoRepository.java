package com.floral.opalina.repository;

import com.floral.opalina.domain.Pedido;
import com.floral.opalina.domain.enums.EstadoPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository {
    List<Pedido> buscarTodos();

    List<Pedido> buscarTodosOrdenados();

    List<Pedido> buscarPorEstado(EstadoPedido estado);

    List<Pedido> buscarParaReporte(EstadoPedido estado, LocalDateTime desde, LocalDateTime hastaExclusivo);

    List<Pedido> buscarParaAdministracion(EstadoPedido estado, String busqueda);

    List<Pedido> buscarPorCliente(String clienteId, EstadoPedido estado);

    long contar(EstadoPedido estado);

    BigDecimal totalVentas(EstadoPedido estado);

    Optional<Pedido> buscarPorId(String id);

    Optional<Pedido> buscarPorIdParaActualizar(String id);

    String generarId();

    Pedido guardar(Pedido pedido);
}
