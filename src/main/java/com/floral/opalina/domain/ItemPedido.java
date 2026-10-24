package com.floral.opalina.domain;

import java.math.BigDecimal;

public record ItemPedido(String productoId, String nombreProducto, String categoria, String imagen, int cantidad, BigDecimal precioUnitario, BigDecimal totalLinea) { }
