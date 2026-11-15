package polimarket.proveedores.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(schema = "proveedores", name = "producto_suministrado")
public class ProductoSuministrado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "codigo_producto", nullable = false)
    private String codigoProducto;

    @Column(name = "precio_compra")
    private BigDecimal precioCompra;

    protected ProductoSuministrado() {
    }

    public ProductoSuministrado(Long proveedorId, String codigoProducto, BigDecimal precioCompra) {
        this.proveedorId = proveedorId;
        this.codigoProducto = codigoProducto;
        this.precioCompra = precioCompra;
    }

    public Long getProveedorId() { return proveedorId; }
    public String getCodigoProducto() { return codigoProducto; }
}
