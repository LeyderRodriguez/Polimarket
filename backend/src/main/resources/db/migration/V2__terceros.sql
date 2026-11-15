CREATE TABLE terceros.tercero (
    id               BIGSERIAL PRIMARY KEY,
    tipo             VARCHAR(10)  NOT NULL,
    tipo_documento   VARCHAR(10)  NOT NULL,
    numero_documento VARCHAR(30)  NOT NULL,
    nombres          VARCHAR(100),
    apellidos        VARCHAR(100),
    razon_social     VARCHAR(150),
    telefono         VARCHAR(30),
    correo           VARCHAR(150),
    direccion        VARCHAR(200),
    CONSTRAINT uq_tercero_documento UNIQUE (tipo_documento, numero_documento)
);

CREATE TABLE terceros.rol_tercero (
    id               BIGSERIAL PRIMARY KEY,
    tercero_id       BIGINT      NOT NULL REFERENCES terceros.tercero (id),
    tipo_rol         VARCHAR(20) NOT NULL,
    fecha_asignacion DATE        NOT NULL,
    activo           BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_rol_tercero UNIQUE (tercero_id, tipo_rol)
);
