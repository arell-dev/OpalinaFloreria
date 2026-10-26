CREATE TEMPORARY TABLE tmp_cliente_ids (
    id_anterior VARCHAR(64) NOT NULL PRIMARY KEY,
    id_nuevo BIGINT NOT NULL UNIQUE,
    marcador VARCHAR(64) NOT NULL UNIQUE
) ENGINE=InnoDB;

INSERT INTO tmp_cliente_ids (id_anterior, id_nuevo, marcador)
SELECT cliente_id,
       ROW_NUMBER() OVER (ORDER BY creado_en, cliente_id),
       CONCAT('__mcli_', ROW_NUMBER() OVER (ORDER BY creado_en, cliente_id), '_', REPLACE(UUID(), '-', ''))
FROM cliente;

CREATE TEMPORARY TABLE tmp_producto_ids (
    id_anterior VARCHAR(100) NOT NULL PRIMARY KEY,
    id_nuevo BIGINT NOT NULL UNIQUE,
    marcador VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

INSERT INTO tmp_producto_ids (id_anterior, id_nuevo, marcador)
SELECT producto_id,
       ROW_NUMBER() OVER (ORDER BY sku, producto_id),
       CONCAT('__mprod_', ROW_NUMBER() OVER (ORDER BY sku, producto_id), '_', REPLACE(UUID(), '-', ''))
FROM producto;

ALTER TABLE credencial DROP FOREIGN KEY fk_credencial_cliente;
ALTER TABLE pedido DROP FOREIGN KEY fk_pedido_cliente;
ALTER TABLE pago DROP FOREIGN KEY fk_pago_revisor;
ALTER TABLE comprobante_pago DROP FOREIGN KEY fk_comprobante_pago_revisor;
ALTER TABLE consulta_contacto
    DROP FOREIGN KEY fk_consulta_contacto_cliente,
    DROP FOREIGN KEY fk_consulta_contacto_atendido;
ALTER TABLE item_pedido DROP FOREIGN KEY fk_item_pedido_producto;
ALTER TABLE producto_insumo DROP FOREIGN KEY fk_producto_insumo_producto;

UPDATE credencial c
JOIN tmp_cliente_ids m ON c.cliente_id = m.id_anterior
SET c.cliente_id = m.marcador;
UPDATE pedido p
JOIN tmp_cliente_ids m ON p.cliente_id = m.id_anterior
SET p.cliente_id = m.marcador;
UPDATE pago p
JOIN tmp_cliente_ids m ON p.revisado_por_cliente_id = m.id_anterior
SET p.revisado_por_cliente_id = m.marcador;
UPDATE comprobante_pago c
JOIN tmp_cliente_ids m ON c.revisado_por_cliente_id = m.id_anterior
SET c.revisado_por_cliente_id = m.marcador;
UPDATE consulta_contacto c
JOIN tmp_cliente_ids m ON c.cliente_id = m.id_anterior
SET c.cliente_id = m.marcador;
UPDATE consulta_contacto c
JOIN tmp_cliente_ids m ON c.atendido_por_cliente_id = m.id_anterior
SET c.atendido_por_cliente_id = m.marcador;
UPDATE cliente c
JOIN tmp_cliente_ids m ON c.cliente_id = m.id_anterior
SET c.cliente_id = m.marcador;

UPDATE credencial c
JOIN tmp_cliente_ids m ON c.cliente_id = m.marcador
SET c.cliente_id = CAST(m.id_nuevo AS CHAR);
UPDATE pedido p
JOIN tmp_cliente_ids m ON p.cliente_id = m.marcador
SET p.cliente_id = CAST(m.id_nuevo AS CHAR);
UPDATE pago p
JOIN tmp_cliente_ids m ON p.revisado_por_cliente_id = m.marcador
SET p.revisado_por_cliente_id = CAST(m.id_nuevo AS CHAR);
UPDATE comprobante_pago c
JOIN tmp_cliente_ids m ON c.revisado_por_cliente_id = m.marcador
SET c.revisado_por_cliente_id = CAST(m.id_nuevo AS CHAR);
UPDATE consulta_contacto c
JOIN tmp_cliente_ids m ON c.cliente_id = m.marcador
SET c.cliente_id = CAST(m.id_nuevo AS CHAR);
UPDATE consulta_contacto c
JOIN tmp_cliente_ids m ON c.atendido_por_cliente_id = m.marcador
SET c.atendido_por_cliente_id = CAST(m.id_nuevo AS CHAR);
UPDATE cliente c
JOIN tmp_cliente_ids m ON c.cliente_id = m.marcador
SET c.cliente_id = CAST(m.id_nuevo AS CHAR);

UPDATE item_pedido i
JOIN tmp_producto_ids m ON i.producto_id = m.id_anterior
SET i.producto_id = m.marcador;
UPDATE producto_insumo i
JOIN tmp_producto_ids m ON i.producto_id = m.id_anterior
SET i.producto_id = m.marcador;
UPDATE producto p
JOIN tmp_producto_ids m ON p.producto_id = m.id_anterior
SET p.producto_id = m.marcador;

UPDATE item_pedido i
JOIN tmp_producto_ids m ON i.producto_id = m.marcador
SET i.producto_id = CAST(m.id_nuevo AS CHAR);
UPDATE producto_insumo i
JOIN tmp_producto_ids m ON i.producto_id = m.marcador
SET i.producto_id = CAST(m.id_nuevo AS CHAR);
UPDATE producto p
JOIN tmp_producto_ids m ON p.producto_id = m.marcador
SET p.producto_id = CAST(m.id_nuevo AS CHAR);

ALTER TABLE cliente MODIFY cliente_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE credencial MODIFY cliente_id BIGINT NOT NULL;
ALTER TABLE pedido MODIFY cliente_id BIGINT NOT NULL;
ALTER TABLE pago MODIFY revisado_por_cliente_id BIGINT NULL;
ALTER TABLE comprobante_pago MODIFY revisado_por_cliente_id BIGINT NULL;
ALTER TABLE consulta_contacto
    MODIFY cliente_id BIGINT NULL,
    MODIFY atendido_por_cliente_id BIGINT NULL;

ALTER TABLE producto MODIFY producto_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE item_pedido MODIFY producto_id BIGINT NOT NULL;
ALTER TABLE producto_insumo MODIFY producto_id BIGINT NOT NULL;

ALTER TABLE credencial
    ADD CONSTRAINT fk_credencial_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT;
ALTER TABLE pedido
    ADD CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT;
ALTER TABLE pago
    ADD CONSTRAINT fk_pago_revisor FOREIGN KEY (revisado_por_cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT;
ALTER TABLE comprobante_pago
    ADD CONSTRAINT fk_comprobante_pago_revisor FOREIGN KEY (revisado_por_cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT;
ALTER TABLE consulta_contacto
    ADD CONSTRAINT fk_consulta_contacto_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_consulta_contacto_atendido FOREIGN KEY (atendido_por_cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT;
ALTER TABLE item_pedido
    ADD CONSTRAINT fk_item_pedido_producto FOREIGN KEY (producto_id) REFERENCES producto (producto_id) ON DELETE RESTRICT;
ALTER TABLE producto_insumo
    ADD CONSTRAINT fk_producto_insumo_producto FOREIGN KEY (producto_id) REFERENCES producto (producto_id) ON DELETE RESTRICT;

DROP TEMPORARY TABLE tmp_cliente_ids;
DROP TEMPORARY TABLE tmp_producto_ids;
