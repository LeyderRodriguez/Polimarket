package polimarket.fachada;

import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/productos")
class ProductosController {

    record SolicitudProducto(
            @NotBlank String codigo,
            @NotBlank String nombre,
            String descripcion,
            String categoria,
            @NotNull @DecimalMin("0") BigDecimal precioVenta,
            @Min(0) int stockMinimo) {
    }

    record SolicitudCambioProducto(
            String nombre,
            String descripcion,
            String categoria,
            @DecimalMin("0") BigDecimal precioVenta,
            @Min(0) Integer stockMinimo) {
    }

    private final FachadaPoliMarket fachada;

    ProductosController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BODEGA','VENDEDOR')")
    List<DisponibilidadDto> listar(@AuthenticationPrincipal UsuarioSesion usuario) {
        return fachada.listarDisponibilidad(usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BODEGA')")
    ProductoDto crear(@Valid @RequestBody SolicitudProducto s) {
        return fachada.crearProducto(new NuevoProducto(s.codigo(), s.nombre(), s.descripcion(), s.categoria(),
                s.precioVenta(), s.stockMinimo()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BODEGA')")
    ProductoDto actualizar(@PathVariable long id, @Valid @RequestBody SolicitudCambioProducto s) {
        return fachada.actualizarProducto(id, new CambioProducto(s.nombre(), s.descripcion(), s.categoria(),
                s.precioVenta(), s.stockMinimo()));
    }
}
