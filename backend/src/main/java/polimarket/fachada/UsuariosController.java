package polimarket.fachada;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import polimarket.seguridad.api.RolUsuario;
import polimarket.seguridad.api.UsuarioDto;
import polimarket.seguridad.api.UsuarioSesion;

@Tag(name = "Cuentas (Recursos Humanos)")
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('RECURSOS_HUMANOS')")
class UsuariosController {

    record SolicitudCuenta(
            @NotNull Long terceroId,
            @NotBlank @Size(max = 150) String correo,
            @NotBlank @Size(min = 8, max = 72) String contrasenaInicial, // BCrypt solo admite 72 bytes
            @NotNull RolUsuario rol) {
    }

    private final FachadaPoliMarket fachada;

    UsuariosController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    /** El rol no se puede cambiar: si llega en el cuerpo se ignora. Para cambiarlo se crea otra cuenta. */
    record SolicitudCambioCuenta(@NotBlank @Size(max = 150) String correo) {
    }

    @Operation(summary = "Crear una cuenta con rol. Si es VENDEDOR queda con aval PENDIENTE", description = "Roles: RECURSOS_HUMANOS.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CuentaCreadaDto crear(@Valid @RequestBody SolicitudCuenta s) {
        return fachada.registrarCuenta(s.terceroId(), s.correo(), s.contrasenaInicial(), s.rol());
    }

    @Operation(summary = "Listar las cuentas, sin contraseñas", description = "Roles: RECURSOS_HUMANOS.")
    @GetMapping
    List<CuentaDto> listar() {
        return fachada.listarCuentas();
    }

    @Operation(summary = "Cambiar el correo de una cuenta (el rol no se puede cambiar)", description = "Roles: RECURSOS_HUMANOS.")
    @PatchMapping("/{id}")
    UsuarioDto actualizar(@PathVariable long id, @Valid @RequestBody SolicitudCambioCuenta s) {
        return fachada.actualizarCuenta(id, s.correo());
    }

    @Operation(summary = "Desactivar una cuenta y cerrar sus sesiones", description = "Roles: RECURSOS_HUMANOS.")
    @PostMapping("/{id}/desactivacion")
    UsuarioDto desactivar(@AuthenticationPrincipal UsuarioSesion solicitante, @PathVariable long id) {
        return fachada.desactivarCuenta(solicitante, id);
    }
}
