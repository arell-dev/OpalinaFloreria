package com.floral.opalina.service;

import com.floral.opalina.dto.CheckoutForm;
import com.floral.opalina.dto.PedidoApiForm;
import com.floral.opalina.model.CambioEstado;
import com.floral.opalina.model.Cliente;
import com.floral.opalina.model.Entrega;
import com.floral.opalina.model.ItemPedido;
import com.floral.opalina.model.Pedido;
import com.floral.opalina.model.Producto;
import com.floral.opalina.model.enums.EstadoPago;
import com.floral.opalina.model.enums.EstadoPedido;
import com.floral.opalina.model.enums.ModalidadEntrega;
import com.floral.opalina.repository.PedidoRepository;
import com.floral.opalina.session.UsuarioSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ProductoService productos;
    private final CarritoService carrito;
    private final UsuarioSession usuario;
    private final ClienteService clientes;

    public PedidoService(PedidoRepository pedidoRepository, ProductoService productoService, CarritoService carritoService, UsuarioSession usuarioSession, ClienteService clienteService) {
        pedidos = pedidoRepository;
        productos = productoService;
        carrito = carritoService;
        usuario = usuarioSession;
        clientes = clienteService;
    }

    public List<Pedido> listar() {
        return pedidos.buscarTodosOrdenados();
    }

    public List<Pedido> listar(EstadoPedido estado) {
        return pedidos.buscarPorEstado(estado);
    }

    public List<Pedido> buscarParaAdministracion(EstadoPedido estado, String busqueda) {
        return pedidos.buscarParaAdministracion(estado, busqueda);
    }

    public List<Pedido> delCliente(String clienteId) {
        return delCliente(clienteId, null);
    }

    public List<Pedido> delCliente(String clienteId, EstadoPedido estado) {
        return pedidos.buscarPorCliente(clienteId, estado);
    }

    public long contar(EstadoPedido estado) {
        return pedidos.contar(estado);
    }

    public BigDecimal totalVentas(EstadoPedido estado) {
        return pedidos.totalVentas(estado);
    }

    public Optional<Pedido> porId(String id) {
        return pedidos.buscarPorId(id);
    }

    public synchronized Pedido crear(CheckoutForm formulario) {
        if (carrito.vacio()) {
            throw new IllegalArgumentException("Agrega al menos un producto antes de confirmar");
        }

        if (formulario.getModalidad() == ModalidadEntrega.DELIVERY && (formulario.getDireccion() == null || formulario.getDireccion().isBlank())) {
            throw new IllegalArgumentException("La dirección es obligatoria para delivery");
        }

        if (!usuario.autenticado()) {
            throw new IllegalArgumentException("Inicia sesión para continuar");
        }

        var itemsCarrito = carrito.items();
        List<ItemPedido> lineas = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (var itemCarrito : itemsCarrito) {
            Producto producto = productos.buscarPorId(itemCarrito.producto().getId())
                                         .orElseThrow(() -> new IllegalArgumentException("Un producto ya no está disponible"));

            if (!producto.isComprable() || itemCarrito.cantidad() > producto.getStock()) {
                throw new IllegalArgumentException("El stock disponible de " + producto.getNombre() + " es " + producto.getStock());
            }

            BigDecimal totalLinea = producto.getPrecio().multiply(BigDecimal.valueOf(itemCarrito.cantidad()));
            lineas.add(new ItemPedido(
                    producto.getId(),
                    producto.getNombre(),
                    producto.getCategoria(),
                    producto.getImagen(),
                    itemCarrito.cantidad(),
                    producto.getPrecio(),
                    totalLinea));
            subtotal = subtotal.add(totalLinea);
        }

        boolean recojo = formulario.getModalidad() == ModalidadEntrega.RECOJO;
        BigDecimal delivery = carrito.delivery(recojo);
        Cliente cliente = clientes.porId(usuario.getUsuario().id()).orElseThrow();

        Entrega entrega = new Entrega(
                formulario.getModalidad(),
                recojo ? "" : formulario.getDireccion(),
                recojo ? "" : formulario.getReferencia(),
                formulario.getFecha(),
                formulario.getHora(),
                formulario.getNotas(),
                formulario.getMetodoPago());

        LocalDateTime ahora = LocalDateTime.now();
        String id = pedidos.generarId();
        Pedido pedido = new Pedido(
                id,
                cliente.getId(),
                formulario.getNombreReceptor(),
                formulario.getEmail(),
                formulario.getTelefono(),
                lineas,
                entrega,
                subtotal,
                delivery,
                subtotal.add(delivery),
                EstadoPedido.PENDIENTE,
                EstadoPago.PENDIENTE,
                ahora,
                List.of(new CambioEstado(EstadoPedido.PENDIENTE, ahora)));

        Map<String, Integer> cantidades = new HashMap<>();
        for (var itemCarrito : itemsCarrito) {
            cantidades.put(itemCarrito.producto().getId(), itemCarrito.cantidad());
        }

        productos.descontarStock(cantidades);
        pedidos.guardar(pedido);
        carrito.vaciar();
        return pedido;
    }

    public synchronized Pedido crearDesdeApi(PedidoApiForm formulario) {
        Cliente cliente = clientes.porId(formulario.getClienteId())
                                  .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        if (formulario.getModalidad() == ModalidadEntrega.DELIVERY && (formulario.getDireccion() == null || formulario.getDireccion().isBlank())) {
            throw new IllegalArgumentException("La dirección es obligatoria para delivery");
        }

        List<ItemPedido> lineas = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (PedidoApiForm.Linea linea : formulario.getItems()) {
            Producto producto = productos.buscarPorId(linea.getProductoId())
                                         .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + linea.getProductoId()));

            if (!producto.isComprable() || linea.getCantidad() > producto.getStock()) {
                throw new IllegalArgumentException("El stock disponible de " + producto.getNombre() + " es " + producto.getStock());
            }

            BigDecimal totalLinea = producto.getPrecio().multiply(BigDecimal.valueOf(linea.getCantidad()));
            lineas.add(new ItemPedido(
                    producto.getId(),
                    producto.getNombre(),
                    producto.getCategoria(),
                    producto.getImagen(),
                    linea.getCantidad(),
                    producto.getPrecio(),
                    totalLinea));
            subtotal = subtotal.add(totalLinea);
        }

        boolean recojo = formulario.getModalidad() == ModalidadEntrega.RECOJO;
        BigDecimal delivery = carrito.calcularDelivery(subtotal, recojo);
        Entrega entrega = new Entrega(
                formulario.getModalidad(),
                recojo ? "" : formulario.getDireccion(),
                recojo ? "" : formulario.getReferencia(),
                formulario.getFecha(),
                formulario.getHora(),
                "",
                formulario.getMetodoPago());

        LocalDateTime ahora = LocalDateTime.now();
        String id = pedidos.generarId();
        Pedido pedido = new Pedido(
                id,
                cliente.getId(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getTelefono(),
                lineas,
                entrega,
                subtotal,
                delivery,
                subtotal.add(delivery),
                EstadoPedido.PENDIENTE,
                EstadoPago.PENDIENTE,
                ahora,
                List.of(new CambioEstado(EstadoPedido.PENDIENTE, ahora)));

        Map<String, Integer> cantidades = new HashMap<>();
        for (PedidoApiForm.Linea linea : formulario.getItems()) {
            cantidades.merge(linea.getProductoId(), linea.getCantidad(), Integer::sum);
        }

        productos.descontarStock(cantidades);
        pedidos.guardar(pedido);
        return pedido;
    }

    public synchronized void cambiarEstado(String id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidos.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        EstadoPedido estadoActual = pedido.getEstado();

        boolean transicionPermitida = switch (estadoActual) {
            case PENDIENTE -> nuevoEstado == EstadoPedido.EN_PROCESO || nuevoEstado == EstadoPedido.CANCELADO;
            case EN_PROCESO -> nuevoEstado == EstadoPedido.ENVIADO || nuevoEstado == EstadoPedido.CANCELADO;
            case ENVIADO -> nuevoEstado == EstadoPedido.ENTREGADO;
            default -> false;
        };

        if (!transicionPermitida) {
            throw new IllegalArgumentException("No se puede cambiar de " + estadoActual + " a " + nuevoEstado);
        }

        pedido.cambiarEstado(nuevoEstado);
        pedidos.guardar(pedido);
    }
}
