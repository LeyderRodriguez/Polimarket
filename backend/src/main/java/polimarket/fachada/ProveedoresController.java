package polimarket.fachada;

import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/proveedores")
@PreAuthorize("hasRole('BODEGA')")
class ProveedoresController {

    record SolicitudProveedor(@NotNull Long terceroId, String nombreContacto, String telefonoContacto) {
    }

    record SolicitudCambioProveedor(String nombreContacto, String telefonoContacto) {
    }

    record SolicitudAsociacion(@NotNull Long productoId) {
    }

    record Asociacion(Long proveedorId, Long productoId) {
    }

    private final FachadaPoliMarket fachada;

    ProveedoresController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    List<ProveedorDto> listar() {
        return fachada.listarProveedores();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProveedorDto crear(@Valid @RequestBody SolicitudProveedor s) {
        return fachada.registrarProveedor(new NuevoProveedor(s.terceroId(), s.nombreContacto(), s.telefonoContacto()));
    }

    @PatchMapping("/{id}")
    ProveedorDto actualizar(@PathVariable long id, @RequestBody SolicitudCambioProveedor s) {
        return fachada.actualizarProveedor(id, new CambioProveedor(s.nombreContacto(), s.telefonoContacto()));
    }

    @PostMapping("/{id}/productos")
    @ResponseStatus(HttpStatus.CREATED)
    Asociacion asociarProducto(@PathVariable long id, @Valid @RequestBody SolicitudAsociacion s) {
        fachada.asociarProductoAProveedor(id, s.productoId());
        return new Asociacion(id, s.productoId());
    }
}
