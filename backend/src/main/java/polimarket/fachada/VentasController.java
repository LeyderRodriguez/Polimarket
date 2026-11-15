package polimarket.fachada;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
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

@Tag(name = "Ventas y entregas")
@RestController
@RequestMapping("/api/ventas")
class VentasController {

    record SolicitudVenta(
            @NotNull Long clienteId,
            @NotBlank @Size(max = 200) String direccionEntrega,
            @NotEmpty @Size(max = 100) List<@Valid ItemSolicitud> productos) {
    }

    record ItemSolicitud(@NotNull Long productoId, @Min(1) @Max(100_000) int cantidad) {
    }

    private final FachadaPoliMarket fachada;

    VentasController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @Operation(summary = "Registrar una venta: valida el aval y reserva el stock (todo o nada)", description = "Roles: VENDEDOR con aval.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDEDOR')")
    VentaDto registrar(@AuthenticationPrincipal UsuarioSesion usuario, @Valid @RequestBody SolicitudVenta s) {
        List<ItemVenta> items = s.productos().stream().map(i -> new ItemVenta(i.productoId(), i.cantidad())).toList();
        return fachada.registrarVenta(usuario, new NuevaVenta(s.clienteId(), s.direccionEntrega(), items));
    }

    /** No exige aval: un vendedor que lo perdió aún debe ver sus ventas para poder cancelarlas. */
    @Operation(summary = "Listar mis ventas", description = "Roles: VENDEDOR (no exige aval).")
    @GetMapping("/mis-ventas")
    @PreAuthorize("hasRole('VENDEDOR')")
    List<VentaResumenDto> misVentas(@AuthenticationPrincipal UsuarioSesion usuario) {
        return fachada.misVentas(usuario);
    }

    @Operation(summary = "Anular una venta propia pendiente y liberar su reserva", description = "Roles: VENDEDOR dueño (no exige aval).")
    @PostMapping("/{id}/cancelacion")
    @PreAuthorize("hasRole('VENDEDOR')")
    VentaDto cancelar(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id) {
        return fachada.anularVenta(usuario, id);
    }

    @Operation(summary = "Ventas pendientes de entrega: qué, a quién y dónde", description = "Roles: ENTREGAS.")
    @GetMapping("/pendientes-entrega")
    @PreAuthorize("hasRole('ENTREGAS')")
    List<VentaPendienteDto> pendientesEntrega() {
        return fachada.pendientesEntrega();
    }

    @Operation(summary = "Despachar una venta una sola vez: consume la reserva y descuenta el stock", description = "Roles: ENTREGAS.")
    @PostMapping("/{id}/despacho")
    @PreAuthorize("hasRole('ENTREGAS')")
    EntregaDto despachar(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id) {
        return fachada.despachar(usuario, id);
    }
}
