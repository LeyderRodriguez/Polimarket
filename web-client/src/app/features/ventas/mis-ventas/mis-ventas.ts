import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import type { ErrorApi, VentaResumen } from '../../../core/api/tipos';
import { VentasApi } from '../../../core/api/ventas.api';
import { pesos } from '../../../core/formato';
import { aErrorApi } from '../../../core/http/error-api';
import { AccionConfirmada } from '../../../shared/accion-confirmada/accion-confirmada';
import { AvisoError } from '../../../shared/aviso-error/aviso-error';

/** RSI-6: las ventas del vendedor y la anulación de las que siguen pendientes de entrega (libera la reserva de stock). */
@Component({
  selector: 'app-mis-ventas',
  imports: [AvisoError, AccionConfirmada],
  templateUrl: './mis-ventas.html',
})
export class MisVentas implements OnInit {
  private readonly api = inject(VentasApi);

  protected readonly ventas = signal<VentaResumen[]>([]);
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal<ErrorApi | null>(null);
  protected readonly errorAccion = signal<ErrorApi | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly anulando = signal(false);

  /** De la más reciente a la más antigua. */
  protected readonly ordenadas = computed(() => [...this.ventas()].sort((a, b) => b.ventaId - a.ventaId));
  protected readonly pesos = pesos;

  ngOnInit(): void {
    void this.cargar();
  }

  protected etiqueta(estado: VentaResumen['estado']): { texto: string; clase: string } {
    switch (estado) {
      case 'PENDIENTE_ENTREGA':
        return { texto: 'Pendiente de entrega', clase: 'warn' };
      case 'DESPACHADA':
        return { texto: 'Despachada', clase: 'ok' };
      case 'ANULADA':
        return { texto: 'Anulada', clase: 'neutra' };
      default:
        // Un estado que el backend agregue después no debe romper la pantalla: se muestra tal cual.
        return { texto: String(estado), clase: 'neutra' };
    }
  }

  protected async cargar(): Promise<void> {
    this.cargando.set(true);
    this.errorCarga.set(null);
    try {
      this.ventas.set(await firstValueFrom(this.api.misVentas()));
    } catch (fallo) {
      this.ventas.set([]);
      this.errorCarga.set(aErrorApi(fallo));
    } finally {
      this.cargando.set(false);
    }
  }

  protected async anular(ventaId: number): Promise<void> {
    this.anulando.set(true);
    this.errorAccion.set(null);
    this.aviso.set(null);
    try {
      await firstValueFrom(this.api.anular(ventaId));
      this.aviso.set(`Venta #${ventaId} anulada. La reserva de stock se liberó.`);
    } catch (fallo) {
      this.errorAccion.set(aErrorApi(fallo));
    } finally {
      this.anulando.set(false);
    }
    // Salga bien o mal, se vuelve a pedir la lista: si falló, es probable que la venta ya hubiera cambiado de estado.
    await this.cargar();
  }
}
