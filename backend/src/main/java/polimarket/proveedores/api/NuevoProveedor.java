package polimarket.proveedores.api;

/** El proveedor es un tercero que ya existe (persona o empresa); aquí se agregan sus datos de contacto. */
public record NuevoProveedor(long terceroId, String nombreContacto, String telefonoContacto) {
}
