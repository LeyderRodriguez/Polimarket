-- tercero_id apunta a terceros.tercero por identificador, sin llave foránea entre esquemas.
CREATE TABLE seguridad.usuario (
    id              BIGSERIAL PRIMARY KEY,
    tercero_id      BIGINT       NOT NULL UNIQUE,
    correo          VARCHAR(150) NOT NULL,
    contrasena_hash VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX uq_usuario_correo ON seguridad.usuario (LOWER(correo));
