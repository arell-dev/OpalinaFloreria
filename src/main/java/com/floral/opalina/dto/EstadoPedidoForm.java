package com.floral.opalina.dto;

import com.floral.opalina.model.enums.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public class EstadoPedidoForm {
    @NotNull
    private EstadoPedido estado;

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }
}

