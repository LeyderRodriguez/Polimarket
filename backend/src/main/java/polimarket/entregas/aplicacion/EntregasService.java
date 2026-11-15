package polimarket.entregas.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimarket.bodega.api.IInventario;
import polimarket.bodega.api.ReservaDto;
import polimarket.entregas.api.EntregaDto;
import polimarket.entregas.api.IEntregas;
import polimarket.entregas.dominio.Entrega;
import polimarket.entregas.dominio.EntregaRepositorio;
import polimarket.ventas.api.IVentas;
import polimarket.ventas.api.VentaPendienteDto;

@Service
@Transactional
class EntregasService implements IEntregas {

    private final EntregaRepositorio entregas;
    private final IVentas ventas;
    private final IInventario inventario;
    private final Clock reloj;

    EntregasService(EntregaRepositorio entregas, IVentas ventas, IInventario inventario, Clock reloj) {
        this.entregas = entregas;
        this.ventas = ventas;
        this.inventario = inventario;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaPendienteDto> listarPendientes() {
        return ventas.listarPendientesEntrega();
    }

    @Override
    public EntregaDto despachar(long ventaId, long usuarioId) {
        // 1) Ventas bloquea la venta y comprueba que siga pendiente: aquí se frena el doble despacho.
        VentaPendienteDto venta = ventas.marcarDespachada(ventaId);
        // 2) Bodega consume la reserva y descuenta el físico. Si algo falla, todo se revierte, incluido el paso 1.
        ReservaDto reserva = inventario.consumirReserva(ventaId, usuarioId);
        Entrega entrega = entregas.save(new Entrega(ventaId, Instant.now(reloj), venta.direccionEntrega(),
                venta.cliente(), usuarioId));
        return new EntregaDto(entrega.getId(), ventaId, entrega.getEstado(), reserva.estado());
    }
}
