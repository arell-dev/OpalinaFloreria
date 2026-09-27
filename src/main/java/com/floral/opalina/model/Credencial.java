package com.floral.opalina.model;

import com.floral.opalina.model.enums.Rol;

public record Credencial(String clienteId, String email, String claveTemporal, Rol rol) { }
