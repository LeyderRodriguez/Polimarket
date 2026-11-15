package polimarket.seguridad.api;

/** Datos para crear una cuenta. La contraseña llega en claro y se guarda solo como hash. */
public record NuevoUsuario(long terceroId, String correo, String contrasena, RolUsuario rol) {
}
