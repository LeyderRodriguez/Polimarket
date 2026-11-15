package polimarket.bodega.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import polimarket.bodega.api.EstadoReserva;
import polimarket.compartido.ExcepcionNegocio;

/** Reserva de unidades para una venta. venta_id referencia a ventas.venta por identificador. */
@Entity
@Table(schema = "bodega", name = "reserva_inventario")
public class ReservaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado = EstadoReserva.ACTIVA;

    protected ReservaInventario() {
    }

    public ReservaInventario(Long ventaId, Instant fecha) {
        this.ventaId = ventaId;
        this.fecha = fecha;
    }

    /** Pasa a CONSUMIDA al despachar: las unidades salen físicamente de la bodega. */
    public void consumir() {
        exigirActiva();
        estado = EstadoReserva.CONSUMIDA;
    }

    /** Pasa a LIBERADA al anular la venta: las unidades vuelven a estar disponibles. */
    public void liberar() {
        exigirActiva();
        estado = EstadoReserva.LIBERADA;
    }

    private void exigirActiva() {
        if (estado != EstadoReserva.ACTIVA) {
            throw ExcepcionNegocio.conflicto("ESTADO_INVALIDO", "La reserva ya está " + estado + ".");
        }
    }

    public Long getId() { return id; }
    public Long getVentaId() { return ventaId; }
    public EstadoReserva getEstado() { return estado; }
}
