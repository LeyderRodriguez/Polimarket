package polimarket.terceros.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import polimarket.terceros.TipoTercero;

@Entity
@Table(schema = "terceros", name = "tercero")
public class Tercero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTercero tipo;

    @Column(name = "tipo_documento", nullable = false)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false)
    private String numeroDocumento;

    private String nombres;
    private String apellidos;

    @Column(name = "razon_social")
    private String razonSocial;

    private String telefono;
    private String correo;
    private String direccion;

    protected Tercero() {
    }

    public Tercero(TipoTercero tipo, String tipoDocumento, String numeroDocumento, String nombres, String apellidos,
            String razonSocial, String telefono, String correo, String direccion) {
        this.tipo = tipo;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.razonSocial = razonSocial;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
    }

    public Long getId() { return id; }
    public TipoTercero getTipo() { return tipo; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getRazonSocial() { return razonSocial; }
    public String getTelefono() { return telefono; }
    public String getCorreo() { return correo; }
    public String getDireccion() { return direccion; }
}
