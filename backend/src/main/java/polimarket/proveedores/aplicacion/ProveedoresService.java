package polimarket.proveedores.aplicacion;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.proveedores.api.CambioProveedor;
import polimarket.proveedores.api.IProveedores;
import polimarket.proveedores.api.NuevoProveedor;
import polimarket.proveedores.api.ProveedorDto;
import polimarket.proveedores.dominio.ProductoSuministrado;
import polimarket.proveedores.dominio.ProductoSuministradoRepositorio;
import polimarket.proveedores.dominio.Proveedor;
import polimarket.proveedores.dominio.ProveedorRepositorio;
import polimarket.terceros.api.ITerceros;
import polimarket.terceros.api.TerceroDto;
import polimarket.terceros.api.TipoRolTercero;

@Service
@Transactional
class ProveedoresService implements IProveedores {

    private final ProveedorRepositorio proveedores;
    private final ProductoSuministradoRepositorio suministrados;
    private final ITerceros terceros;

    ProveedoresService(ProveedorRepositorio proveedores, ProductoSuministradoRepositorio suministrados,
            ITerceros terceros) {
        this.proveedores = proveedores;
        this.suministrados = suministrados;
        this.terceros = terceros;
    }

    @Override
    public ProveedorDto registrar(NuevoProveedor n) {
        if (terceros.consultarPorId(n.terceroId()).isEmpty()) {
            throw ExcepcionNegocio.noEncontrado("No se encontró el tercero.");
        }
        if (proveedores.existsByTerceroId(n.terceroId())) {
            throw ExcepcionNegocio.conflicto("PROVEEDOR_EXISTENTE", "Ese tercero ya está registrado como proveedor.");
        }
        Proveedor p = proveedores.save(new Proveedor(n.terceroId(), limpiar(n.nombreContacto()),
                limpiar(n.telefonoContacto())));
        terceros.asignarRol(n.terceroId(), TipoRolTercero.PROVEEDOR);
        return aDtos(List.of(p)).get(0);
    }

    @Override
    public ProveedorDto actualizar(long proveedorId, CambioProveedor cambio) {
        Proveedor p = exigir(proveedorId);
        p.actualizar(limpiar(cambio.nombreContacto()), limpiar(cambio.telefonoContacto()));
        return aDtos(List.of(p)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDto> listar() {
        return aDtos(proveedores.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProveedorDto> consultarPorId(long proveedorId) {
        return proveedores.findById(proveedorId).map(p -> aDtos(List.of(p)).get(0));
    }

    @Override
    public void asociarProducto(long proveedorId, String codigoProducto, BigDecimal precioCompra) {
        exigir(proveedorId);
        if (codigoProducto == null || codigoProducto.isBlank()) {
            throw ExcepcionNegocio.datosInvalidos("El código del producto es obligatorio.");
        }
        String codigo = codigoProducto.trim().toUpperCase();
        if (suministrados.existsByProveedorIdAndCodigoProducto(proveedorId, codigo)) {
            throw ExcepcionNegocio.conflicto("PRODUCTO_YA_ASOCIADO", "Ese producto ya está asociado al proveedor.");
        }
        suministrados.save(new ProductoSuministrado(proveedorId, codigo, precioCompra));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDto> consultarPorProducto(String codigoProducto) {
        if (codigoProducto == null) {
            return List.of();
        }
        List<Long> ids = suministrados.findByCodigoProducto(codigoProducto.trim().toUpperCase()).stream()
                .map(ProductoSuministrado::getProveedorId).toList();
        return aDtos(proveedores.findAllById(ids));
    }

    private Proveedor exigir(long proveedorId) {
        return proveedores.findById(proveedorId)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("No se encontró el proveedor."));
    }

    /** Arma los DTO con una sola consulta de nombres a Terceros. */
    private List<ProveedorDto> aDtos(Collection<Proveedor> lista) {
        Map<Long, TerceroDto> porId = terceros.consultarPorIds(lista.stream().map(Proveedor::getTerceroId).toList())
                .stream().collect(Collectors.toMap(TerceroDto::id, Function.identity()));
        return lista.stream()
                .map(p -> new ProveedorDto(p.getId(), p.getTerceroId(), porId.get(p.getTerceroId()).nombreCompleto(),
                        p.getNombreContacto(), p.getTelefonoContacto()))
                .toList();
    }

    private static String limpiar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
