package polimarket.recursoshumanos.api;

import java.util.List;
import java.util.Optional;

/** Contrato del personal de la empresa: empleados y vendedores. */
public interface IGestionPersonal {

    /** Registra al tercero como empleado. Si ya lo es, devuelve el existente. */
    EmpleadoDto registrarEmpleado(long terceroId, String cargo);

    /** Registra al tercero como vendedor con aval PENDIENTE. No aprueba nada. */
    VendedorDto registrarVendedor(long terceroId, String zona);

    List<VendedorDto> listarVendedores();

    Optional<VendedorDto> consultarVendedor(long vendedorId);

    Optional<VendedorDto> consultarVendedorPorTercero(long terceroId);
}
