package polimarket.api;

import jakarta.validation.Valid;
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
import polimarket.seguridad.UsuarioSesion;
import polimarket.terceros.NuevoTercero;
import polimarket.terceros.TerceroDto;
import polimarket.terceros.TipoTercero;

@RestController
@RequestMapping("/api/terceros")
@PreAuthorize("hasAnyRole('RECURSOS_HUMANOS','VENDEDOR')")
class TercerosController {

    record SolicitudTercero(
            @NotNull TipoTercero tipo,
            @NotBlank String tipoDocumento,
            @NotBlank String numeroDocumento,
            String nombres,
            String apellidos,
            String razonSocial,
            String telefono,
            String correo,
            String direccion) {
    }

    private final FachadaPoliMarket fachada;

    TercerosController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    TerceroDto buscar(@AuthenticationPrincipal UsuarioSesion usuario, @RequestParam String tipoDocumento,
            @RequestParam String numeroDocumento) {
        return fachada.buscarTercero(usuario, tipoDocumento, numeroDocumento);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TerceroDto registrar(@AuthenticationPrincipal UsuarioSesion usuario, @Valid @RequestBody SolicitudTercero s) {
        return fachada.registrarTercero(usuario, new NuevoTercero(s.tipo(), s.tipoDocumento(), s.numeroDocumento(),
                s.nombres(), s.apellidos(), s.razonSocial(), s.telefono(), s.correo(), s.direccion()));
    }
}
