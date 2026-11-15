package polimarket.ventas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import polimarket.compartido.ExcepcionNegocio;
import polimarket.ventas.api.EstadoVenta;

@Entity
@Table(schema = "ventas", name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "vendedor_id", nullable = false)
    private Long vendedorId;

    @Column(nullable = false)
    private Instant fecha;

    @Column(name = "direccion_entrega", nullable = false)
    private String direccionEntrega;

    @Column(nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVenta estado = EstadoVenta.PENDIENTE_ENTREGA;

    protected Venta() {
    }

    public Venta(Long clienteId, Long vendedorId, Instant fecha, String direccionEntrega, BigDecimal total) {
        this.clienteId = clienteId;
        this.vendedorId = vendedorId;
        this.fecha = fecha;
        this.direccionEntrega = direccionEntrega;
        this.total = total;
    }

    /** Solo una venta pendiente se puede anular; una despachada o ya anulada no. */
    public void anular() {
        exigirPendiente();
        estado = EstadoVenta.ANULADA;
    }

    /** Solo una venta pendiente se despacha, y una sola vez. */
    public void marcarDespachada() {
        exigirPendiente();
        estado = EstadoVenta.DESPACHADA;
    }

    private void exigirPendiente() {
        if (estado != EstadoVenta.PENDIENTE_ENTREGA) {
            throw ExcepcionNegocio.conflicto("ESTADO_INVALIDO", "La venta está " + estado + " y ya no se puede modificar.");
        }
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public Long getVendedorId() { return vendedorId; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public BigDecimal getTotal() { return total; }
    public EstadoVenta getEstado() { return estado; }
}
