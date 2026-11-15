package polimarket.proveedores.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "proveedores", name = "proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tercero_id", nullable = false)
    private Long terceroId;

    @Column(name = "nombre_contacto")
    private String nombreContacto;

    @Column(name = "telefono_contacto")
    private String telefonoContacto;

    protected Proveedor() {
    }

    public Proveedor(Long terceroId, String nombreContacto, String telefonoContacto) {
        this.terceroId = terceroId;
        this.nombreContacto = nombreContacto;
        this.telefonoContacto = telefonoContacto;
    }

    /** Un dato nulo significa «sin cambio». */
    public void actualizar(String nombreContacto, String telefonoContacto) {
        if (nombreContacto != null) {
            this.nombreContacto = nombreContacto;
        }
        if (telefonoContacto != null) {
            this.telefonoContacto = telefonoContacto;
        }
    }

    public Long getId() { return id; }
    public Long getTerceroId() { return terceroId; }
    public String getNombreContacto() { return nombreContacto; }
    public String getTelefonoContacto() { return telefonoContacto; }
}
