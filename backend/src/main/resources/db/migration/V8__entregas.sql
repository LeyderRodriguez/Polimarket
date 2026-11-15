-- venta_id es único: una venta se despacha una sola vez, también si dos solicitudes llegan a la vez.
CREATE TABLE entregas.entrega (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT       NOT NULL UNIQUE,
    fecha_despacho  TIMESTAMPTZ  NOT NULL,
    direccion       VARCHAR(200) NOT NULL,
    receptor        VARCHAR(150),
    estado          VARCHAR(15)  NOT NULL,
    responsable_id  BIGINT       NOT NULL
);
