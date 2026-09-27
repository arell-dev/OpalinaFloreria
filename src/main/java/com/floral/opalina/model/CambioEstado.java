package com.floral.opalina.model;

import com.floral.opalina.model.enums.EstadoPedido;
import java.time.LocalDateTime;

public record CambioEstado(EstadoPedido estado, LocalDateTime fecha) { }
