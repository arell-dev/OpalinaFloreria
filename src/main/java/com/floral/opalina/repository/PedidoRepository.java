package com.floral.opalina.repository;

import com.floral.opalina.model.Pedido;
import com.floral.opalina.model.enums.EstadoPedido;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository {
    List<Pedido> buscarTodos();

    List<Pedido> buscarTodosOrdenados();

    List<Pedido> buscarPorEstado(EstadoPedido estado);

    List<Pedido> buscarParaAdministracion(EstadoPedido estado, String busqueda);

    List<Pedido> buscarPorCliente(String clienteId, EstadoPedido estado);

    long contar(EstadoPedido estado);

    BigDecimal totalVentas(EstadoPedido estado);

    Optional<Pedido> buscarPorId(String id);

    String generarId();

    void guardar(Pedido pedido);
}
