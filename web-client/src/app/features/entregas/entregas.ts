import { Component, OnInit, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import type { ErrorApi, VentaPendiente } from '../../core/api/tipos';
import { VentasApi } from '../../core/api/ventas.api';
import { aErrorApi } from '../../core/http/error-api';
import { AccionConfirmada } from '../../shared/accion-confirmada/accion-confirmada';
import { AvisoError } from '../../shared/aviso-error/aviso-error';

/** RSI-5: el personal de entregas ve qué llevar, a quién y a dónde, y despacha. Despachar descuenta el stock en el backend. */
@Component({
  selector: 'app-entregas',
  imports: [AvisoError, AccionConfirmada],
  templateUrl: './entregas.html',
  styleUrl: './entregas.css',
})
export class Entregas implements OnInit {
  private readonly api = inject(VentasApi);

  protected readonly pendientes = signal<VentaPendiente[]>([]);
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal<ErrorApi | null>(null);
  protected readonly errorAccion = signal<ErrorApi | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly despachando = signal(false);

  ngOnInit(): void {
    void this.cargar();
  }

  protected async cargar(): Promise<void> {
    this.cargando.set(true);
    this.errorCarga.set(null);
    try {
      this.pendientes.set(await firstValueFrom(this.api.pendientesEntrega()));
    } catch (fallo) {
      this.pendientes.set([]);
      this.errorCarga.set(aErrorApi(fallo));
    } finally {
      this.cargando.set(false);
    }
  }

  protected async despachar(ventaId: number): Promise<void> {
    this.despachando.set(true);
    this.errorAccion.set(null);
    this.aviso.set(null);
    try {
      const entrega = await firstValueFrom(this.api.despachar(ventaId));
      this.aviso.set(`Venta #${ventaId} despachada (entrega #${entrega.entregaId}). Se descontó el stock.`);
    } catch (fallo) {
      this.errorAccion.set(aErrorApi(fallo));
    } finally {
      this.despachando.set(false);
    }
    // Salga bien o mal, se vuelve a pedir la lista: si falló, es probable que otra persona ya la hubiera despachado.
    await this.cargar();
  }
}
