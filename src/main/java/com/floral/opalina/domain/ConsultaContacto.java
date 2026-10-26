package com.floral.opalina.domain;

import java.time.LocalDateTime;

public record ConsultaContacto(Long clienteId, String nombre, String email, String telefono, String asunto, String mensaje, LocalDateTime creadoEn) {
}
