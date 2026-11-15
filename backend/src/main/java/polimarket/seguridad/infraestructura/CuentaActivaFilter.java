package polimarket.seguridad.infraestructura;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import polimarket.seguridad.api.UsuarioSesion;
import polimarket.seguridad.dominio.UsuarioRepositorio;

/**
 * Si la cuenta de una sesión abierta fue desactivada, cierra esa sesión en la siguiente petición y responde 401.
 * Cuesta una lectura por clave primaria por petición; para el tamaño de este sistema es aceptable y evita
 * mantener un registro de sesiones.
 */
class CuentaActivaFilter extends OncePerRequestFilter {

    private final UsuarioRepositorio usuarios;

    CuentaActivaFilter(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioSesion usuario
                && !usuarios.findById(usuario.getId()).map(u -> u.isActivo()).orElse(false)) {
            HttpSession sesion = request.getSession(false);
            if (sesion != null) {
                sesion.invalidate();
            }
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"codigo\":\"CUENTA_DESACTIVADA\",\"mensaje\":\"Su cuenta fue desactivada.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
