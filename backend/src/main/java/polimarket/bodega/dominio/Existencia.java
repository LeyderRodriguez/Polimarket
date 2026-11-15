package polimarket.bodega.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import polimarket.compartido.ExcepcionNegocio;

/**
 * Existencia de un producto en una bodega. {@code cantidad} es el stock físico; lo reservado por
 * ventas pendientes se recibe como parámetro porque vive en las reservas.
 */
@Entity
@Table(schema = "bodega", name = "existencia")
public class Existencia {

    /** El prototipo tiene una sola bodega. */
    public static final long BODEGA_PRINCIPAL = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bodega_id", nullable = false)
    private Long bodegaId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;

    protected Existencia() {
    }

    public Existencia(Long bodegaId, Long productoId, int cantidad, int stockMinimo) {
        this.bodegaId = bodegaId;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.stockMinimo = stockMinimo;
    }

    public int disponible(int reservado) {
        return cantidad - reservado;
    }

    public boolean hayDisponibilidad(int solicitado, int reservado) {
        return disponible(reservado) >= solicitado;
    }

    /** Bajo stock se mide sobre lo disponible para vender, no sobre el físico. */
    public boolean bajoStock(int reservado) {
        return disponible(reservado) < stockMinimo;
    }

    public void registrarEntrada(int unidades) {
        if (unidades <= 0) {
            throw ExcepcionNegocio.datosInvalidos("La cantidad de una entrada debe ser mayor que cero.");
        }
        cantidad += unidades;
    }

    /** Fija el stock físico. No puede ser negativo ni quedar por debajo de las reservas activas. */
    public void ajustarA(int nuevaCantidad, int reservado) {
        if (nuevaCantidad < 0) {
            throw ExcepcionNegocio.datosInvalidos("El stock físico no puede ser negativo.");
        }
        if (nuevaCantidad < reservado) {
            throw ExcepcionNegocio.conflicto("STOCK_INSUFICIENTE",
                    "El ajuste dejaría el stock físico (" + nuevaCantidad + ") por debajo de las unidades reservadas ("
                            + reservado + ").");
        }
        cantidad = nuevaCantidad;
    }

    public void cambiarStockMinimo(int nuevoMinimo) {
        if (nuevoMinimo < 0) {
            throw ExcepcionNegocio.datosInvalidos("El stock mínimo no puede ser negativo.");
        }
        stockMinimo = nuevoMinimo;
    }

    public Long getProductoId() { return productoId; }
    public int getCantidad() { return cantidad; }
    public int getStockMinimo() { return stockMinimo; }
}
