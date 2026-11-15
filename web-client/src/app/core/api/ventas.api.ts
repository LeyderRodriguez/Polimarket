import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { Entrega, SolicitudVenta, Venta, VentaPendiente, VentaResumen } from './tipos';

/** Ventas y su entrega. El total, el aval y el stock los decide el backend; aquí solo se envía y se recibe. */
@Injectable({ providedIn: 'root' })
export class VentasApi {
  private readonly http = inject(HttpClient);

  /** `POST /api/ventas`: registra la venta y reserva el stock, todo o nada. */
  registrar(solicitud: SolicitudVenta) {
    return this.http.post<Venta>('/api/ventas', solicitud);
  }

  /** `GET /api/ventas/mis-ventas`: las ventas del vendedor que inició sesión. */
  misVentas() {
    return this.http.get<VentaResumen[]>('/api/ventas/mis-ventas');
  }

  /** `POST /api/ventas/{id}/cancelacion`: anula una venta propia pendiente y libera su reserva. */
  anular(ventaId: number) {
    return this.http.post<Venta>(`/api/ventas/${ventaId}/cancelacion`, null);
  }

  /** `GET /api/ventas/pendientes-entrega`: qué entregar, a quién y dónde (solo Entregas). */
  pendientesEntrega() {
    return this.http.get<VentaPendiente[]>('/api/ventas/pendientes-entrega');
  }

  /** `POST /api/ventas/{id}/despacho`: despacha una sola vez, consume la reserva y descuenta el stock (solo Entregas). */
  despachar(ventaId: number) {
    return this.http.post<Entrega>(`/api/ventas/${ventaId}/despacho`, null);
  }
}
