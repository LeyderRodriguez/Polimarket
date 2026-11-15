package polimarket.demo;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import polimarket.bodega.api.IInventario;
import polimarket.bodega.api.NuevaEntrada;
import polimarket.bodega.api.NuevoProducto;
import polimarket.bodega.api.ProductoDto;
import polimarket.proveedores.api.IProveedores;
import polimarket.proveedores.api.NuevoProveedor;
import polimarket.recursoshumanos.api.IAutorizacion;
import polimarket.recursoshumanos.api.IGestionPersonal;
import polimarket.seguridad.api.IAutenticacion;
import polimarket.seguridad.api.NuevoUsuario;
import polimarket.seguridad.api.RolUsuario;
import polimarket.seguridad.api.UsuarioDto;
import polimarket.terceros.api.ITerceros;
import polimarket.terceros.api.NuevoTercero;
import polimarket.terceros.api.TerceroDto;
import polimarket.terceros.api.TipoTercero;
import polimarket.ventas.api.IClientes;

/**
 * Datos ficticios para desarrollo y demostración. Solo corre con el perfil {@code dev} y con
 * {@code polimarket.demo.cargar-datos=true}. Si el tercero de la cuenta inicial ya existe, no hace nada:
 * reiniciar la aplicación no duplica datos ni pisa los cambios.
 *
 * <p>Usa solo las interfaces de los componentes, igual que lo haría cualquier otro consumidor.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "polimarket.demo.cargar-datos", havingValue = "true")
class CargadorDatosDemo implements ApplicationRunner {

    private static final String DOCUMENTO_RH = "1000000001";

    private final ITerceros terceros;
    private final IAutenticacion autenticacion;
    private final IGestionPersonal personal;
    private final IAutorizacion autorizacion;
    private final IProveedores proveedores;
    private final IInventario inventario;
    private final IClientes clientes;
    private final String clave;

    CargadorDatosDemo(ITerceros terceros, IAutenticacion autenticacion, IGestionPersonal personal,
            IAutorizacion autorizacion, IProveedores proveedores, IInventario inventario, IClientes clientes,
            @Value("${polimarket.demo.clave}") String clave) {
        this.terceros = terceros;
        this.autenticacion = autenticacion;
        this.personal = personal;
        this.autorizacion = autorizacion;
        this.proveedores = proveedores;
        this.inventario = inventario;
        this.clientes = clientes;
        this.clave = clave;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (terceros.consultarPorDocumento("CC", DOCUMENTO_RH).isPresent()) {
            return; // ya cargado
        }
        // Cuenta inicial de Recursos Humanos: sin ella nadie podría crear las demás cuentas.
        TerceroDto rh = persona(DOCUMENTO_RH, "Rosa", "Herrera");
        personal.registrarEmpleado(rh.id(), "Recursos Humanos");
        cuenta(rh, "rh@polimarket.test", RolUsuario.RECURSOS_HUMANOS);

        TerceroDto bruno = persona("1000000002", "Bruno", "Salazar");
        personal.registrarEmpleado(bruno.id(), "Bodega");
        UsuarioDto cuentaBodega = cuenta(bruno, "bodega@polimarket.test", RolUsuario.BODEGA);

        TerceroDto elena = persona("1000000003", "Elena", "Castro");
        personal.registrarEmpleado(elena.id(), "Entregas");
        cuenta(elena, "entregas@polimarket.test", RolUsuario.ENTREGAS);

        // Una vendedora con aval aprobado y un vendedor con aval pendiente.
        TerceroDto ana = persona("1000000004", "Ana", "Pérez");
        long anaVendedor = personal.registrarVendedor(ana.id(), "Norte").id();
        cuenta(ana, "ana@polimarket.test", RolUsuario.VENDEDOR);
        autorizacion.aprobar(anaVendedor, rh.id(), "Alta de demostración");

        TerceroDto luis = persona("1000000005", "Luis", "Mora");
        personal.registrarVendedor(luis.id(), "Sur");
        cuenta(luis, "luis@polimarket.test", RolUsuario.VENDEDOR);

        // Proveedor, productos y existencias. El azúcar queda bajo su mínimo para ver el bajo stock.
        TerceroDto empresa = terceros.registrar(new NuevoTercero(TipoTercero.JURIDICA, "NIT", "900100200", null, null,
                "Distribuidora Ejemplo", "3000000000", "ventas@distribuidora.test", "Zona industrial"));
        long proveedor = proveedores.registrar(new NuevoProveedor(empresa.id(), "Lucía", "3000000000")).id();
        producto("ARR-01", "Arroz", "5000", 5, 10, proveedor, cuentaBodega.id());
        producto("AZU-01", "Azúcar", "3500", 5, 4, proveedor, cuentaBodega.id());
        producto("ACE-01", "Aceite", "12000", 3, 20, proveedor, cuentaBodega.id());

        clientes.registrarCliente(persona("2000000001", "María", "Pérez").id(), List.of("sin gluten"));
        clientes.registrarCliente(persona("2000000002", "Carlos", "Ruiz").id(), List.of());
    }

    private TerceroDto persona(String documento, String nombres, String apellidos) {
        return terceros.registrar(new NuevoTercero(TipoTercero.NATURAL, "CC", documento, nombres, apellidos, null, null,
                null, null));
    }

    private UsuarioDto cuenta(TerceroDto tercero, String correo, RolUsuario rol) {
        return autenticacion.crearUsuario(new NuevoUsuario(tercero.id(), correo, clave, rol));
    }

    private void producto(String codigo, String nombre, String precio, int minimo, int unidades, long proveedor,
            long usuarioId) {
        ProductoDto p = inventario.crearProducto(new NuevoProducto(codigo, nombre, null, null, new BigDecimal(precio), minimo));
        proveedores.asociarProducto(proveedor, p.codigo(), null);
        inventario.registrarEntrada(new NuevaEntrada(p.id(), unidades, proveedor, "INICIAL", null), usuarioId);
    }
}
