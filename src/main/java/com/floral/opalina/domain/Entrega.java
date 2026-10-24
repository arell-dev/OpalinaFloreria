package com.floral.opalina.domain;

import com.floral.opalina.domain.enums.*;
import java.time.*;

public record Entrega(ModalidadEntrega modalidad, String direccion, String referencia, LocalDate fecha, LocalTime hora, String notas, MetodoPago metodoPago) { }
