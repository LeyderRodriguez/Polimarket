package polimarket.seguridad.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.seguridad.IAutenticacion;
import polimarket.seguridad.NuevoUsuario;
import polimarket.seguridad.UsuarioDto;
import polimarket.seguridad.dominio.Usuario;
import polimarket.seguridad.dominio.UsuarioRepositorio;

@Service
@Transactional
class UsuariosService implements IAutenticacion {

    private static final int LARGO_MINIMO_CLAVE = 8;

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final Clock reloj;

    UsuariosService(UsuarioRepositorio usuarios, PasswordEncoder codificador, Clock reloj) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.reloj = reloj;
    }

    @Override
    public UsuarioDto crearUsuario(NuevoUsuario n) {
        if (n == null || n.rol() == null || n.correo() == null || !n.correo().contains("@")) {
            throw ExcepcionNegocio.datosInvalidos("Correo y rol son obligatorios y el correo debe ser válido.");
        }
        if (n.contrasena() == null || n.contrasena().length() < LARGO_MINIMO_CLAVE) {
            throw ExcepcionNegocio.datosInvalidos("La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres.");
        }
        String correo = n.correo().trim().toLowerCase();
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw ExcepcionNegocio.conflicto("CORREO_DUPLICADO", "Ya existe una cuenta con ese correo.");
        }
        if (usuarios.existsByTerceroId(n.terceroId())) {
            throw ExcepcionNegocio.conflicto("TERCERO_CON_CUENTA", "Ese tercero ya tiene una cuenta.");
        }
        Usuario u = usuarios.save(new Usuario(n.terceroId(), correo, codificador.encode(n.contrasena()), n.rol(),
                Instant.now(reloj)));
        return aDto(u);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioDto> consultarPorId(long id) {
        return usuarios.findById(id).map(UsuariosService::aDto);
    }

    private static UsuarioDto aDto(Usuario u) {
        return new UsuarioDto(u.getId(), u.getTerceroId(), u.getCorreo(), u.getRol(), u.isActivo());
    }
}
