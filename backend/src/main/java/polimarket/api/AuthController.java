package polimarket.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.seguridad.UsuarioSesion;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    record SolicitudLogin(@NotBlank String correo, @NotBlank String contrasena) {
    }

    private final AuthenticationManager autenticador;
    private final SecurityContextRepository repositorioDeContexto;
    private final FachadaPoliMarket fachada;

    AuthController(AuthenticationManager autenticador, SecurityContextRepository repositorioDeContexto,
            FachadaPoliMarket fachada) {
        this.autenticador = autenticador;
        this.repositorioDeContexto = repositorioDeContexto;
        this.fachada = fachada;
    }

    @PostMapping("/login")
    PerfilDto login(@Valid @RequestBody SolicitudLogin solicitud, HttpServletRequest request,
            HttpServletResponse response) {
        Authentication autenticacion;
        try {
            autenticacion = autenticador.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(solicitud.correo(), solicitud.contrasena()));
        } catch (AuthenticationException e) {
            throw ExcepcionNegocio.noAutenticado("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos.");
        }
        if (request.getSession(false) != null) {
            request.changeSessionId(); // evita la fijación de sesión
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        repositorioDeContexto.saveContext(contexto, request, response);
        return fachada.perfil((UsuarioSesion) autenticacion.getPrincipal());
    }

    @GetMapping("/me")
    PerfilDto me(@AuthenticationPrincipal UsuarioSesion usuario) {
        return fachada.perfil(usuario);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(Authentication autenticacion, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, autenticacion);
    }
}
