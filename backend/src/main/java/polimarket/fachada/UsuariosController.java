package polimarket.fachada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import polimarket.seguridad.api.RolUsuario;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CuentaCreadaDto crear(@Valid @RequestBody SolicitudCuenta s) {
        return fachada.registrarCuenta(s.terceroId(), s.correo(), s.contrasenaInicial(), s.rol());
    }
}
