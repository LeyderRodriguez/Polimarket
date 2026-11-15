package polimarket.seguridad.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import polimarket.seguridad.RolUsuario;

@Entity
@Table(schema = "seguridad", name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tercero_id", nullable = false)
    private Long terceroId;

    @Column(nullable = false)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    protected Usuario() {
    }

    public Usuario(Long terceroId, String correo, String contrasenaHash, RolUsuario rol, Instant fechaCreacion) {
        this.terceroId = terceroId;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.fechaCreacion = fechaCreacion;
    }

    public Long getId() { return id; }
    public Long getTerceroId() { return terceroId; }
    public String getCorreo() { return correo; }
    public String getContrasenaHash() { return contrasenaHash; }
    public RolUsuario getRol() { return rol; }
    public boolean isActivo() { return activo; }
}
