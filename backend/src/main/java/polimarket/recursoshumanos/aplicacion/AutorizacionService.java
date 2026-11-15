package polimarket.recursoshumanos.aplicacion;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.recursoshumanos.api.AvalDto;
import polimarket.recursoshumanos.api.EstadoAval;
import polimarket.recursoshumanos.api.EstadoEmpleado;
import polimarket.recursoshumanos.api.IAutorizacion;
import polimarket.recursoshumanos.dominio.AvalRH;
import polimarket.recursoshumanos.dominio.AvalRepositorio;
import polimarket.recursoshumanos.dominio.Empleado;
import polimarket.recursoshumanos.dominio.EmpleadoRepositorio;
import polimarket.recursoshumanos.dominio.VendedorRepositorio;

@Service
@Transactional
class AutorizacionService implements IAutorizacion {

    private final VendedorRepositorio vendedores;
    private final EmpleadoRepositorio empleados;
    private final AvalRepositorio avales;
    private final Clock reloj;

    AutorizacionService(VendedorRepositorio vendedores, EmpleadoRepositorio empleados, AvalRepositorio avales,
            Clock reloj) {
        this.vendedores = vendedores;
        this.empleados = empleados;
        this.avales = avales;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaAutorizado(long vendedorId) {
        return avales.findFirstByVendedorIdOrderByIdDesc(vendedorId)
                .map(a -> a.getEstado() == EstadoAval.APROBADO)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public AvalDto consultarAval(long vendedorId) {
        exigirVendedor(vendedorId);
        EstadoAval estado = avales.findFirstByVendedorIdOrderByIdDesc(vendedorId)
                .map(AvalRH::getEstado).orElse(EstadoAval.PENDIENTE);
        return new AvalDto(vendedorId, estado, estado == EstadoAval.APROBADO);
    }

    @Override
    public AvalDto aprobar(long vendedorId, long responsableTerceroId, String observacion) {
        return resolver(vendedorId, EstadoAval.APROBADO, responsableTerceroId, observacion);
    }

    @Override
    public AvalDto rechazar(long vendedorId, long responsableTerceroId, String observacion) {
        return resolver(vendedorId, EstadoAval.RECHAZADO, responsableTerceroId, observacion);
    }

    private AvalDto resolver(long vendedorId, EstadoAval destino, long responsableTerceroId, String observacion) {
        // Se bloquea al vendedor antes de leer su último aval: así dos resoluciones simultáneas no se pisan.
        vendedores.bloquear(vendedorId).orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró el vendedor."));
        Empleado responsable = empleados.findByTerceroId(responsableTerceroId)
                .filter(e -> e.getEstado() == EstadoEmpleado.ACTIVO)
                .orElseThrow(() -> ExcepcionNegocio.sinPermiso("SIN_PERMISO",
                        "El responsable debe ser un empleado activo."));
        Instant ahora = Instant.now(reloj);

        AvalRH ultimo = avales.findFirstByVendedorIdOrderByIdDesc(vendedorId)
                .orElseGet(() -> avales.save(AvalRH.solicitar(vendedorId, ahora)));
        if (ultimo.getEstado() == destino) {
            throw ExcepcionNegocio.conflicto("ESTADO_INVALIDO", "El aval ya está " + destino + ".");
        }
        // Si el último aval ya estaba resuelto, se abre uno nuevo para conservar el historial.
        AvalRH aval = ultimo.getEstado() == EstadoAval.PENDIENTE
                ? ultimo
                : avales.save(AvalRH.solicitar(vendedorId, ahora));
        aval.resolver(destino, responsable.getId(), observacion, ahora);
        return new AvalDto(vendedorId, aval.getEstado(), aval.getEstado() == EstadoAval.APROBADO);
    }

    private void exigirVendedor(long vendedorId) {
        if (!vendedores.existsById(vendedorId)) {
            throw ExcepcionNegocio.noEncontrado("No se encontró el vendedor.");
        }
    }
}
