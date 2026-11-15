package polimarket.seguridad;

/** Cuenta sin contraseña ni hash. */
public record UsuarioDto(Long id, Long terceroId, String correo, RolUsuario rol, boolean activo) {
}
