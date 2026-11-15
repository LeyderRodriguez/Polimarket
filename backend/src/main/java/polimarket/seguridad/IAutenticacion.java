package polimarket.seguridad;

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
}
