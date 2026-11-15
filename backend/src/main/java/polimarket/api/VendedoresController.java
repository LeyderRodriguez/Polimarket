package polimarket.api;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import polimarket.recursoshumanos.AvalDto;
import polimarket.recursoshumanos.VendedorDto;
import polimarket.seguridad.UsuarioSesion;

@RestController
@RequestMapping("/api/vendedores")
class VendedoresController {

    record SolicitudAval(String observacion) {
    }

    private final FachadaPoliMarket fachada;

    VendedoresController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    @PreAuthorize("hasRole('RECURSOS_HUMANOS')")
    List<VendedorDto> listar() {
        return fachada.listarVendedores();
    }

    @GetMapping("/{id}/autorizacion")
    @PreAuthorize("hasAnyRole('RECURSOS_HUMANOS','VENDEDOR')")
    AvalDto autorizacion(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id) {
        return fachada.consultarAutorizacion(usuario, id);
    }

    @PostMapping("/{id}/aval/aprobar")
    @PreAuthorize("hasRole('RECURSOS_HUMANOS')")
    AvalDto aprobar(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id,
            @RequestBody(required = false) SolicitudAval s) {
        return fachada.aprobarAval(usuario, id, s == null ? null : s.observacion());
    }

    @PostMapping("/{id}/aval/rechazar")
    @PreAuthorize("hasRole('RECURSOS_HUMANOS')")
    AvalDto rechazar(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id,
            @RequestBody(required = false) SolicitudAval s) {
        return fachada.rechazarAval(usuario, id, s == null ? null : s.observacion());
    }
}
