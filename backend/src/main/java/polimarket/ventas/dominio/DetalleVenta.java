package polimarket.ventas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(schema = "ventas", name = "detalle_venta")
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    protected DetalleVenta() {
    }

    public DetalleVenta(Long ventaId, Long productoId, int cantidad, BigDecimal precioUnitario) {
        this.ventaId = ventaId;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getVentaId() { return ventaId; }
    public Long getProductoId() { return productoId; }
    public int getCantidad() { return cantidad; }
}
