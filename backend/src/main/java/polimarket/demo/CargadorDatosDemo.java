package polimarket.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import polimarket.recursoshumanos.api.IGestionPersonal;
import polimarket.seguridad.api.IAutenticacion;
import polimarket.seguridad.api.NuevoUsuario;
import polimarket.seguridad.api.RolUsuario;
import polimarket.terceros.api.ITerceros;
import polimarket.terceros.api.NuevoTercero;
import polimarket.terceros.api.TerceroDto;
import polimarket.terceros.api.TipoTercero;

/**
 * Datos ficticios para desarrollo y demostración. Solo corre con el perfil {@code dev} y con
 * {@code polimarket.demo.cargar-datos=true}. Si el tercero de la cuenta inicial ya existe, no hace nada:
 * reiniciar la aplicación no duplica datos ni pisa los cambios.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "polimarket.demo.cargar-datos", havingValue = "true")
class CargadorDatosDemo implements ApplicationRunner {

    private static final String DOCUMENTO_RH = "1000000001";

    private final ITerceros terceros;
    private final IAutenticacion autenticacion;
    private final IGestionPersonal personal;
    private final String clave;

    CargadorDatosDemo(ITerceros terceros, IAutenticacion autenticacion, IGestionPersonal personal,
            @Value("${polimarket.demo.clave}") String clave) {
        this.terceros = terceros;
        this.autenticacion = autenticacion;
        this.personal = personal;
        this.clave = clave;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (terceros.consultarPorDocumento("CC", DOCUMENTO_RH).isPresent()) {
            return; // ya cargado
        }
        // Cuenta inicial de Recursos Humanos: sin ella nadie podría crear las demás cuentas.
        TerceroDto rh = terceros.registrar(new NuevoTercero(
                TipoTercero.NATURAL, "CC", DOCUMENTO_RH, "Rosa", "Herrera", null, null, null, null));
        personal.registrarEmpleado(rh.id(), "Recursos Humanos");
        autenticacion.crearUsuario(new NuevoUsuario(rh.id(), "rh@polimarket.test", clave, RolUsuario.RECURSOS_HUMANOS));
    }
}
