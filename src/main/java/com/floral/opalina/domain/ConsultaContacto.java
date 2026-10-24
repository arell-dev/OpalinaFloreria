package com.floral.opalina.domain;

import java.time.LocalDateTime;

public record ConsultaContacto(String clienteId, String nombre, String email, String telefono, String asunto, String mensaje, LocalDateTime creadoEn) {
}
