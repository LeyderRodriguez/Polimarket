package polimarket.recursoshumanos;

/** Vendedor con su estado de aval actual. El código se deriva del identificador (VEN-0001). */
public record VendedorDto(Long id, Long terceroId, String nombre, String codigo, String zona, EstadoAval estadoAval) {
}
