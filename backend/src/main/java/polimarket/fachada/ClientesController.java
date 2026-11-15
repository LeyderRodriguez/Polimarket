package polimarket.fachada;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
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
import polimarket.seguridad.api.UsuarioSesion;
import polimarket.ventas.api.ClienteDto;

@Tag(name = "Clientes")
@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasRole('VENDEDOR')")
class ClientesController {

    record SolicitudCliente(@NotNull Long terceroId, @Size(max = 20) List<@Size(max = 80) String> preferencias) {
    }

    record SolicitudCambioCliente(@Size(max = 20) List<@Size(max = 80) String> preferencias, @Size(max = 30) String telefono,
            @Size(max = 150) String correo, @Size(max = 200) String direccion) {
    }

    private final FachadaPoliMarket fachada;

    ClientesController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @Operation(summary = "Listar clientes", description = "Roles: VENDEDOR con aval.")
    @GetMapping
    List<ClienteDto> listar(@AuthenticationPrincipal UsuarioSesion usuario) {
        return fachada.listarClientes(usuario);
    }

    @Operation(summary = "Registrar un cliente a partir de un tercero", description = "Roles: VENDEDOR con aval.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClienteDto registrar(@AuthenticationPrincipal UsuarioSesion usuario, @Valid @RequestBody SolicitudCliente s) {
        return fachada.registrarCliente(usuario, s.terceroId(), s.preferencias());
    }

    @Operation(summary = "Actualizar preferencias y datos de contacto de un cliente", description = "Roles: VENDEDOR con aval.")
    @PatchMapping("/{id}")
    ClienteDto actualizar(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id,
            @Valid @RequestBody SolicitudCambioCliente s) {
        return fachada.actualizarCliente(usuario, id, s.preferencias(), s.telefono(), s.correo(), s.direccion());
    }
}
