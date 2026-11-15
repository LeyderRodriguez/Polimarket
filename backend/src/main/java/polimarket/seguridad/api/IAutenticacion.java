package polimarket.seguridad.api;

import java.util.List;
import java.util.Optional;

/**
 * Contrato del componente Seguridad. El inicio y cierre de sesión los resuelve Spring Security
 * con la configuración de este componente; aquí se expone la gestión de cuentas.
 */
public interface IAutenticacion {

    /**
     * Crea una cuenta con la clave en hash. Lanza CORREO_DUPLICADO si el correo ya existe y
     * TERCERO_CON_CUENTA si el tercero ya tiene una cuenta.
     */
    UsuarioDto crearUsuario(NuevoUsuario usuario);

    Optional<UsuarioDto> consultarPorId(long id);

    /** Cuentas sin contraseña ni hash. */
    List<UsuarioDto> listarUsuarios();

    /** Cambia el correo. Lanza CORREO_DUPLICADO si otra cuenta ya lo usa. El rol no se puede cambiar. */
    UsuarioDto actualizarCorreo(long id, String correo);

    /**
     * Desactiva la cuenta: no puede iniciar sesión y sus sesiones abiertas dejan de valer en la siguiente
     * petición. No se borra. Lanza ESTADO_INVALIDO si ya estaba desactivada.
     */
    UsuarioDto desactivar(long id);
}
