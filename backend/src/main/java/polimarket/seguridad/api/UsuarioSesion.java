package polimarket.seguridad.api;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Usuario autenticado que guarda la sesión del servidor. Los controllers lo reciben con
 * {@code @AuthenticationPrincipal}: así la identidad sale de la sesión y no de lo que envía el cliente.
 */
public class UsuarioSesion implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long terceroId;
    private final String correo;
    private String contrasenaHash; // se borra tras autenticar: la sesión no debe guardar el hash
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

    /** Spring Security lo llama después de autenticar para que el hash no se quede en la sesión. */
    @Override
    public void eraseCredentials() {
        contrasenaHash = null;
    }

    @Override
    public String getUsername() { return correo; }

    @Override
    public boolean isEnabled() { return activo; }
}
