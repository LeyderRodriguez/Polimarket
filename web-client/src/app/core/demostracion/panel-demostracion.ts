import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { DemostracionServicio, type Llamada } from './demostracion.servicio';

/** Panel inferior del modo demostración: una fila por llamada al backend y, al elegirla, lo enviado y lo recibido. */
@Component({
  selector: 'app-panel-demostracion',
  imports: [DatePipe],
  templateUrl: './panel-demostracion.html',
  styleUrl: './panel-demostracion.css',
})
export class PanelDemostracion {
  protected readonly demo = inject(DemostracionServicio);
  private readonly elegidaId = signal<number | null>(null);

  protected readonly elegida = computed(() => this.demo.llamadas().find((l) => l.id === this.elegidaId()) ?? null);

  protected elegir(llamada: Llamada): void {
    this.elegidaId.set(this.elegidaId() === llamada.id ? null : llamada.id);
  }

  protected json(valor: unknown): string {
    return valor === null || valor === undefined ? '(sin cuerpo)' : JSON.stringify(valor, null, 2);
  }

  protected esError(llamada: Llamada): boolean {
    return llamada.estado === null || llamada.estado >= 400;
  }
}
