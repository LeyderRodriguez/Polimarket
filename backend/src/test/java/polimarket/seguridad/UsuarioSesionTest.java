package polimarket.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import polimarket.seguridad.api.RolUsuario;
import polimarket.seguridad.api.UsuarioSesion;

class UsuarioSesionTest {

    @Test
    void despuesDeAutenticarElHashNoSeQuedaEnLaSesion() {
        UsuarioSesion usuario = new UsuarioSesion(1L, 2L, "ana@prueba.test", "$2a$10$hash", RolUsuario.VENDEDOR, true);

        usuario.eraseCredentials(); // Spring Security lo llama tras autenticar

        assertThat(usuario.getPassword()).isNull();
        assertThat(usuario.getUsername()).isEqualTo("ana@prueba.test"); // el resto de la identidad se conserva
    }

    @Test
    void elRolSeConvierteEnAutoridadConPrefijoROLE() {
        UsuarioSesion usuario = new UsuarioSesion(1L, 2L, "ana@prueba.test", "x", RolUsuario.BODEGA, true);

        assertThat(usuario.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_BODEGA");
    }
}
