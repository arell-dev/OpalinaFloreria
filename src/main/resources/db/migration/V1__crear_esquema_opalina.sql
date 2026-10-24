CREATE TABLE cliente (
    cliente_id VARCHAR(64) NOT NULL,
    nombre VARCHAR(161) NOT NULL,
    email VARCHAR(254) NOT NULL,
    telefono VARCHAR(20) NULL,
    direccion VARCHAR(180) NULL,
    referencia VARCHAR(180) NULL,
    creado_en DATETIME NOT NULL,
    CONSTRAINT pk_cliente PRIMARY KEY (cliente_id),
    CONSTRAINT uq_cliente_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE credencial (
    cliente_id VARCHAR(64) NOT NULL,
    clave_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    creado_en DATETIME NOT NULL,
    actualizado_en DATETIME NULL,
    CONSTRAINT pk_credencial PRIMARY KEY (cliente_id),
    CONSTRAINT fk_credencial_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT,
    CONSTRAINT ck_credencial_rol CHECK (rol IN ('CLIENTE', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE producto (
    producto_id VARCHAR(100) NOT NULL,
    sku VARCHAR(30) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT NULL,
    categoria VARCHAR(60) NOT NULL,
    imagen_url VARCHAR(500) NULL,
    precio DECIMAL(10,2) NOT NULL,
    stock INT UNSIGNED NOT NULL DEFAULT 0,
    disponible BOOLEAN NOT NULL,
    destacado BOOLEAN NOT NULL,
    CONSTRAINT pk_producto PRIMARY KEY (producto_id),
    CONSTRAINT uq_producto_sku UNIQUE (sku),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    CONSTRAINT ck_producto_stock CHECK (stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE configuracion_tienda (
    configuracion_id TINYINT UNSIGNED NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    email VARCHAR(254) NOT NULL,
    direccion VARCHAR(180) NOT NULL,
    whatsapp VARCHAR(20) NOT NULL,
    zona_delivery VARCHAR(200) NOT NULL,
    horario_general VARCHAR(200) NOT NULL,
    instagram VARCHAR(255) NULL,
    facebook VARCHAR(255) NULL,
    tiktok VARCHAR(255) NULL,
    costo_delivery DECIMAL(10,2) NOT NULL,
    delivery_gratis_desde DECIMAL(10,2) NOT NULL,
    ultimo_numero_pedido BIGINT NOT NULL DEFAULT 100,
    CONSTRAINT pk_configuracion_tienda PRIMARY KEY (configuracion_id),
    CONSTRAINT ck_configuracion_unica CHECK (configuracion_id = 1),
    CONSTRAINT ck_configuracion_costos CHECK (costo_delivery >= 0 AND delivery_gratis_desde >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pedido (
    pedido_id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(24) NOT NULL,
    cliente_id VARCHAR(64) NOT NULL,
    email_contacto VARCHAR(254) NOT NULL,
    estado VARCHAR(24) NOT NULL,
    estado_pago VARCHAR(20) NOT NULL,
    modalidad_entrega VARCHAR(20) NOT NULL,
    metodo_pago VARCHAR(24) NOT NULL,
    fecha_solicitada DATE NOT NULL,
    hora_solicitada TIME NOT NULL,
    costo_delivery DECIMAL(10,2) NOT NULL,
    creado_en DATETIME NOT NULL,
    CONSTRAINT pk_pedido PRIMARY KEY (pedido_id),
    CONSTRAINT uq_pedido_codigo UNIQUE (codigo),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT,
    CONSTRAINT ck_pedido_estado CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'ENVIADO', 'ENTREGADO', 'CANCELADO')),
    CONSTRAINT ck_pedido_estado_pago CHECK (estado_pago IN ('PENDIENTE', 'PAGADO')),
    CONSTRAINT ck_pedido_modalidad CHECK (modalidad_entrega IN ('DELIVERY', 'RECOJO')),
    CONSTRAINT ck_pedido_metodo_pago CHECK (metodo_pago IN ('YAPE', 'EFECTIVO', 'TRANSFERENCIA')),
    CONSTRAINT ck_pedido_delivery CHECK (costo_delivery >= 0),
    INDEX ix_pedido_cliente_fecha (cliente_id, creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE destinatario (
    destinatario_id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    direccion VARCHAR(180) NULL,
    referencia VARCHAR(180) NULL,
    dedicatoria VARCHAR(300) NULL,
    CONSTRAINT pk_destinatario PRIMARY KEY (destinatario_id),
    CONSTRAINT uq_destinatario_pedido UNIQUE (pedido_id),
    CONSTRAINT fk_destinatario_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (pedido_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE item_pedido (
    item_pedido_id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    producto_id VARCHAR(100) NOT NULL,
    nombre_producto VARCHAR(120) NOT NULL,
    categoria VARCHAR(60) NOT NULL,
    imagen_url VARCHAR(500) NULL,
    cantidad INT UNSIGNED NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    CONSTRAINT pk_item_pedido PRIMARY KEY (item_pedido_id),
    CONSTRAINT uq_item_pedido_producto UNIQUE (pedido_id, producto_id),
    CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (pedido_id) ON DELETE RESTRICT,
    CONSTRAINT fk_item_pedido_producto FOREIGN KEY (producto_id) REFERENCES producto (producto_id) ON DELETE RESTRICT,
    CONSTRAINT ck_item_pedido_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_item_pedido_precio CHECK (precio_unitario >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cambio_estado_pedido (
    cambio_id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    estado VARCHAR(24) NOT NULL,
    fecha DATETIME NOT NULL,
    CONSTRAINT pk_cambio_estado_pedido PRIMARY KEY (cambio_id),
    CONSTRAINT fk_cambio_estado_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (pedido_id) ON DELETE RESTRICT,
    CONSTRAINT ck_cambio_estado CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'ENVIADO', 'ENTREGADO', 'CANCELADO')),
    INDEX ix_cambio_estado_fecha (pedido_id, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pago (
    pago_id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    metodo VARCHAR(24) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    referencia_operacion VARCHAR(100) NULL,
    registrado_en DATETIME NOT NULL,
    revisado_en DATETIME NULL,
    revisado_por_cliente_id VARCHAR(64) NULL,
    observacion VARCHAR(300) NULL,
    CONSTRAINT pk_pago PRIMARY KEY (pago_id),
    CONSTRAINT fk_pago_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (pedido_id) ON DELETE RESTRICT,
    CONSTRAINT fk_pago_revisor FOREIGN KEY (revisado_por_cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT,
    CONSTRAINT ck_pago_metodo CHECK (metodo IN ('YAPE', 'EFECTIVO', 'TRANSFERENCIA')),
    CONSTRAINT ck_pago_estado CHECK (estado IN ('PENDIENTE', 'VALIDADO', 'RECHAZADO')),
    CONSTRAINT ck_pago_monto CHECK (monto > 0),
    INDEX ix_pago_pedido_fecha (pedido_id, registrado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comprobante_pago (
    comprobante_pago_id BIGINT NOT NULL AUTO_INCREMENT,
    pago_id BIGINT NOT NULL,
    evidencia_url VARCHAR(500) NOT NULL,
    estado_revision VARCHAR(20) NOT NULL,
    fecha_envio DATETIME NOT NULL,
    fecha_revision DATETIME NULL,
    revisado_por_cliente_id VARCHAR(64) NULL,
    observacion VARCHAR(300) NULL,
    CONSTRAINT pk_comprobante_pago PRIMARY KEY (comprobante_pago_id),
    CONSTRAINT fk_comprobante_pago_pago FOREIGN KEY (pago_id) REFERENCES pago (pago_id) ON DELETE RESTRICT,
    CONSTRAINT fk_comprobante_pago_revisor FOREIGN KEY (revisado_por_cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT,
    CONSTRAINT ck_comprobante_revision CHECK (estado_revision IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    INDEX ix_comprobante_pago_fecha (pago_id, fecha_envio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comprobante_digital (
    comprobante_digital_id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    numero VARCHAR(40) NOT NULL,
    fecha_emision DATETIME NOT NULL,
    total_emitido DECIMAL(10,2) NOT NULL,
    archivo_url VARCHAR(500) NOT NULL,
    CONSTRAINT pk_comprobante_digital PRIMARY KEY (comprobante_digital_id),
    CONSTRAINT uq_comprobante_digital_pedido UNIQUE (pedido_id),
    CONSTRAINT uq_comprobante_digital_numero UNIQUE (numero),
    CONSTRAINT fk_comprobante_digital_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (pedido_id) ON DELETE RESTRICT,
    CONSTRAINT ck_comprobante_digital_total CHECK (total_emitido >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE programacion_entrega (
    programacion_id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    fecha_programada DATE NOT NULL,
    hora_programada TIME NOT NULL,
    repartidor_asignado VARCHAR(80) NULL,
    telefono_repartidor VARCHAR(20) NULL,
    confirmacion_recepcion BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_entrega_real DATETIME NULL,
    observaciones VARCHAR(300) NULL,
    CONSTRAINT pk_programacion_entrega PRIMARY KEY (programacion_id),
    CONSTRAINT uq_programacion_pedido UNIQUE (pedido_id),
    CONSTRAINT fk_programacion_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (pedido_id) ON DELETE RESTRICT,
    INDEX ix_programacion_fecha_hora (fecha_programada, hora_programada)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE insumo (
    insumo_id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    categoria VARCHAR(60) NOT NULL,
    unidad_medida VARCHAR(20) NOT NULL,
    stock_actual DECIMAL(10,3) NOT NULL DEFAULT 0,
    stock_minimo DECIMAL(10,3) NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_insumo PRIMARY KEY (insumo_id),
    CONSTRAINT ck_insumo_stocks CHECK (stock_actual >= 0 AND stock_minimo >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE producto_insumo (
    producto_id VARCHAR(100) NOT NULL,
    insumo_id BIGINT NOT NULL,
    cantidad_requerida DECIMAL(10,3) NOT NULL,
    CONSTRAINT pk_producto_insumo PRIMARY KEY (producto_id, insumo_id),
    CONSTRAINT fk_producto_insumo_producto FOREIGN KEY (producto_id) REFERENCES producto (producto_id) ON DELETE RESTRICT,
    CONSTRAINT fk_producto_insumo_insumo FOREIGN KEY (insumo_id) REFERENCES insumo (insumo_id) ON DELETE RESTRICT,
    CONSTRAINT ck_producto_insumo_cantidad CHECK (cantidad_requerida > 0),
    INDEX ix_producto_insumo_insumo (insumo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE movimiento_insumo (
    movimiento_id BIGINT NOT NULL AUTO_INCREMENT,
    insumo_id BIGINT NOT NULL,
    tipo_movimiento VARCHAR(20) NOT NULL,
    sentido VARCHAR(20) NOT NULL,
    cantidad DECIMAL(10,3) NOT NULL,
    fecha DATETIME NOT NULL,
    motivo VARCHAR(255) NULL,
    CONSTRAINT pk_movimiento_insumo PRIMARY KEY (movimiento_id),
    CONSTRAINT fk_movimiento_insumo_insumo FOREIGN KEY (insumo_id) REFERENCES insumo (insumo_id) ON DELETE RESTRICT,
    CONSTRAINT ck_movimiento_tipo CHECK (tipo_movimiento IN ('ENTRADA', 'CONSUMO', 'AJUSTE')),
    CONSTRAINT ck_movimiento_sentido CHECK (sentido IN ('AUMENTO', 'DISMINUCION')),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_movimiento_coherencia CHECK ((tipo_movimiento = 'ENTRADA' AND sentido = 'AUMENTO') OR (tipo_movimiento = 'CONSUMO' AND sentido = 'DISMINUCION') OR tipo_movimiento = 'AJUSTE'),
    INDEX ix_movimiento_insumo_fecha (insumo_id, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE consulta_contacto (
    consulta_contacto_id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id VARCHAR(64) NULL,
    nombre VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL,
    telefono VARCHAR(20) NULL,
    asunto VARCHAR(120) NULL,
    mensaje TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    creado_en DATETIME NOT NULL,
    respuesta TEXT NULL,
    respondido_en DATETIME NULL,
    atendido_por_cliente_id VARCHAR(64) NULL,
    CONSTRAINT pk_consulta_contacto PRIMARY KEY (consulta_contacto_id),
    CONSTRAINT fk_consulta_contacto_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT,
    CONSTRAINT fk_consulta_contacto_atendido FOREIGN KEY (atendido_por_cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT,
    CONSTRAINT ck_consulta_contacto_estado CHECK (estado IN ('PENDIENTE', 'ATENDIDA')),
    INDEX ix_consulta_contacto_estado_fecha (estado, creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
