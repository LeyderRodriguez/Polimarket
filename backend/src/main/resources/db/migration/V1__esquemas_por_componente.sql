-- Un esquema por componente de negocio. Cada componente solo toca sus propias tablas
-- y se refiere a las de otro por identificador (sin llaves foráneas entre esquemas).
CREATE SCHEMA IF NOT EXISTS seguridad;
CREATE SCHEMA IF NOT EXISTS terceros;
CREATE SCHEMA IF NOT EXISTS recursos_humanos;
CREATE SCHEMA IF NOT EXISTS ventas;
CREATE SCHEMA IF NOT EXISTS bodega;
CREATE SCHEMA IF NOT EXISTS proveedores;
CREATE SCHEMA IF NOT EXISTS entregas;
