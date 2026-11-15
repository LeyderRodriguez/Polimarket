-- Una sola bodega en el prototipo. proveedor_id, venta_id y usuario_id apuntan a otros
-- componentes por identificador, sin llave foránea entre esquemas.
CREATE TABLE bodega.bodega (
    id        BIGSERIAL PRIMARY KEY,
    nombre    VARCHAR(80) NOT NULL,
    ubicacion VARCHAR(120)
);
INSERT INTO bodega.bodega (id, nombre, ubicacion) VALUES (1, 'Bodega principal', 'Sede central');
SELECT setval(pg_get_serial_sequence('bodega.bodega', 'id'), 1);

CREATE TABLE bodega.producto (
    id           BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(30)    NOT NULL UNIQUE,
    nombre       VARCHAR(120)   NOT NULL,
    descripcion  VARCHAR(300),
    categoria    VARCHAR(60),
    precio_venta NUMERIC(12, 2) NOT NULL CHECK (precio_venta >= 0),
    activo       BOOLEAN        NOT NULL DEFAULT TRUE
);

-- cantidad es el stock físico. Disponible = cantidad - reservas ACTIVAS.
CREATE TABLE bodega.existencia (
    id           BIGSERIAL PRIMARY KEY,
    bodega_id    BIGINT  NOT NULL REFERENCES bodega.bodega (id),
    producto_id  BIGINT  NOT NULL REFERENCES bodega.producto (id),
    cantidad     INTEGER NOT NULL CHECK (cantidad >= 0),
    stock_minimo INTEGER NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    CONSTRAINT uq_existencia UNIQUE (bodega_id, producto_id)
);

CREATE TABLE bodega.compra (
    id             BIGSERIAL PRIMARY KEY,
    bodega_id      BIGINT         NOT NULL REFERENCES bodega.bodega (id),
    proveedor_id   BIGINT         NOT NULL,
    fecha          TIMESTAMPTZ    NOT NULL,
    numero_factura VARCHAR(50),
    total          NUMERIC(14, 2) NOT NULL DEFAULT 0
);

CREATE TABLE bodega.detalle_compra (
    id             BIGSERIAL PRIMARY KEY,
    compra_id      BIGINT         NOT NULL REFERENCES bodega.compra (id),
    producto_id    BIGINT         NOT NULL REFERENCES bodega.producto (id),
    cantidad       INTEGER        NOT NULL CHECK (cantidad > 0),
    costo_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0
);

CREATE TABLE bodega.movimiento_inventario (
    id               BIGSERIAL PRIMARY KEY,
    bodega_id        BIGINT      NOT NULL REFERENCES bodega.bodega (id),
    producto_id      BIGINT      NOT NULL REFERENCES bodega.producto (id),
    fecha            TIMESTAMPTZ NOT NULL,
    tipo             VARCHAR(10) NOT NULL,
    cantidad         INTEGER     NOT NULL,
    cantidad_anterior INTEGER    NOT NULL,
    cantidad_nueva   INTEGER     NOT NULL,
    motivo           VARCHAR(300),
    referencia       VARCHAR(60),
    usuario_id       BIGINT      NOT NULL,
    compra_id        BIGINT REFERENCES bodega.compra (id)
);

CREATE INDEX ix_movimiento_producto ON bodega.movimiento_inventario (producto_id, fecha);

-- Reservas por venta. venta_id referencia a ventas.venta por identificador.
CREATE TABLE bodega.reserva_inventario (
    id       BIGSERIAL PRIMARY KEY,
    venta_id BIGINT      NOT NULL,
    fecha    TIMESTAMPTZ NOT NULL,
    estado   VARCHAR(12) NOT NULL
);

CREATE TABLE bodega.detalle_reserva (
    id          BIGSERIAL PRIMARY KEY,
    reserva_id  BIGINT  NOT NULL REFERENCES bodega.reserva_inventario (id),
    producto_id BIGINT  NOT NULL REFERENCES bodega.producto (id),
    cantidad    INTEGER NOT NULL CHECK (cantidad > 0)
);

CREATE INDEX ix_detalle_reserva_producto ON bodega.detalle_reserva (producto_id);
