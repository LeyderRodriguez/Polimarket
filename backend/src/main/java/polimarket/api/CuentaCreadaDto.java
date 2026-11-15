package polimarket.api;

import polimarket.recursoshumanos.EstadoAval;
import polimarket.seguridad.RolUsuario;

/** Cuenta recién creada. Para un vendedor incluye su registro y el aval, que empieza PENDIENTE. */
public record CuentaCreadaDto(
        Long id,
        Long terceroId,
        String correo,
        RolUsuario rol,
        boolean activo,
        Long vendedorId,
        EstadoAval estadoAval) {
}
