package polimarket.fachada;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import polimarket.seguridad.api.UsuarioSesion;
import polimarket.terceros.api.NuevoTercero;
import polimarket.terceros.api.TerceroDto;
import polimarket.terceros.api.TipoTercero;

@Tag(name = "Terceros")
@RestController
@RequestMapping("/api/terceros")
@PreAuthorize("hasAnyRole('RECURSOS_HUMANOS','VENDEDOR')")
class TercerosController {

    record SolicitudTercero(
            @NotNull TipoTercero tipo,
            @NotBlank @Size(max = 10) String tipoDocumento,
            @NotBlank @Size(max = 30) String numeroDocumento,
            @Size(max = 100) String nombres,
            @Size(max = 100) String apellidos,
            @Size(max = 150) String razonSocial,
            @Size(max = 30) String telefono,
            @Size(max = 150) String correo,
            @Size(max = 200) String direccion) {
    }

    private final FachadaPoliMarket fachada;

    TercerosController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @Operation(summary = "Buscar un tercero por documento (evita duplicados)", description = "Roles: RECURSOS_HUMANOS, VENDEDOR con aval.")
    @GetMapping
    TerceroDto buscar(@AuthenticationPrincipal UsuarioSesion usuario, @RequestParam String tipoDocumento,
            @RequestParam String numeroDocumento) {
        return fachada.buscarTercero(usuario, tipoDocumento, numeroDocumento);
    }

    @Operation(summary = "Registrar un tercero (persona o empresa)", description = "Roles: RECURSOS_HUMANOS, VENDEDOR con aval.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TerceroDto registrar(@AuthenticationPrincipal UsuarioSesion usuario, @Valid @RequestBody SolicitudTercero s) {
        return fachada.registrarTercero(usuario, new NuevoTercero(s.tipo(), s.tipoDocumento(), s.numeroDocumento(),
                s.nombres(), s.apellidos(), s.razonSocial(), s.telefono(), s.correo(), s.direccion()));
    }
}
