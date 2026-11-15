package polimarket.ventas.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Contrato de clientes. El cliente es un tercero existente con un rol de cliente. */
public interface IClientes {

    /** Registra al tercero como cliente. Lanza CLIENTE_EXISTENTE si ya lo es. */
    ClienteDto registrarCliente(long terceroId, List<String> preferencias);

    /** Reemplaza las preferencias del cliente. */
    ClienteDto actualizarPreferencias(long clienteId, List<String> preferencias);

    List<ClienteDto> listarClientes();

    Optional<ClienteDto> consultarCliente(long clienteId);

    /** Varios clientes en una sola consulta. */
    List<ClienteDto> consultarClientes(Collection<Long> clienteIds);
}
