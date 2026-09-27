package com.floral.opalina.model;

import com.floral.opalina.model.enums.*;
import java.time.*;

public record Entrega(ModalidadEntrega modalidad, String direccion, String referencia, LocalDate fecha, LocalTime hora, String notas, MetodoPago metodoPago) { }
