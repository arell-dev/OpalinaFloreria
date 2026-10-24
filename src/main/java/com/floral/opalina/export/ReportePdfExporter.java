package com.floral.opalina.export;

import com.floral.opalina.domain.enums.EstadoPedido;
import com.floral.opalina.dto.ReporteDatos;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

@Component
public class ReportePdfExporter {
    private static final Locale IDIOMA = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", IDIOMA);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", IDIOMA);

    public byte[] exportar(ReporteDatos reporte, LocalDate desde, LocalDate hasta, EstadoPedido estado) {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PdfReport pdf = new PdfReport(documento);
            dibujarEncabezado(pdf, desde, hasta, estado);
            dibujarResumen(pdf, reporte.resumen());
            dibujarEstados(pdf, reporte.pedidosPorEstado());
            dibujarVentasMensuales(pdf, reporte.ventasPorMes());
            dibujarTablaProductos(pdf, reporte.productosSolicitados());
            dibujarTablaPedidos(pdf, reporte.pedidos());
            pdf.cerrarPagina();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo generar el reporte PDF.", error);
        }
    }

    private void dibujarEncabezado(PdfReport pdf, LocalDate desde, LocalDate hasta, EstadoPedido estado) throws IOException {
        pdf.texto("OPALINA  |  FLORERÍA PIURA", 42, pdf.y, PdfReport.NEGRITA, 9, PdfReport.CIRUELA);
        pdf.texto("Generado: " + LocalDateTime.now(ZoneId.of("America/Lima")).format(FECHA_HORA),
                348, pdf.y, PdfReport.NORMAL, 8, PdfReport.GRIS);
        pdf.y -= 25;
        pdf.texto("Reporte de pedidos", 42, pdf.y, PdfReport.NEGRITA, 21, PdfReport.TEXTO);
        pdf.y -= 18;
        pdf.texto("Periodo: " + rango(desde, hasta) + "    Estado: " + etiqueta(estado),
                42, pdf.y, PdfReport.NORMAL, 9, PdfReport.GRIS);
        pdf.y -= 13;
        pdf.linea(42, pdf.y, PDRectangle.A4.getWidth() - 42, pdf.y, PdfReport.BORDE);
        pdf.y -= 22;
    }

    private void dibujarResumen(PdfReport pdf, ReporteDatos.Resumen resumen) throws IOException {
        float anchoTarjeta = (pdf.anchoContenido() - 24) / 4;
        List<String> etiquetas = List.of("Pedidos", "Pedidos pendientes", "Valor sin cancelar", "Unidades solicitadas");
        List<String> valores = List.of(
                String.valueOf(resumen.totalPedidos()),
                String.valueOf(resumen.pedidosPendientes()),
                moneda(resumen.valorPedidos()),
                String.valueOf(resumen.unidadesSolicitadas()));

        for (int indice = 0; indice < etiquetas.size(); indice++) {
            float x = 42 + indice * (anchoTarjeta + 8);
            pdf.tarjeta(x, pdf.y - 58, anchoTarjeta, 58);
            pdf.texto(etiquetas.get(indice), x + 10, pdf.y - 19, PdfReport.NORMAL, 7.5f, PdfReport.GRIS);
            pdf.textoAjustado(valores.get(indice), x + 10, pdf.y - 42, anchoTarjeta - 20, PdfReport.NEGRITA, 11, PdfReport.TEXTO);
        }
        pdf.y -= 76;
        pdf.texto("Promedio por pedido no cancelado: " + moneda(resumen.promedioPedido()),
                42, pdf.y, PdfReport.NEGRITA, 9, PdfReport.CIRUELA);
        pdf.y -= 15;
        pdf.texto("El resumen y las gráficas de importes excluyen pedidos cancelados.",
                42, pdf.y, PdfReport.NORMAL, 7.5f, PdfReport.GRIS);
        pdf.y -= 11;
        pdf.texto("Los totales de la tabla conservan el importe registrado; no confirman el pago.",
                42, pdf.y, PdfReport.NORMAL, 7.5f, PdfReport.GRIS);
        pdf.y -= 20;
    }

    private void dibujarEstados(PdfReport pdf, List<ReporteDatos.EstadoCantidad> estados) throws IOException {
        pdf.tituloSeccion("Pedidos por estado");
        long total = estados.stream().mapToLong(ReporteDatos.EstadoCantidad::cantidad).sum();
        if (total == 0) {
            pdf.texto("No hay pedidos que coincidan con los filtros.", 42, pdf.y, PdfReport.NORMAL, 9, PdfReport.GRIS);
            pdf.y -= 22;
            return;
        }

        float centroX = 120;
        float centroY = pdf.y - 66;
        float radio = 50;
        pdf.dona(centroX, centroY, radio, 27, estados, total);

        float xIndicador = 220;
        float yLeyenda = pdf.y - 10;
        for (ReporteDatos.EstadoCantidad estado : estados) {
            Color color = colorEstado(estado.estado());
            pdf.muestraColor(xIndicador, yLeyenda - 3, 8, 8, color);
            pdf.texto(estado.etiqueta(), xIndicador + 15, yLeyenda, PdfReport.NORMAL, 8.5f, PdfReport.TEXTO);
            pdf.texto(estado.cantidad() + " pedidos (" + estado.porcentaje() + "%)",
                    xIndicador + 105, yLeyenda, PdfReport.NEGRITA, 8, PdfReport.TEXTO);
            yLeyenda -= 17;
        }

        pdf.y = Math.min(centroY - radio - 12, yLeyenda - 4);
        pdf.y -= 8;
    }

    private void dibujarVentasMensuales(PdfReport pdf, List<ReporteDatos.VentasMes> ventas) throws IOException {
        pdf.tituloSeccion("Importe de pedidos por mes");
        if (ventas.isEmpty()) {
            pdf.texto("No hay importes para graficar en el periodo seleccionado.", 42, pdf.y, PdfReport.NORMAL, 9, PdfReport.GRIS);
            pdf.y -= 20;
            return;
        }

        List<ReporteDatos.VentasMes> ultimosMeses = ventas.subList(Math.max(0, ventas.size() - 12), ventas.size());
        float anchoSlot = pdf.anchoContenido() / ultimosMeses.size();
        float base = pdf.y - 82;
        BigDecimal maximo = ultimosMeses.stream()
                .map(ReporteDatos.VentasMes::importe)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        for (int indice = 0; indice < ultimosMeses.size(); indice++) {
            ReporteDatos.VentasMes mes = ultimosMeses.get(indice);
            float x = 42 + indice * anchoSlot;
            float altura = maximo.signum() == 0 ? 0 : mes.importe()
                    .multiply(BigDecimal.valueOf(64))
                    .divide(maximo, 2, RoundingMode.HALF_UP)
                    .floatValue();
            pdf.barraVertical(x + anchoSlot * 0.3f, base, anchoSlot * 0.4f, Math.max(2, altura), PdfReport.CIRUELA);
            pdf.textoAjustado(mes.etiqueta(), x, base - 13, anchoSlot, PdfReport.NORMAL, 6.2f, PdfReport.GRIS);
        }
        pdf.y = base - 30;
        if (ventas.size() > 12) {
            pdf.texto("El gráfico muestra los últimos 12 meses; la tabla incluye todos los meses filtrados.", 42, pdf.y, PdfReport.NORMAL, 7.5f, PdfReport.GRIS);
            pdf.y -= 16;
        }

        List<List<String>> filas = ventas.stream()
                                         .map(mes -> List.of(mes.etiqueta(), String.valueOf(mes.cantidad()), moneda(mes.importe())))
                                         .toList();
        pdf.tabla(List.of("Mes", "Pedidos", "Importe"), new float[] {230, 100, 181}, filas);
        pdf.y -= 16;
    }

    private void dibujarTablaProductos(PdfReport pdf, List<ReporteDatos.ProductoSolicitado> productos) throws IOException {
        pdf.tituloSeccion("Productos más solicitados");
        List<List<String>> filas = productos.stream()
                                            .map(producto -> List.of(producto.nombre(), String.valueOf(producto.unidades()), moneda(producto.importe())))
                                            .toList();
        if (filas.isEmpty()) {
            pdf.texto("No hay productos solicitados para los filtros elegidos.", 42, pdf.y, PdfReport.NORMAL, 9, PdfReport.GRIS);
            pdf.y -= 20;
            return;
        }
        pdf.tabla(List.of("Producto", "Unidades", "Importe"), new float[] {300, 95, 116}, filas);
        pdf.y -= 16;
    }

    private void dibujarTablaPedidos(PdfReport pdf, List<ReporteDatos.PedidoFila> pedidos) throws IOException {
        pdf.tituloSeccion("Pedidos incluidos");
        List<List<String>> filas = pedidos.stream()
                .map(pedido -> List.of(
                        pedido.codigo(),
                        pedido.fecha().format(FECHA_HORA),
                        pedido.cliente(),
                        pedido.estadoEtiqueta(),
                        String.valueOf(pedido.unidades()),
                        moneda(pedido.importe())))
                .toList();
        if (filas.isEmpty()) {
            pdf.texto("No hay pedidos para mostrar.", 42, pdf.y, PdfReport.NORMAL, 9, PdfReport.GRIS);
            return;
        }
        pdf.tabla(List.of("Código", "Fecha", "Cliente", "Estado", "Unid.", "Importe"),
                new float[] {72, 88, 136, 77, 48, 90}, filas);
    }

    private String rango(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return "Todo el historial";
        }
        String inicio = desde == null ? "Sin fecha inicial" : desde.format(FECHA);
        String fin = hasta == null ? "Sin fecha final" : hasta.format(FECHA);
        return inicio + " - " + fin;
    }

    private String etiqueta(EstadoPedido estado) {
        if (estado == null) {
            return "Todos los estados";
        }
        return switch (estado) {
            case EN_PROCESO -> "En proceso";
            case PENDIENTE -> "Pendiente";
            case ENVIADO -> "Enviado";
            case ENTREGADO -> "Entregado";
            case CANCELADO -> "Cancelado";
        };
    }

    private static Color colorEstado(EstadoPedido estado) {
        return switch (estado) {
            case PENDIENTE -> new Color(190, 132, 28);
            case EN_PROCESO -> new Color(103, 77, 119);
            case ENVIADO -> new Color(72, 116, 166);
            case ENTREGADO -> new Color(83, 107, 79);
            case CANCELADO -> new Color(171, 75, 83);
        };
    }

    private String moneda(BigDecimal importe) {
        NumberFormat formato = NumberFormat.getNumberInstance(IDIOMA);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return "S/ " + formato.format(importe);
    }

    private static class PdfReport {
        private static final float MARGEN = 42;
        private static final PDFont NORMAL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private static final PDFont NEGRITA = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private static final Color CIRUELA = new Color(98, 71, 108);
        private static final Color TEXTO = new Color(41, 44, 53);
        private static final Color GRIS = new Color(101, 98, 108);
        private static final Color BORDE = new Color(227, 229, 235);
        private static final Color FONDO = new Color(248, 247, 250);
        private static final Color BLANCO = Color.WHITE;

        private final PDDocument documento;
        private PDPage pagina;
        private PDPageContentStream contenido;
        private float y;
        private int numeroPagina;

        private PdfReport(PDDocument documento) throws IOException {
            this.documento = documento;
            nuevaPagina();
        }

        private float anchoContenido() {
            return PDRectangle.A4.getWidth() - MARGEN * 2;
        }

        private void nuevaPagina() throws IOException {
            cerrarPagina();
            pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            contenido = new PDPageContentStream(documento, pagina);
            numeroPagina++;
            float alto = pagina.getMediaBox().getHeight();
            texto("OPALINA  |  REPORTE DE PEDIDOS", MARGEN, alto - 30, NEGRITA, 8, CIRUELA);
            texto(numeroPagina == 1 ? "Administración" : "Continuación", 470, alto - 30, NORMAL, 8, GRIS);
            linea(MARGEN, alto - 39, altoContenidoDerecha(), alto - 39, BORDE);
            y = alto - 57;
        }

        private void cerrarPagina() throws IOException {
            if (contenido == null) {
                return;
            }
            linea(MARGEN, 30, altoContenidoDerecha(), 30, BORDE);
            texto("Opalina Florería Piura", MARGEN, 18, NORMAL, 7, GRIS);
            texto("Página " + numeroPagina, 500, 18, NORMAL, 7, GRIS);
            contenido.close();
            contenido = null;
        }

        private float altoContenidoDerecha() {
            return pagina.getMediaBox().getWidth() - MARGEN;
        }

        private void tituloSeccion(String titulo) throws IOException {
            if (y < 72) {
                nuevaPagina();
            }
            texto(titulo, MARGEN, y, NEGRITA, 11, TEXTO);
            y -= 18;
        }

        private void tarjeta(float x, float y, float ancho, float alto) throws IOException {
            contenido.setNonStrokingColor(FONDO);
            contenido.setStrokingColor(BORDE);
            contenido.addRect(x, y, ancho, alto);
            contenido.fillAndStroke();
        }

        private void dona(
                float centroX,
                float centroY,
                float radioExterior,
                float radioInterior,
                List<ReporteDatos.EstadoCantidad> estados,
                long total) throws IOException {
            double angulo = -90;
            for (ReporteDatos.EstadoCantidad estado : estados) {
                if (estado.cantidad() == 0) {
                    continue;
                }
                double barrido = estado.cantidad() * 360.0 / total;
                sector(centroX, centroY, radioExterior, angulo, barrido, colorEstado(estado.estado()));
                angulo += barrido;
            }

            circulo(centroX, centroY, radioInterior, BLANCO);
            textoCentrado(String.valueOf(total), centroX, centroY + 3,
                    NEGRITA, 13, TEXTO);
            textoCentrado("pedidos", centroX, centroY - 9,
                    NORMAL, 7.5f, GRIS);
        }

        private void sector(float centroX, float centroY, float radio, double inicio, double barrido, Color color) throws IOException {
            int segmentos = Math.max(1, (int) Math.ceil(barrido / 5));
            double paso = Math.toRadians(barrido / segmentos);
            double angulo = Math.toRadians(inicio);

            contenido.setNonStrokingColor(color);
            contenido.moveTo(centroX, centroY);
            contenido.lineTo(
                    (float) (centroX + Math.cos(angulo) * radio),
                    (float) (centroY + Math.sin(angulo) * radio));
            for (int indice = 1; indice <= segmentos; indice++) {
                double punto = angulo + paso * indice;
                contenido.lineTo(
                        (float) (centroX + Math.cos(punto) * radio),
                        (float) (centroY + Math.sin(punto) * radio));
            }
            contenido.closePath();
            contenido.fill();
        }

        private void circulo(float centroX, float centroY, float radio, Color color) throws IOException {
            float control = radio * 0.55228475f;
            contenido.setNonStrokingColor(color);
            contenido.moveTo(centroX, centroY + radio);
            contenido.curveTo(centroX + control, centroY + radio,
                    centroX + radio, centroY + control, centroX + radio, centroY);
            contenido.curveTo(centroX + radio, centroY - control,
                    centroX + control, centroY - radio, centroX, centroY - radio);
            contenido.curveTo(centroX - control, centroY - radio,
                    centroX - radio, centroY - control, centroX - radio, centroY);
            contenido.curveTo(centroX - radio, centroY + control,
                    centroX - control, centroY + radio, centroX, centroY + radio);
            contenido.closePath();
            contenido.fill();
        }

        private void muestraColor(float x, float y, float ancho, float alto, Color color) throws IOException {
            contenido.setNonStrokingColor(color);
            contenido.addRect(x, y, ancho, alto);
            contenido.fill();
        }

        private void textoCentrado(String valor, float centroX, float y, PDFont fuente, float tamano, Color color) throws IOException {
            String texto = compatible(valor);
            float ancho = fuente.getStringWidth(texto) / 1000 * tamano;
            texto(texto, centroX - ancho / 2, y, fuente, tamano, color);
        }

        private void barraVertical(float x, float y, float ancho, float alto, Color color) throws IOException {
            contenido.setNonStrokingColor(color);
            contenido.addRect(x, y, ancho, alto);
            contenido.fill();
        }

        private void linea(float x1, float y1, float x2, float y2, Color color) throws IOException {
            contenido.setStrokingColor(color);
            contenido.moveTo(x1, y1);
            contenido.lineTo(x2, y2);
            contenido.stroke();
        }

        private void texto(String texto, float x, float y, PDFont fuente, float tamano, Color color) throws IOException {
            contenido.beginText();
            contenido.setFont(fuente, tamano);
            contenido.setNonStrokingColor(color);
            contenido.newLineAtOffset(x, y);
            contenido.showText(compatible(texto));
            contenido.endText();
        }

        private void textoAjustado(String valor, float x, float y, float ancho, PDFont fuente, float tamano, Color color) throws IOException {
            String texto = compatible(valor);
            while (!texto.isEmpty() && fuente.getStringWidth(texto) / 1000 * tamano > ancho) {
                texto = texto.substring(0, texto.length() - 1);
            }
            if (!texto.equals(compatible(valor)) && texto.length() > 3) {
                texto = texto.substring(0, texto.length() - 3) + "...";
            }
            texto(texto, x, y, fuente, tamano, color);
        }

        private void tabla(List<String> titulos, float[] anchos, List<List<String>> filas) throws IOException {
            cabeceraTabla(titulos, anchos);
            for (int indice = 0; indice < filas.size(); indice++) {
                if (y - 21 < 46) {
                    nuevaPagina();
                    cabeceraTabla(titulos, anchos);
                }
                if (indice % 2 == 1) {
                    contenido.setNonStrokingColor(FONDO);
                    contenido.addRect(MARGEN, y - 16, anchoContenido(), 21);
                    contenido.fill();
                }
                float x = MARGEN + 7;
                for (int columna = 0; columna < titulos.size(); columna++) {
                    textoAjustado(filas.get(indice).get(columna), x, y - 8, anchos[columna] - 12, NORMAL, 7.5f, TEXTO);
                    x += anchos[columna];
                }
                linea(MARGEN, y - 16, altoContenidoDerecha(), y - 16, BORDE);
                y -= 21;
            }
            y -= 7;
        }

        private void cabeceraTabla(List<String> titulos, float[] anchos) throws IOException {
            if (y - 25 < 46) {
                nuevaPagina();
            }
            contenido.setNonStrokingColor(FONDO);
            contenido.addRect(MARGEN, y - 17, anchoContenido(), 23);
            contenido.fill();
            float x = MARGEN + 7;
            for (int columna = 0; columna < titulos.size(); columna++) {
                textoAjustado(titulos.get(columna), x, y - 9, anchos[columna] - 12, NEGRITA, 7.5f, CIRUELA);
                x += anchos[columna];
            }
            y -= 25;
        }

        private String compatible(String valor) {
            StringBuilder resultado = new StringBuilder(valor.length());
            valor.codePoints().forEach(caracter -> {
                if (caracter <= 255 && !Character.isISOControl(caracter)) {
                    resultado.appendCodePoint(caracter);
                } else {
                    resultado.append('?');
                }
            });
            return resultado.toString();
        }
    }
}
