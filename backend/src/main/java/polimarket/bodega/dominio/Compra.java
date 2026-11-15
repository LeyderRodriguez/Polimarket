package polimarket.bodega.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Compra a un proveedor que origina una entrada de inventario. */
@Entity
@Table(schema = "bodega", name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bodega_id", nullable = false)
    private Long bodegaId;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(nullable = false)
    private Instant fecha;

    @Column(name = "numero_factura")
    private String numeroFactura;

    @Column(nullable = false)
    private BigDecimal total;

    protected Compra() {
    }

    public Compra(Long bodegaId, Long proveedorId, Instant fecha, String numeroFactura, BigDecimal total) {
        this.bodegaId = bodegaId;
        this.proveedorId = proveedorId;
        this.fecha = fecha;
        this.numeroFactura = numeroFactura;
        this.total = total;
    }

    public Long getId() { return id; }
}
