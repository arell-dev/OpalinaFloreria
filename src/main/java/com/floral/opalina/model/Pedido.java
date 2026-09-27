package com.floral.opalina.model;

import com.floral.opalina.model.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class Pedido {
    private final String id, clienteId, nombreCliente, email, telefono;
    private final List<ItemPedido> items;
    private final Entrega entrega;
    private final BigDecimal subtotal, costoDelivery, total;
    private EstadoPedido estado;
    private final EstadoPago estadoPago;
    private final LocalDateTime creadoEn;
    private final List<CambioEstado> historial;

    public Pedido(String id, String clienteId, String nombreCliente, String email, String telefono, List<ItemPedido> items, Entrega entrega, BigDecimal subtotal, BigDecimal costoDelivery, BigDecimal total, EstadoPedido estado, EstadoPago estadoPago, LocalDateTime creadoEn, List<CambioEstado> historial) {
        this.id = id;
        this.clienteId = clienteId;
        this.nombreCliente = nombreCliente;
        this.email = email;
        this.telefono = telefono;
        this.items = List.copyOf(items);
        this.entrega = entrega;
        this.subtotal = subtotal;
        this.costoDelivery = costoDelivery;
        this.total = total;
        this.estado = estado;
        this.estadoPago = estadoPago;
        this.creadoEn = creadoEn;
        this.historial = new ArrayList<>(historial);
    }

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public List<ItemPedido> getItems() {
        return items;
    }

    public Entrega getEntrega() {
        return entrega;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getCostoDelivery() {
        return costoDelivery;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public synchronized EstadoPedido getEstado() {
        return estado;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public synchronized List<CambioEstado> getHistorial() {
        return List.copyOf(historial);
    }

    public synchronized void cambiarEstado(EstadoPedido nuevo) {
        estado = nuevo;
        historial.add(new CambioEstado(nuevo, LocalDateTime.now()));
    }
}
