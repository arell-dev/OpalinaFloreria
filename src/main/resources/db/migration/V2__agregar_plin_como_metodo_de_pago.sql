ALTER TABLE pedido
    DROP CHECK ck_pedido_metodo_pago;

ALTER TABLE pedido
    ADD CONSTRAINT ck_pedido_metodo_pago
        CHECK (metodo_pago IN ('YAPE', 'PLIN', 'EFECTIVO', 'TRANSFERENCIA'));

ALTER TABLE pago
    DROP CHECK ck_pago_metodo;

ALTER TABLE pago
    ADD CONSTRAINT ck_pago_metodo
        CHECK (metodo IN ('YAPE', 'PLIN', 'EFECTIVO', 'TRANSFERENCIA'));
