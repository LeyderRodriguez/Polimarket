package polimarket.bodega.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(schema = "bodega", name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;
    private String categoria;

    @Column(name = "precio_venta", nullable = false)
    private BigDecimal precioVenta;

    @Column(nullable = false)
    private boolean activo = true;

    protected Producto() {
    }

    public Producto(String codigo, String nombre, String descripcion, String categoria, BigDecimal precioVenta) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.precioVenta = redondear(precioVenta);
    }

    /** Un dato nulo significa «sin cambio». */
    public void actualizar(String nombre, String descripcion, String categoria, BigDecimal precioVenta) {
        if (nombre != null) {
            this.nombre = nombre;
        }
        if (descripcion != null) {
            this.descripcion = descripcion;
        }
        if (categoria != null) {
            this.categoria = categoria;
        }
        if (precioVenta != null) {
            this.precioVenta = redondear(precioVenta);
        }
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public boolean isActivo() { return activo; }
}
