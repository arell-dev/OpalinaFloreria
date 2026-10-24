package com.floral.opalina.controller;

import com.floral.opalina.domain.enums.EstadoPedido;
import com.floral.opalina.dto.ReporteDatos;
import com.floral.opalina.export.ReportePdfExporter;
import com.floral.opalina.service.ReporteService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/reportes")
public class AdminReporteController {
    private static final int TAMANO_PAGINA = 10;

    private final ReporteService reportes;
    private final ReportePdfExporter exportadorPdf;

    public AdminReporteController(ReporteService reportes, ReportePdfExporter exportadorPdf) {
        this.reportes = reportes;
        this.exportadorPdf = exportadorPdf;
    }

    @GetMapping
    public String mostrar(@RequestParam(required = false) EstadoPedido estado, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, @RequestParam(defaultValue = "0") int pagina, Model model) {
        boolean rangoInvalido = rangoInvalido(desde, hasta);
        ReporteDatos reporte = rangoInvalido ? ReporteDatos.vacio() : reportes.generar(estado, desde, hasta);
        cargarVista(model, reporte, estado, desde, hasta, pagina, rangoInvalido);
        return "admin/reportes";
    }

    @GetMapping(value = "/exportar.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) EstadoPedido estado, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (rangoInvalido(desde, hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final.");
        }

        byte[] archivo = exportadorPdf.exportar(reportes.generar(estado, desde, hasta), desde, hasta, estado);
        String nombreArchivo = "reporte-opalina-" + LocalDate.now() + ".pdf";
        String disposicion = ContentDisposition.attachment()
                                               .filename(nombreArchivo, StandardCharsets.UTF_8)
                                               .build()
                                               .toString();

        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_PDF)
                             .header(HttpHeaders.CONTENT_DISPOSITION, disposicion)
                             .body(archivo);
    }

    private void cargarVista(Model model, ReporteDatos reporte, EstadoPedido estado, LocalDate desde, LocalDate hasta, int pagina, boolean rangoInvalido) {
        int totalPedidos = reporte.pedidos().size();
        int totalPaginas = (int) Math.ceil(totalPedidos / (double) TAMANO_PAGINA);
        int paginaActual = totalPaginas == 0 ? 0 : Math.max(0, Math.min(pagina, totalPaginas - 1));
        int inicio = paginaActual * TAMANO_PAGINA;
        int fin = Math.min(inicio + TAMANO_PAGINA, totalPedidos);
        List<ReporteDatos.PedidoFila> pedidosPagina = reporte.pedidos().subList(inicio, fin);

        model.addAttribute("reporte", reporte);
        model.addAttribute("pedidosPagina", pedidosPagina);
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("estados", EstadoPedido.values());
        model.addAttribute("graficoEstados", gradienteEstados(reporte.pedidosPorEstado()));
        model.addAttribute("rangoInvalido", rangoInvalido);
    }

    private String gradienteEstados(List<ReporteDatos.EstadoCantidad> estados) {
        long total = estados.stream().mapToLong(ReporteDatos.EstadoCantidad::cantidad).sum();
        if (total == 0) {
            return "conic-gradient(#f0edf3 0deg 360deg)";
        }

        StringBuilder gradiente = new StringBuilder("conic-gradient(");
        double inicio = 0;
        long procesados = 0;
        boolean primero = true;
        for (ReporteDatos.EstadoCantidad estado : estados) {
            if (estado.cantidad() == 0) {
                continue;
            }

            procesados += estado.cantidad();
            double fin = procesados == total ? 360 : inicio + estado.cantidad() * 360.0 / total;
            if (!primero) {
                gradiente.append(", ");
            }
            gradiente.append(colorEstado(estado.estado()))
                     .append(' ').append(inicio).append("deg ")
                     .append(fin).append("deg");
            inicio = fin;
            primero = false;
        }
        return gradiente.append(')').toString();
    }

    private String colorEstado(EstadoPedido estado) {
        return switch (estado) {
            case PENDIENTE -> "var(--reporte-pendiente)";
            case EN_PROCESO -> "var(--reporte-proceso)";
            case ENVIADO -> "var(--reporte-enviado)";
            case ENTREGADO -> "var(--reporte-entregado)";
            case CANCELADO -> "var(--reporte-cancelado)";
        };
    }

    private boolean rangoInvalido(LocalDate desde, LocalDate hasta) {
        return desde != null && hasta != null && desde.isAfter(hasta);
    }
}
