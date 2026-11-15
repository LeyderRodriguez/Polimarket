-- tercero_id, vendedor_id y producto_id apuntan a otros componentes por identificador,
-- sin llave foránea entre esquemas. Las llaves foráneas son solo dentro de ventas.
CREATE TABLE ventas.cliente (
    id             BIGSERIAL PRIMARY KEY,
    tercero_id     BIGINT NOT NULL UNIQUE,
    fecha_registro DATE   NOT NULL,
    preferencias   VARCHAR(500)
);

CREATE TABLE ventas.venta (
    id                BIGSERIAL PRIMARY KEY,
    cliente_id        BIGINT         NOT NULL REFERENCES ventas.cliente (id),
    vendedor_id       BIGINT         NOT NULL,
    fecha             TIMESTAMPTZ    NOT NULL,
    direccion_entrega VARCHAR(200)   NOT NULL,
    total             NUMERIC(14, 2) NOT NULL,
    estado            VARCHAR(20)    NOT NULL
);

CREATE INDEX ix_venta_vendedor ON ventas.venta (vendedor_id, id DESC);
CREATE INDEX ix_venta_estado ON ventas.venta (estado);

CREATE TABLE ventas.detalle_venta (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT         NOT NULL REFERENCES ventas.venta (id),
    producto_id     BIGINT         NOT NULL,
    cantidad        INTEGER        NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12, 2) NOT NULL
);

CREATE INDEX ix_detalle_venta ON ventas.detalle_venta (venta_id);
