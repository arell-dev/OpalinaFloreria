package com.floral.opalina.config;

import com.floral.opalina.model.CambioEstado;
import com.floral.opalina.model.Cliente;
import com.floral.opalina.model.Credencial;
import com.floral.opalina.model.Entrega;
import com.floral.opalina.model.ItemPedido;
import com.floral.opalina.model.Pedido;
import com.floral.opalina.model.Producto;
import com.floral.opalina.model.enums.EstadoPago;
import com.floral.opalina.model.enums.EstadoPedido;
import com.floral.opalina.model.enums.MetodoPago;
import com.floral.opalina.model.enums.ModalidadEntrega;
import com.floral.opalina.model.enums.Rol;
import com.floral.opalina.repository.ClienteRepository;
import com.floral.opalina.repository.PedidoRepository;
import com.floral.opalina.repository.ProductoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("memory")
public class DataInitializer implements CommandLineRunner {

    private final ProductoRepository productos;
    private final ClienteRepository clientes;
    private final PedidoRepository pedidos;

    public DataInitializer(ProductoRepository productos, ClienteRepository clientes, PedidoRepository pedidos) {
        this.productos = productos;
        this.clientes = clientes;
        this.pedidos = pedidos;
    }

    @Override
    public void run(String... args) {
        List.of(
                productoInicial(
                        "8-MiniRamoCotidiano",
                        "Mini Ramo Cotidiano",
                        "1-3 tallos con follaje y tarjeta corta para detalles de oficina.",
                        "Temporada",
                        "22",
                        18,
                        false,
                        "OPA-001",
                        "8-MiniRamoCotidiano.png"),
                productoInicial(
                        "12-RamoOpalinaMorado",
                        "Ramo Opalina Morado",
                        "Gerberas, hortensias y margaritas en paleta lila y morado.",
                        "Romántico",
                        "55",
                        28,
                        true,
                        "OPA-002",
                        "12-RamoOpalinaMorado.png"),
                productoInicial(
                        "9-RamoAmarillo21Marzo",
                        "Ramo Amarillo 21 de Marzo",
                        "Girasoles y flores amarillas de tendencia con lazo amarillo.",
                        "Temporada",
                        "65",
                        12,
                        true,
                        "OPA-003",
                        "9-RamoAmarillo21Marzo.png"),
                productoInicial(
                        "3-Buchon12Rosas",
                        "Buchón 12 Rosas",
                        "Bouquet grande con 12 rosas en papel coreano rojo y negro.",
                        "Romántico",
                        "85",
                        7,
                        true,
                        "OPA-004",
                        "3-Buchon12Rosas.jpg"),
                productoInicial(
                        "2-BoxFloralConChocolates",
                        "Box Floral con Chocolates",
                        "Ramo corto en box cilíndrica con rosas y chocolates.",
                        "Cumpleaños",
                        "95",
                        9,
                        false,
                        "OPA-005",
                        "2-BoxFloralConChocolates.jpg"),
                productoInicial(
                        "11-RamoLiriosPremium",
                        "Ramo de Lirios Premium",
                        "Lirios claros con flores de apoyo y follaje fresco.",
                        "Premium",
                        "110",
                        4,
                        false,
                        "OPA-006",
                        "11-RamoLiriosPremium.png"),
                productoInicial(
                        "6-JardinOpalinaGrande",
                        "Jardín de Opalina Grande",
                        "Bouquet firma grande, multivarietal con tarjeta impresa.",
                        "Premium",
                        "150",
                        3,
                        false,
                        "OPA-007",
                        "6-JardinOpalinaGrande.png"),
                productoInicial(
                        "10-RamoGraduacion",
                        "Ramo de Graduación",
                        "Rosas o gerberas con lazo y mini topper personalizable.",
                        "Cumpleaños",
                        "78",
                        15,
                        false,
                        "OPA-008",
                        "10-RamoGraduación.png"),
                productoInicial(
                        "7-LagrimaCondolencias",
                        "Lágrima Condolencias",
                        "Arreglo sobrio de condolencia con faja.",
                        "Condolencias",
                        "120",
                        6,
                        false,
                        "OPA-009",
                        "7-LagrimaCondolencias.png"),
                productoInicial(
                        "4-CoronaPremium",
                        "Corona Premium",
                        "Corona ceremonial con flores mixtas y mensaje.",
                        "Condolencias",
                        "280",
                        0,
                        false,
                        "OPA-010",
                        "4-CoronaPremium.jpg"),
                productoInicial(
                        "5-DuoBobaFlor",
                        "Dúo Boba + Flor",
                        "Detalle colaborativo con Boba Pop, flores y tarjeta.",
                        "Temporada",
                        "32",
                        22,
                        false,
                        "OPA-011",
                        "5-DuoBobaFlor.png"),
                productoInicial(
                        "1-arregloPersonalizado",
                        "Arreglo Personalizado",
                        "Diseño según ocasión, colores preferidos y presupuesto.",
                        "Personalizado",
                        "120",
                        11,
                        false,
                        "OPA-012",
                        "1-arregloPersonalizado.jpg"))
                .forEach(productos::guardar);

        Cliente maria = new Cliente(
                "cliente-maria",
                "María López",
                "maria.lopez@opalina.test",
                "+51 999 888 777",
                "Av. Grau 123, Urb. San Eduardo, Piura",
                "Casa de 2 pisos, portón gris",
                fecha("2024-01-01"));
        Cliente carlos = new Cliente(
                "cliente-carlos",
                "Carlos Ramírez",
                "carlos.ramirez@opalina.test",
                "+51 987 654 321",
                "",
                "",
                fecha("2025-01-01"));
        Cliente ana = new Cliente(
                "cliente-ana",
                "Ana Torres",
                "ana.torres@opalina.test",
                "+51 912 345 678",
                "",
                "",
                fecha("2025-02-01"));
        Cliente luis = new Cliente(
                "cliente-luis",
                "Luis Fernández",
                "luis.fernandez@opalina.test",
                "+51 955 667 788",
                "",
                "",
                fecha("2025-03-01"));
        Cliente admin = new Cliente(
                "admin-opalina",
                "Administración Opalina",
                "admin@opalina.test",
                "",
                "",
                "",
                LocalDateTime.now());

        List.of(maria, carlos, ana, luis, admin).forEach(clientes::guardar);
        
        clientes.guardarCredencial(new Credencial(maria.getId(), maria.getEmail(), "Cliente2026!", Rol.CLIENTE));
        clientes.guardarCredencial(new Credencial(admin.getId(), admin.getEmail(), "Admin2026!", Rol.ADMIN));

        pedidos.guardar(pedido(
                "OP-2026-0052",
                maria,
                EstadoPedido.PENDIENTE,
                EstadoPago.PENDIENTE,
                fechaHora("2026-08-28T12:00:00"),
                entrega(
                        ModalidadEntrega.DELIVERY,
                        "Av. Grau 123, Urb. San Eduardo, Piura",
                        "Casa de 2 pisos, portón gris",
                        "2026-08-29",
                        "10:00",
                        "Por favor llamar al llegar."),
                "0.00",
                List.of(
                        item("6-JardinOpalinaGrande", 1),
                        item("12-RamoOpalinaMorado", 2)),
                List.of(cambio(EstadoPedido.PENDIENTE, "2026-08-28T12:00:00"))));

        pedidos.guardar(pedido(
                "OP-2026-0049",
                carlos,
                EstadoPedido.PENDIENTE,
                EstadoPago.PENDIENTE,
                fechaHora("2026-08-21T12:00:00"),
                entrega(ModalidadEntrega.DELIVERY, "Piura", "", "2026-08-22", "15:00", ""),
                "0.00",
                List.of(
                        item("3-Buchon12Rosas", 1),
                        item("2-BoxFloralConChocolates", 1)),
                List.of(cambio(EstadoPedido.PENDIENTE, "2026-08-21T12:00:00"))));

        pedidos.guardar(pedido(
                "OP-2026-0048",
                maria,
                EstadoPedido.EN_PROCESO,
                EstadoPago.PAGADO,
                fechaHora("2026-08-14T12:00:00"),
                entrega(
                        ModalidadEntrega.DELIVERY,
                        "Av. Grau 123, Urb. San Eduardo, Piura",
                        "",
                        "2026-08-15",
                        "12:00",
                        ""),
                "0.00",
                List.of(item("6-JardinOpalinaGrande", 1)),
                List.of(
                        cambio(EstadoPedido.PENDIENTE, "2026-08-14T12:00:00"),
                        cambio(EstadoPedido.EN_PROCESO, "2026-08-15T09:00:00"))));

        pedidos.guardar(pedido(
                "OP-2026-0045",
                ana,
                EstadoPedido.EN_PROCESO,
                EstadoPago.PAGADO,
                fechaHora("2026-08-09T12:00:00"),
                entrega(ModalidadEntrega.RECOJO, "", "", "2026-08-10", "11:00", ""),
                "0.00",
                List.of(item("4-CoronaPremium", 1)),
                List.of(
                        cambio(EstadoPedido.PENDIENTE, "2026-08-09T12:00:00"),
                        cambio(EstadoPedido.EN_PROCESO, "2026-08-10T08:00:00"))));

        pedidos.guardar(pedido(
                "OP-2026-0041",
                luis,
                EstadoPedido.ENTREGADO,
                EstadoPago.PAGADO,
                fechaHora("2026-08-01T12:00:00"),
                entrega(ModalidadEntrega.DELIVERY, "Piura", "", "2026-08-02", "09:00", ""),
                "15.00",
                List.of(
                        item("9-RamoAmarillo21Marzo", 2),
                        item("5-DuoBobaFlor", 1)),
                List.of(
                        cambio(EstadoPedido.PENDIENTE, "2026-08-01T12:00:00"),
                        cambio(EstadoPedido.EN_PROCESO, "2026-08-01T15:00:00"),
                        cambio(EstadoPedido.ENVIADO, "2026-08-02T07:00:00"),
                        cambio(EstadoPedido.ENTREGADO, "2026-08-02T09:30:00"))));
    }

    private Pedido pedido(
            String id,
            Cliente cliente,
            EstadoPedido estado,
            EstadoPago estadoPago,
            LocalDateTime creadoEn,
            Entrega entrega,
            String costoDelivery,
            List<ItemPedido> items,
            List<CambioEstado> historial) {

        BigDecimal subtotal = items.stream()
                                   .map(ItemPedido::totalLinea)
                                   .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal delivery = new BigDecimal(costoDelivery);

        return new Pedido(
                id,
                cliente.getId(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getTelefono(),
                items,
                entrega,
                subtotal,
                delivery,
                subtotal.add(delivery),
                estado,
                estadoPago,
                creadoEn,
                historial);
    }

    private ItemPedido item(String productoId, int cantidad) {
        Producto producto = productos.buscarPorId(productoId).orElseThrow();
        BigDecimal totalLinea = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));

        return new ItemPedido(
                producto.getId(),
                producto.getNombre(),
                producto.getCategoria(),
                producto.getImagen(),
                cantidad,
                producto.getPrecio(),
                totalLinea);
    }

    private Entrega entrega(
            ModalidadEntrega modalidad,
            String direccion,
            String referencia,
            String fecha,
            String hora,
            String notas) {
        return new Entrega(
                modalidad,
                direccion,
                referencia,
                LocalDate.parse(fecha),
                LocalTime.parse(hora),
                notas,
                MetodoPago.YAPE);
    }

    private CambioEstado cambio(EstadoPedido estado, String fecha) {
        return new CambioEstado(estado, fechaHora(fecha));
    }

    private LocalDateTime fecha(String fecha) {
        return LocalDate.parse(fecha).atStartOfDay();
    }

    private LocalDateTime fechaHora(String fechaHora) {
        return LocalDateTime.parse(fechaHora);
    }

    private Producto productoInicial(
            String id,
            String nombre,
            String descripcion,
            String categoria,
            String precio,
            int stock,
            boolean destacado,
            String sku,
            String archivo) {
        String rutaImagen = "/assets/imagenes/productos/" + archivo;

        return new Producto(
                id,
                nombre,
                descripcion,
                categoria,
                new BigDecimal(precio),
                stock,
                true,
                destacado,
                sku,
                rutaImagen);
    }
}
