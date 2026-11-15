package polimarket.fachada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import polimarket.entregas.api.EntregaDto;
import polimarket.seguridad.api.UsuarioSesion;
import polimarket.ventas.api.ItemVenta;
import polimarket.ventas.api.NuevaVenta;
import polimarket.ventas.api.VentaDto;
import polimarket.ventas.api.VentaPendienteDto;
import polimarket.ventas.api.VentaResumenDto;

@RestController
@RequestMapping("/api/ventas")
class VentasController {

    record SolicitudVenta(
            @NotNull Long clienteId,
            @NotBlank String direccionEntrega,
            @NotEmpty List<@Valid ItemSolicitud> productos) {
    }

    record ItemSolicitud(@NotNull Long productoId, @Min(1) int cantidad) {
    }

    private final FachadaPoliMarket fachada;

    VentasController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDEDOR')")
    VentaDto registrar(@AuthenticationPrincipal UsuarioSesion usuario, @Valid @RequestBody SolicitudVenta s) {
        List<ItemVenta> items = s.productos().stream().map(i -> new ItemVenta(i.productoId(), i.cantidad())).toList();
        return fachada.registrarVenta(usuario, new NuevaVenta(s.clienteId(), s.direccionEntrega(), items));
    }

    /** No exige aval: un vendedor que lo perdió aún debe ver sus ventas para poder cancelarlas. */
    @GetMapping("/mis-ventas")
    @PreAuthorize("hasRole('VENDEDOR')")
    List<VentaResumenDto> misVentas(@AuthenticationPrincipal UsuarioSesion usuario) {
        return fachada.misVentas(usuario);
    }

    @PostMapping("/{id}/cancelacion")
    @PreAuthorize("hasRole('VENDEDOR')")
    VentaDto cancelar(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id) {
        return fachada.anularVenta(usuario, id);
    }

    @GetMapping("/pendientes-entrega")
    @PreAuthorize("hasRole('ENTREGAS')")
    List<VentaPendienteDto> pendientesEntrega() {
        return fachada.pendientesEntrega();
    }

    @PostMapping("/{id}/despacho")
    @PreAuthorize("hasRole('ENTREGAS')")
    EntregaDto despachar(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id) {
        return fachada.despachar(usuario, id);
    }
}
