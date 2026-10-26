package com.floral.opalina.dto;

import com.floral.opalina.domain.enums.EstadoPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReporteDatos( Resumen resumen, List<VentasMes> ventasPorMes, List<EstadoCantidad> pedidosPorEstado, List<ProductoSolicitado> productosSolicitados, List<PedidoFila> pedidos) {

    public ReporteDatos {
        ventasPorMes = List.copyOf(ventasPorMes);
        pedidosPorEstado = List.copyOf(pedidosPorEstado);
        productosSolicitados = List.copyOf(productosSolicitados);
        pedidos = List.copyOf(pedidos);
    }

    public static ReporteDatos vacio() {
        return new ReporteDatos(
               new Resumen(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0), List.of(), List.of(), List.of(), List.of());
    }

    public record Resumen( int totalPedidos, long pedidosPendientes, long pedidosCancelados, BigDecimal valorPedidos, BigDecimal promedioPedido, long unidadesSolicitadas) { }

    public record VentasMes(String etiqueta, long cantidad, BigDecimal importe, int porcentaje) { }

    public record EstadoCantidad( EstadoPedido estado, String etiqueta, long cantidad, int porcentaje, String claseCss) { }

    public record ProductoSolicitado( Long productoId, String nombre, long unidades, BigDecimal importe, int porcentaje) { }

    public record PedidoFila( Long id, String codigo, LocalDateTime fecha, String cliente, String correo, EstadoPedido estado, String estadoEtiqueta, String estadoCss, long unidades, BigDecimal importe) { }
}
