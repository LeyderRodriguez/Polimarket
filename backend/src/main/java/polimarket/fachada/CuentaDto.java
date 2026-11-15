package polimarket.fachada;

import polimarket.seguridad.api.RolUsuario;

/** Cuenta para el listado de RH: sin contraseña ni hash, con el nombre de la persona. */
public record CuentaDto(Long id, Long terceroId, String nombre, String correo, RolUsuario rol, boolean activo) {
}
