package polimarket.fachada;

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

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('RECURSOS_HUMANOS')")
class UsuariosController {

    record SolicitudCuenta(
            @NotNull Long terceroId,
            @NotBlank String correo,
            @NotBlank @Size(min = 8) String contrasenaInicial,
            @NotNull RolUsuario rol) {
    }

    private final FachadaPoliMarket fachada;

    UsuariosController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    /** El rol no se puede cambiar: si llega en el cuerpo se ignora. Para cambiarlo se crea otra cuenta. */
    record SolicitudCambioCuenta(@NotBlank String correo) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CuentaCreadaDto crear(@Valid @RequestBody SolicitudCuenta s) {
        return fachada.registrarCuenta(s.terceroId(), s.correo(), s.contrasenaInicial(), s.rol());
    }

    @GetMapping
    List<CuentaDto> listar() {
        return fachada.listarCuentas();
    }

    @PatchMapping("/{id}")
    UsuarioDto actualizar(@PathVariable long id, @Valid @RequestBody SolicitudCambioCuenta s) {
        return fachada.actualizarCuenta(id, s.correo());
    }

    @PostMapping("/{id}/desactivacion")
    UsuarioDto desactivar(@AuthenticationPrincipal UsuarioSesion solicitante, @PathVariable long id) {
        return fachada.desactivarCuenta(solicitante, id);
    }
}
