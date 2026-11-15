package polimarket.fachada;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import polimarket.recursoshumanos.api.AvalDto;
import polimarket.recursoshumanos.api.VendedorDto;
import polimarket.seguridad.api.UsuarioSesion;

@Tag(name = "Vendedores y aval")
@RestController
@RequestMapping("/api/vendedores")
class VendedoresController {

    record SolicitudAval(@Size(max = 500) String observacion) {
    }

    private final FachadaPoliMarket fachada;

    VendedoresController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @Operation(summary = "Listar vendedores con el estado de su aval", description = "Roles: RECURSOS_HUMANOS.")
    @GetMapping
    @PreAuthorize("hasRole('RECURSOS_HUMANOS')")
    List<VendedorDto> listar() {
        return fachada.listarVendedores();
    }

    @Operation(summary = "Consultar si un vendedor está autorizado", description = "Roles: RECURSOS_HUMANOS; el vendedor solo el suyo.")
    @GetMapping("/{id}/autorizacion")
    @PreAuthorize("hasAnyRole('RECURSOS_HUMANOS','VENDEDOR')")
    AvalDto autorizacion(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id) {
        return fachada.consultarAutorizacion(usuario, id);
    }

    @Operation(summary = "Aprobar el aval de un vendedor", description = "Roles: RECURSOS_HUMANOS.")
    @PostMapping("/{id}/aval/aprobar")
    @PreAuthorize("hasRole('RECURSOS_HUMANOS')")
    AvalDto aprobar(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id,
            @Valid @RequestBody(required = false) SolicitudAval s) {
        return fachada.aprobarAval(usuario, id, s == null ? null : s.observacion());
    }

    @Operation(summary = "Rechazar o revocar el aval de un vendedor", description = "Roles: RECURSOS_HUMANOS.")
    @PostMapping("/{id}/aval/rechazar")
    @PreAuthorize("hasRole('RECURSOS_HUMANOS')")
    AvalDto rechazar(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id,
            @Valid @RequestBody(required = false) SolicitudAval s) {
        return fachada.rechazarAval(usuario, id, s == null ? null : s.observacion());
    }
}
