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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import polimarket.bodega.api.CambioProducto;
import polimarket.bodega.api.DisponibilidadDto;
import polimarket.bodega.api.NuevoProducto;
import polimarket.bodega.api.ProductoDto;
import polimarket.seguridad.api.UsuarioSesion;

@Tag(name = "Productos")
@RestController
@RequestMapping("/api/productos")
class ProductosController {

    record SolicitudProducto(
            @NotBlank @Size(max = 30) String codigo,
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 300) String descripcion,
            @Size(max = 60) String categoria,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal precioVenta,
            @Min(0) @Max(1_000_000) int stockMinimo) {
    }

    record SolicitudCambioProducto(
            @Size(max = 120) String nombre,
            @Size(max = 300) String descripcion,
            @Size(max = 60) String categoria,
            @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal precioVenta,
            @Min(0) @Max(1_000_000) Integer stockMinimo) {
    }

    private final FachadaPoliMarket fachada;

    ProductosController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @Operation(summary = "Productos con stock físico, reservado y disponible", description = "Roles: BODEGA, VENDEDOR con aval.")
    @GetMapping
    @PreAuthorize("hasAnyRole('BODEGA','VENDEDOR')")
    List<DisponibilidadDto> listar(@AuthenticationPrincipal UsuarioSesion usuario) {
        return fachada.listarDisponibilidad(usuario);
    }

    @Operation(summary = "Crear un producto", description = "Roles: BODEGA.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BODEGA')")
    ProductoDto crear(@Valid @RequestBody SolicitudProducto s) {
        return fachada.crearProducto(new NuevoProducto(s.codigo(), s.nombre(), s.descripcion(), s.categoria(),
                s.precioVenta(), s.stockMinimo()));
    }

    @Operation(summary = "Actualizar un producto (solo los campos que se envían)", description = "Roles: BODEGA.")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BODEGA')")
    ProductoDto actualizar(@PathVariable long id, @Valid @RequestBody SolicitudCambioProducto s) {
        return fachada.actualizarProducto(id, new CambioProducto(s.nombre(), s.descripcion(), s.categoria(),
                s.precioVenta(), s.stockMinimo()));
    }
}
