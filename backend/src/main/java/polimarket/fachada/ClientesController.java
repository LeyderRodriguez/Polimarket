package polimarket.fachada;

import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasRole('VENDEDOR')")
class ClientesController {

    record SolicitudCliente(@NotNull Long terceroId, List<String> preferencias) {
    }

    record SolicitudCambioCliente(List<String> preferencias, String telefono, String correo, String direccion) {
    }

    private final FachadaPoliMarket fachada;

    ClientesController(FachadaPoliMarket fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    List<ClienteDto> listar(@AuthenticationPrincipal UsuarioSesion usuario) {
        return fachada.listarClientes(usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClienteDto registrar(@AuthenticationPrincipal UsuarioSesion usuario, @Valid @RequestBody SolicitudCliente s) {
        return fachada.registrarCliente(usuario, s.terceroId(), s.preferencias());
    }

    @PatchMapping("/{id}")
    ClienteDto actualizar(@AuthenticationPrincipal UsuarioSesion usuario, @PathVariable long id,
            @RequestBody SolicitudCambioCliente s) {
        return fachada.actualizarCliente(usuario, id, s.preferencias(), s.telefono(), s.correo(), s.direccion());
    }
}
