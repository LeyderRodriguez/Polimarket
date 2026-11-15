package polimarket.fachada;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import polimarket.bodega.api.BajoStockDto;
import polimarket.bodega.api.MovimientoDto;
import polimarket.bodega.api.NuevaEntrada;
import polimarket.bodega.api.NuevoAjuste;
import polimarket.seguridad.api.UsuarioSesion;

@Tag(name = "Inventario")
@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("hasRole('BODEGA')")
class InventarioController {

    record SolicitudEntrada(
            @NotNull Long productoId,
            @Min(1) @Max(1_000_000) int cantidad,
            @NotNull Long proveedorId,
            @Size(max = 60) String referencia,
            @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal costoUnitario) {
    }

    record SolicitudAjuste(@NotNull Long productoId, @Min(0) @Max(1_000_000) int nuevoStockFisico,
            @NotBlank @Size(max = 300) String motivo) {
    }

    private final FachadaPoliMarket fachada;

    InventarioController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @Operation(summary = "Productos cuyo disponible está bajo el mínimo, con sus proveedores", description = "Roles: BODEGA.")
    @GetMapping("/bajo-stock")
    List<BajoStockDto> bajoStock() {
        return fachada.listarBajoStock();
    }

    @Operation(summary = "Registrar una entrada de mercancía de un proveedor", description = "Roles: BODEGA.")
    @PostMapping("/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    MovimientoDto entrada(@AuthenticationPrincipal UsuarioSesion usuario, @Valid @RequestBody SolicitudEntrada s) {
        return fachada.registrarEntrada(usuario, new NuevaEntrada(s.productoId(), s.cantidad(), s.proveedorId(),
                s.referencia(), s.costoUnitario()));
    }

    @Operation(summary = "Ajustar el stock físico con motivo", description = "Roles: BODEGA.")
    @PostMapping("/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    MovimientoDto ajuste(@AuthenticationPrincipal UsuarioSesion usuario, @Valid @RequestBody SolicitudAjuste s) {
        return fachada.registrarAjuste(usuario, new NuevoAjuste(s.productoId(), s.nuevoStockFisico(), s.motivo()));
    }
}
