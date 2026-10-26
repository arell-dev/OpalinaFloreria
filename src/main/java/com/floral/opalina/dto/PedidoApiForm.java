package com.floral.opalina.dto;

import com.floral.opalina.domain.enums.MetodoPago;
import com.floral.opalina.domain.enums.ModalidadEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class PedidoApiForm {
    @NotNull
    @Positive
    private Long clienteId;

    @NotNull
    private ModalidadEntrega modalidad;
    private String direccion;
    private String referencia;

    @NotNull
    @Future
    private LocalDate fecha;

    @NotNull
    private LocalTime hora;

    @NotNull
    private MetodoPago metodoPago;

    @NotEmpty
    @Valid
    private List<Linea> items;

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public ModalidadEntrega getModalidad() {
        return modalidad;
    }

    public void setModalidad(ModalidadEntrega modalidad) {
        this.modalidad = modalidad;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public List<Linea> getItems() {
        return items;
    }

    public void setItems(List<Linea> items) {
        this.items = items;
    }

    public static class Linea {
        @NotNull
        @Positive
        private Long productoId;
        
        @Min(1)
        private int cantidad;

        public Long getProductoId() {
            return productoId;
        }

        public void setProductoId(Long productoId) {
            this.productoId = productoId;
        }

        public int getCantidad() {
            return cantidad;
        }

        public void setCantidad(int cantidad) {
            this.cantidad = cantidad;
        }
    }
}

