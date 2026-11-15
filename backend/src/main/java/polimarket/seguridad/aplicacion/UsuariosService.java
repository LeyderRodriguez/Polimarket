package polimarket.seguridad.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.seguridad.api.IAutenticacion;
import polimarket.seguridad.api.NuevoUsuario;
import polimarket.seguridad.api.UsuarioDto;
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

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDto> listarUsuarios() {
        return usuarios.findAll().stream().map(UsuariosService::aDto).toList();
    }

    @Override
    public UsuarioDto actualizarCorreo(long id, String correo) {
        if (correo == null || !correo.contains("@")) {
            throw ExcepcionNegocio.datosInvalidos("El correo debe ser válido.");
        }
        Usuario u = exigir(id);
        String nuevo = correo.trim().toLowerCase();
        if (!nuevo.equalsIgnoreCase(u.getCorreo()) && usuarios.existsByCorreoIgnoreCase(nuevo)) {
            throw ExcepcionNegocio.conflicto("CORREO_DUPLICADO", "Ya existe una cuenta con ese correo.");
        }
        u.cambiarCorreo(nuevo);
        return aDto(u);
    }

    @Override
    public UsuarioDto desactivar(long id) {
        Usuario u = exigir(id);
        u.desactivar();
        return aDto(u);
    }

    private Usuario exigir(long id) {
        return usuarios.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró la cuenta."));
    }

    private static UsuarioDto aDto(Usuario u) {
        return new UsuarioDto(u.getId(), u.getTerceroId(), u.getCorreo(), u.getRol(), u.isActivo());
    }
}
