package polimarket.api;

import polimarket.seguridad.RolUsuario;

/** Quién está autenticado: lo que ven los clientes tras el login o en /api/auth/me. */
public record PerfilDto(Long id, String nombre, RolUsuario rol) {
}
