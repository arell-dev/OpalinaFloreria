package com.floral.opalina.domain;

import com.floral.opalina.domain.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class Pedido {
    private final Long id, clienteId;
    private final String codigo, nombreCliente, email, telefono;
    private final List<ItemPedido> items;
    private final Entrega entrega;
    private final BigDecimal subtotal, costoDelivery, total;
    private EstadoPedido estado;
    private final EstadoPago estadoPago;
    private final LocalDateTime creadoEn;
    private final List<CambioEstado> historial;

    public Pedido(Long id, String codigo, Long clienteId, String nombreCliente, String email, String telefono, List<ItemPedido> items, Entrega entrega, BigDecimal subtotal, BigDecimal costoDelivery, BigDecimal total, EstadoPedido estado, EstadoPago estadoPago, LocalDateTime creadoEn, List<CambioEstado> historial) {
        this.id = id;
        this.codigo = codigo;
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

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Long getClienteId() {
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
