package polimarket.recursoshumanos.api;

/** Estado del aval de un vendedor. {@code autorizado} es verdadero solo si está APROBADO. */
public record AvalDto(long vendedorId, EstadoAval estadoAval, boolean autorizado) {
}
