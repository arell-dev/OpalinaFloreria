package com.floral.opalina.domain;

import com.floral.opalina.domain.enums.EstadoPedido;
import java.time.LocalDateTime;

public record CambioEstado(EstadoPedido estado, LocalDateTime fecha) { }
