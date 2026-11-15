package polimarket.seguridad;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Usuario autenticado que guarda la sesión del servidor. Los controllers lo reciben con
 * {@code @AuthenticationPrincipal}: así la identidad sale de la sesión y no de lo que envía el cliente.
 */
public class UsuarioSesion implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long terceroId;
    private final String correo;
    private final String contrasenaHash;
    private final RolUsuario rol;
    private final boolean activo;

    public UsuarioSesion(Long id, Long terceroId, String correo, String contrasenaHash, RolUsuario rol, boolean activo) {
        this.id = id;
        this.terceroId = terceroId;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.activo = activo;
    }

    public Long getId() { return id; }
    public Long getTerceroId() { return terceroId; }
    public RolUsuario getRol() { return rol; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() { return contrasenaHash; }

    @Override
    public String getUsername() { return correo; }

    @Override
    public boolean isEnabled() { return activo; }
}
