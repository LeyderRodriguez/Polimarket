package polimarket.recursoshumanos.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.recursoshumanos.api.EmpleadoDto;
import polimarket.recursoshumanos.api.EstadoAval;
import polimarket.recursoshumanos.api.IGestionPersonal;
import polimarket.recursoshumanos.api.VendedorDto;
import polimarket.recursoshumanos.dominio.AvalRH;
import polimarket.recursoshumanos.dominio.AvalRepositorio;
import polimarket.recursoshumanos.dominio.Empleado;
import polimarket.recursoshumanos.dominio.EmpleadoRepositorio;
import polimarket.recursoshumanos.dominio.Vendedor;
import polimarket.recursoshumanos.dominio.VendedorRepositorio;
import polimarket.terceros.api.ITerceros;
import polimarket.terceros.api.TerceroDto;
import polimarket.terceros.api.TipoRolTercero;

@Service
@Transactional
class GestionPersonalService implements IGestionPersonal {

    private final ITerceros terceros;
    private final EmpleadoRepositorio empleados;
    private final VendedorRepositorio vendedores;
    private final AvalRepositorio avales;
    private final Clock reloj;

    GestionPersonalService(ITerceros terceros, EmpleadoRepositorio empleados, VendedorRepositorio vendedores,
            AvalRepositorio avales, Clock reloj) {
        this.terceros = terceros;
        this.empleados = empleados;
        this.vendedores = vendedores;
        this.avales = avales;
        this.reloj = reloj;
    }

    @Override
    public EmpleadoDto registrarEmpleado(long terceroId, String cargo) {
        return aDto(empleadoDe(terceroId, cargo));
    }

    @Override
    public VendedorDto registrarVendedor(long terceroId, String zona) {
        Empleado empleado = empleadoDe(terceroId, "Vendedor");
        if (vendedores.existsByEmpleadoId(empleado.getId())) {
            throw ExcepcionNegocio.conflicto("VENDEDOR_EXISTENTE", "Ese tercero ya está registrado como vendedor.");
        }
        Vendedor vendedor = vendedores.save(new Vendedor(empleado.getId(), zona));
        avales.save(AvalRH.solicitar(vendedor.getId(), Instant.now(reloj)));
        terceros.asignarRol(terceroId, TipoRolTercero.VENDEDOR);
        return aDto(vendedor, empleado, terceros.consultarPorId(terceroId).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendedorDto> listarVendedores() {
        List<Vendedor> todos = vendedores.findAll();
        Map<Long, Empleado> empleadosPorId = empleados.findAllById(todos.stream().map(Vendedor::getEmpleadoId).toList())
                .stream().collect(Collectors.toMap(Empleado::getId, Function.identity()));
        Map<Long, TerceroDto> tercerosPorId = terceros
                .consultarPorIds(empleadosPorId.values().stream().map(Empleado::getTerceroId).toList())
                .stream().collect(Collectors.toMap(TerceroDto::id, Function.identity()));
        return todos.stream()
                .map(v -> {
                    Empleado e = empleadosPorId.get(v.getEmpleadoId());
                    return aDto(v, e, tercerosPorId.get(e.getTerceroId()));
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VendedorDto> consultarVendedor(long vendedorId) {
        return vendedores.findById(vendedorId).map(this::completar);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VendedorDto> consultarVendedorPorTercero(long terceroId) {
        return empleados.findByTerceroId(terceroId)
                .flatMap(e -> vendedores.findByEmpleadoId(e.getId()))
                .map(this::completar);
    }

    private Empleado empleadoDe(long terceroId, String cargo) {
        if (terceros.consultarPorId(terceroId).isEmpty()) {
            throw ExcepcionNegocio.noEncontrado("No se encontró el tercero.");
        }
        Empleado empleado = empleados.findByTerceroId(terceroId)
                .orElseGet(() -> empleados.save(new Empleado(terceroId, cargo, LocalDate.now(reloj))));
        terceros.asignarRol(terceroId, TipoRolTercero.EMPLEADO);
        return empleado;
    }

    private VendedorDto completar(Vendedor v) {
        Empleado e = empleados.findById(v.getEmpleadoId()).orElseThrow();
        return aDto(v, e, terceros.consultarPorId(e.getTerceroId()).orElseThrow());
    }

    private VendedorDto aDto(Vendedor v, Empleado e, TerceroDto t) {
        EstadoAval estado = avales.findFirstByVendedorIdOrderByIdDesc(v.getId())
                .map(AvalRH::getEstado).orElse(EstadoAval.PENDIENTE);
        return new VendedorDto(v.getId(), e.getTerceroId(), t.nombreCompleto(), v.getCodigo(), v.getZona(), estado);
    }

    private static EmpleadoDto aDto(Empleado e) {
        return new EmpleadoDto(e.getId(), e.getTerceroId(), e.getCargo(), e.getEstado());
    }
}
