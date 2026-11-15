package polimarket.seguridad.infraestructura;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.seguridad.api.UsuarioSesion;
import polimarket.seguridad.dominio.UsuarioRepositorio;

/** Le dice a Spring Security cómo encontrar una cuenta por su correo. */
@Service
class UsuarioDetallesService implements UserDetailsService {

    private final UsuarioRepositorio usuarios;

    UsuarioDetallesService(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) {
        return usuarios.findByCorreoIgnoreCase(correo.trim())
                .map(u -> new UsuarioSesion(u.getId(), u.getTerceroId(), u.getCorreo(), u.getContrasenaHash(),
                        u.getRol(), u.isActivo()))
                .orElseThrow(() -> new UsernameNotFoundException("Cuenta no encontrada"));
    }
}
