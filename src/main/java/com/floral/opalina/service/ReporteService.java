package com.floral.opalina.service;

import com.floral.opalina.model.ItemPedido;
import com.floral.opalina.model.Pedido;
import com.floral.opalina.model.enums.EstadoPedido;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {
    private final PedidoService pedidos;

    public ReporteService(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    public Resumen resumen(EstadoPedido estado) {
        List<Pedido> seleccionados = seleccionarPedidos(estado);
        BigDecimal ventas = seleccionados.stream()
                                        .filter(pedido -> pedido.getEstado() != EstadoPedido.CANCELADO)
                                        .map(Pedido::getTotal)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal promedio = seleccionados.isEmpty() ? BigDecimal.ZERO : ventas.divide(BigDecimal.valueOf(seleccionados.size()), 2, RoundingMode.HALF_UP);

        long pendientes = seleccionados.stream()
                                       .filter(pedido -> pedido.getEstado() == EstadoPedido.PENDIENTE)
                                       .count();

        long unidades = seleccionados.stream()
                                     .flatMap(pedido -> pedido.getItems().stream())
                                     .mapToLong(ItemPedido::cantidad)
                                     .sum();

        return new Resumen(seleccionados.size(), pendientes, ventas, promedio, unidades);
    }

    public List<VentaProducto> productosVendidos(EstadoPedido estado) {
        Map<String, VentaProducto> ventasPorProducto = new HashMap<>();

        seleccionarPedidos(estado).stream()
                                  .flatMap(pedido -> pedido.getItems().stream())
                                  .forEach(item -> ventasPorProducto.merge(item.productoId(), new VentaProducto(item.productoId(), item.nombreProducto(), item.cantidad(), item.totalLinea()), VentaProducto::combinar));

        return ventasPorProducto.values().stream()
                                         .sorted(Comparator.comparingInt(VentaProducto::cantidad).reversed()
                                         .thenComparing(VentaProducto::nombre))
                                         .toList();
    }

    private List<Pedido> seleccionarPedidos(EstadoPedido estado) {
        return pedidos.listar(estado);
    }

    public record Resumen(int totalPedidos, long pendientes, BigDecimal ventas, BigDecimal promedio, long unidades) { }

    public record VentaProducto(String id, String nombre, int cantidad, BigDecimal total) {
        private VentaProducto combinar(VentaProducto otra) {
            return new VentaProducto(id, nombre, cantidad + otra.cantidad, total.add(otra.total));
        }
    }
}
