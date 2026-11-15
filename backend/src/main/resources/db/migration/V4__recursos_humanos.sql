-- tercero_id apunta a terceros.tercero por identificador, sin llave foránea entre esquemas.
CREATE TABLE recursos_humanos.empleado (
    id            BIGSERIAL PRIMARY KEY,
    tercero_id    BIGINT       NOT NULL UNIQUE,
    cargo         VARCHAR(80)  NOT NULL,
    fecha_ingreso DATE         NOT NULL,
    estado        VARCHAR(15)  NOT NULL DEFAULT 'ACTIVO'
);

CREATE TABLE recursos_humanos.vendedor (
    id          BIGSERIAL PRIMARY KEY,
    empleado_id BIGINT NOT NULL UNIQUE REFERENCES recursos_humanos.empleado (id),
    zona        VARCHAR(60)
);

CREATE TABLE recursos_humanos.aval_rh (
    id              BIGSERIAL PRIMARY KEY,
    vendedor_id     BIGINT      NOT NULL REFERENCES recursos_humanos.vendedor (id),
    estado          VARCHAR(15) NOT NULL,
    fecha_solicitud TIMESTAMPTZ NOT NULL,
    fecha_respuesta TIMESTAMPTZ,
    responsable_id  BIGINT REFERENCES recursos_humanos.empleado (id),
    observacion     VARCHAR(500)
);

CREATE INDEX ix_aval_vendedor ON recursos_humanos.aval_rh (vendedor_id, id DESC);
