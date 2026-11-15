package polimarket.proveedores.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrato del componente Proveedores. El producto se identifica por su código, así este
 * componente no depende de Bodega.
 */
public interface IProveedores {

    /** Registra al tercero como proveedor. Lanza PROVEEDOR_EXISTENTE si ya lo es. */
    ProveedorDto registrar(NuevoProveedor proveedor);

    ProveedorDto actualizar(long proveedorId, CambioProveedor cambio);

    List<ProveedorDto> listar();

    Optional<ProveedorDto> consultarPorId(long proveedorId);

    /** Asocia un producto (por código) a un proveedor. Lanza PRODUCTO_YA_ASOCIADO si ya estaba. */
    void asociarProducto(long proveedorId, String codigoProducto, BigDecimal precioCompra);

    /** Proveedores que suministran el producto con ese código. */
    List<ProveedorDto> consultarPorProducto(String codigoProducto);

    /** Proveedores de varios productos en pocas consultas: código de producto → sus proveedores. */
    Map<String, List<ProveedorDto>> consultarPorProductos(Collection<String> codigosProducto);
}
