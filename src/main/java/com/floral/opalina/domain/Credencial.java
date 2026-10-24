package com.floral.opalina.domain;

import com.floral.opalina.domain.enums.Rol;

public record Credencial(String clienteId, String email, String claveHash, Rol rol) { }
