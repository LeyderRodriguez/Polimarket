package polimarket.ventas.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.terceros.api.ITerceros;
import polimarket.terceros.api.TerceroDto;
import polimarket.terceros.api.TipoRolTercero;
import polimarket.ventas.api.ClienteDto;
import polimarket.ventas.api.IClientes;
import polimarket.ventas.dominio.Cliente;
import polimarket.ventas.dominio.ClienteRepositorio;

@Service
@Transactional
class ClientesService implements IClientes {

    private final ClienteRepositorio clientes;
    private final ITerceros terceros;
    private final Clock reloj;

    ClientesService(ClienteRepositorio clientes, ITerceros terceros, Clock reloj) {
        this.clientes = clientes;
        this.terceros = terceros;
        this.reloj = reloj;
    }

    @Override
    public ClienteDto registrarCliente(long terceroId, List<String> preferencias) {
        if (terceros.consultarPorId(terceroId).isEmpty()) {
            throw ExcepcionNegocio.noEncontrado("No se encontró el tercero.");
        }
        if (clientes.existsByTerceroId(terceroId)) {
            throw ExcepcionNegocio.conflicto("CLIENTE_EXISTENTE", "Ese tercero ya está registrado como cliente.");
        }
        Cliente c = clientes.save(new Cliente(terceroId, LocalDate.now(reloj), preferencias));
        terceros.asignarRol(terceroId, TipoRolTercero.CLIENTE);
        return aDtos(List.of(c)).get(0);
    }

    @Override
    public ClienteDto actualizarPreferencias(long clienteId, List<String> preferencias) {
        Cliente c = clientes.findById(clienteId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró el cliente."));
        c.cambiarPreferencias(preferencias);
        return aDtos(List.of(c)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteDto> listarClientes() {
        return aDtos(clientes.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteDto> consultarCliente(long clienteId) {
        return clientes.findById(clienteId).map(c -> aDtos(List.of(c)).get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteDto> consultarClientes(Collection<Long> clienteIds) {
        return aDtos(clientes.findAllById(clienteIds));
    }

    private List<ClienteDto> aDtos(List<Cliente> lista) {
        Map<Long, TerceroDto> porId = terceros.consultarPorIds(lista.stream().map(Cliente::getTerceroId).toList())
                .stream().collect(Collectors.toMap(TerceroDto::id, Function.identity()));
        return lista.stream()
                .map(c -> new ClienteDto(c.getId(), c.getTerceroId(), porId.get(c.getTerceroId()).nombreCompleto(),
                        c.getPreferencias()))
                .toList();
    }
}
