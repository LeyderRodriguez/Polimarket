package polimarket.fachada;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import polimarket.proveedores.api.CambioProveedor;
import polimarket.proveedores.api.NuevoProveedor;
import polimarket.proveedores.api.ProveedorDto;

@Tag(name = "Proveedores")
@RestController
@RequestMapping("/api/proveedores")
@PreAuthorize("hasRole('BODEGA')")
class ProveedoresController {

    record SolicitudProveedor(@NotNull Long terceroId, @Size(max = 100) String nombreContacto,
            @Size(max = 30) String telefonoContacto) {
    }

    record SolicitudCambioProveedor(@Size(max = 100) String nombreContacto, @Size(max = 30) String telefonoContacto) {
    }

    record SolicitudAsociacion(@NotNull Long productoId) {
    }

    record Asociacion(Long proveedorId, Long productoId) {
    }

    private final FachadaPoliMarket fachada;

    ProveedoresController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @Operation(summary = "Listar proveedores", description = "Roles: BODEGA.")
    @GetMapping
    List<ProveedorDto> listar() {
        return fachada.listarProveedores();
    }

    @Operation(summary = "Registrar un proveedor a partir de un tercero", description = "Roles: BODEGA.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProveedorDto crear(@Valid @RequestBody SolicitudProveedor s) {
        return fachada.registrarProveedor(new NuevoProveedor(s.terceroId(), s.nombreContacto(), s.telefonoContacto()));
    }

    @Operation(summary = "Actualizar los datos de contacto de un proveedor", description = "Roles: BODEGA.")
    @PatchMapping("/{id}")
    ProveedorDto actualizar(@PathVariable long id, @Valid @RequestBody SolicitudCambioProveedor s) {
        return fachada.actualizarProveedor(id, new CambioProveedor(s.nombreContacto(), s.telefonoContacto()));
    }

    @Operation(summary = "Asociar un producto a un proveedor", description = "Roles: BODEGA.")
    @PostMapping("/{id}/productos")
    @ResponseStatus(HttpStatus.CREATED)
    Asociacion asociarProducto(@PathVariable long id, @Valid @RequestBody SolicitudAsociacion s) {
        fachada.asociarProductoAProveedor(id, s.productoId());
        return new Asociacion(id, s.productoId());
    }
}
