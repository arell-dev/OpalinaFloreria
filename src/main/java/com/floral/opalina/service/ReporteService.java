package com.floral.opalina.service;

import com.floral.opalina.domain.ItemPedido;
import com.floral.opalina.domain.Pedido;
import com.floral.opalina.domain.enums.EstadoPedido;
import com.floral.opalina.dto.ReporteDatos;
import com.floral.opalina.repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class ReporteService {
    private static final Locale IDIOMA = Locale.forLanguageTag("es-PE");

    private final PedidoRepository pedidos;

    public ReporteService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    public ReporteDatos generar(EstadoPedido estado, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta == null ? null : hasta.plusDays(1).atStartOfDay();
        List<Pedido> pedidosFiltrados = pedidos.buscarParaReporte(estado, inicio, finExclusivo);
        List<Pedido> pedidosActivos = pedidosFiltrados.stream()
                                                      .filter(pedido -> pedido.getEstado() != EstadoPedido.CANCELADO)
                                                      .toList();

        BigDecimal valorPedidos = pedidosActivos.stream()
                                                .map(Pedido::getTotal)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal promedio = pedidosActivos.isEmpty() ? BigDecimal.ZERO : valorPedidos.divide(BigDecimal.valueOf(pedidosActivos.size()), 2, RoundingMode.HALF_UP);

        long unidades = pedidosActivos.stream()
                                      .flatMap(pedido -> pedido.getItems().stream())
                                      .mapToLong(ItemPedido::cantidad)
                                      .sum();

        ReporteDatos.Resumen resumen = new ReporteDatos.Resumen(
                pedidosFiltrados.size(),
                contar(pedidosFiltrados, EstadoPedido.PENDIENTE),
                contar(pedidosFiltrados, EstadoPedido.CANCELADO),
                valorPedidos,
                promedio,
                unidades);

        return new ReporteDatos(
                resumen,
                ventasPorMes(pedidosActivos),
                pedidosPorEstado(pedidosFiltrados),
                productosSolicitados(pedidosActivos),
                pedidosFiltrados.stream().map(this::filaPedido).toList());
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
    }

    private List<ReporteDatos.VentasMes> ventasPorMes(List<Pedido> pedidosActivos) {
        Map<YearMonth, AcumuladoMes> importes = new TreeMap<>();

        pedidosActivos.forEach(pedido -> {
            YearMonth mes = YearMonth.from(pedido.getCreadoEn());

            AcumuladoMes acumulado = importes.getOrDefault(mes, new AcumuladoMes(0, BigDecimal.ZERO));

            importes.put(mes, new AcumuladoMes(acumulado.cantidad() + 1, acumulado.importe().add(pedido.getTotal())));
        });

        BigDecimal mayorImporte = importes.values().stream()
                                                   .map(AcumuladoMes::importe)
                                                   .max(BigDecimal::compareTo)
                                                   .orElse(BigDecimal.ZERO);

        return importes.entrySet().stream()
                                  .map(entrada -> new ReporteDatos.VentasMes(
                                            etiqueta(entrada.getKey()),
                                            entrada.getValue().cantidad(),
                                            entrada.getValue().importe(),
                                            porcentaje(entrada.getValue().importe(), mayorImporte)))
                                  .toList();
    }

    private List<ReporteDatos.EstadoCantidad> pedidosPorEstado(List<Pedido> pedidosFiltrados) {
        Map<EstadoPedido, Long> cantidades = new EnumMap<>(EstadoPedido.class);
        pedidosFiltrados.forEach(pedido -> cantidades.merge(pedido.getEstado(), 1L, Long::sum));
        long total = pedidosFiltrados.size();

        return Arrays.stream(EstadoPedido.values())
                               .map(estado -> new ReporteDatos.EstadoCantidad(
                                        estado,
                                        etiqueta(estado),
                                        cantidades.getOrDefault(estado, 0L),
                                        porcentaje(cantidades.getOrDefault(estado, 0L), total),
                                        claseCss(estado)))
                               .toList();
    }

    private List<ReporteDatos.ProductoSolicitado> productosSolicitados(List<Pedido> pedidosActivos) {
        Map<String, AcumuladoProducto> importes = new HashMap<>();
        pedidosActivos.stream()
                      .flatMap(pedido -> pedido.getItems().stream())
                      .forEach(item -> importes.merge(item.productoId(),
                        new AcumuladoProducto(item.nombreProducto(), item.cantidad(), item.totalLinea()),
                        AcumuladoProducto::combinar));

        long totalUnidades = importes.values().stream()
                                     .mapToLong(AcumuladoProducto::unidades)
                                     .sum();

        return importes.entrySet().stream()
                                  .map(entrada -> new ReporteDatos.ProductoSolicitado(
                                          entrada.getKey(),
                                          entrada.getValue().nombre(),
                                          entrada.getValue().unidades(),
                                          entrada.getValue().importe(),
                                          porcentaje(entrada.getValue().unidades(), totalUnidades)))
                                  .sorted(Comparator.comparingLong(ReporteDatos.ProductoSolicitado::unidades).reversed()
                                          .thenComparing(ReporteDatos.ProductoSolicitado::nombre, String.                             CASE_INSENSITIVE_ORDER))
                                  .toList();
    }

    private ReporteDatos.PedidoFila filaPedido(Pedido pedido) {
        long unidades = pedido.getItems().stream().mapToLong(ItemPedido::cantidad).sum();
        return new ReporteDatos.PedidoFila(
                pedido.getId(),
                pedido.getCreadoEn(),
                pedido.getNombreCliente(),
                pedido.getEmail(),
                pedido.getEstado(),
                etiqueta(pedido.getEstado()),
                claseCss(pedido.getEstado()),
                unidades,
                pedido.getTotal());
    }

    private long contar(List<Pedido> pedidosFiltrados, EstadoPedido estado) {
        return pedidosFiltrados.stream().filter(pedido -> pedido.getEstado() == estado).count();
    }

    private int porcentaje(BigDecimal valor, BigDecimal maximo) {
        if (maximo.signum() == 0 || valor.signum() == 0) {
            return 0;
        }
        return Math.max(5, valor.multiply(BigDecimal.valueOf(100))
                                  .divide(maximo, 0, RoundingMode.HALF_UP).intValue());
    }

    private int porcentaje(long valor, long total) {
        return total == 0 ? 0 : (int) Math.round(valor * 100.0 / total);
    }

    private String etiqueta(YearMonth mes) {
        String nombreMes = mes.getMonth().getDisplayName(TextStyle.SHORT, IDIOMA);
        return nombreMes.substring(0, 1).toUpperCase(IDIOMA) + nombreMes.substring(1) + " " + mes.getYear();
    }

    private String etiqueta(EstadoPedido estado) {
        return switch (estado) {
            case EN_PROCESO -> "En proceso";
            case PENDIENTE -> "Pendiente";
            case ENVIADO -> "Enviado";
            case ENTREGADO -> "Entregado";
            case CANCELADO -> "Cancelado";
        };
    }

    private String claseCss(EstadoPedido estado) {
        return switch (estado) {
            case EN_PROCESO -> "proceso";
            case PENDIENTE -> "pendiente";
            case ENVIADO -> "enviado";
            case ENTREGADO -> "entregado";
            case CANCELADO -> "cancelado";
        };
    }

    private record AcumuladoMes(long cantidad, BigDecimal importe) { }

    private record AcumuladoProducto(String nombre, long unidades, BigDecimal importe) {
        private AcumuladoProducto combinar(AcumuladoProducto otro) {
            return new AcumuladoProducto(nombre, unidades + otro.unidades, importe.add(otro.importe));
        }
    }
}
