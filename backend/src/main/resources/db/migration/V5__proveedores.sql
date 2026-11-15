-- tercero_id apunta a terceros.tercero por identificador. El producto se referencia por código
-- (no por id) para que Proveedores no dependa de Bodega.
CREATE TABLE proveedores.proveedor (
    id                BIGSERIAL PRIMARY KEY,
    tercero_id        BIGINT       NOT NULL UNIQUE,
    nombre_contacto   VARCHAR(100),
    telefono_contacto VARCHAR(30)
);

CREATE TABLE proveedores.producto_suministrado (
    id              BIGSERIAL PRIMARY KEY,
    proveedor_id    BIGINT      NOT NULL REFERENCES proveedores.proveedor (id),
    codigo_producto VARCHAR(30) NOT NULL,
    precio_compra   NUMERIC(12, 2),
    CONSTRAINT uq_proveedor_producto UNIQUE (proveedor_id, codigo_producto)
);

CREATE INDEX ix_suministrado_codigo ON proveedores.producto_suministrado (codigo_producto);
