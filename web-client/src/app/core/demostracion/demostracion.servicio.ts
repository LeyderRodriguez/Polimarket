import { Injectable, signal } from '@angular/core';

export interface Llamada {
  id: number;
  hora: Date;
  metodo: string;
  url: string;
  /** `null` si la respuesta nunca llegó (sin conexión). */
  estado: number | null;
  duracionMs: number;
  /** Código de error que devolvió el backend, si lo hubo. */
  codigo: string | null;
  enviado: unknown;
  recibido: unknown;
}

const CLAVE = 'polimarket.demostracion';
const MAXIMO = 50;

/**
 * Modo demostración: guarda las últimas llamadas que el cliente hace al backend para mostrarlas en un panel y enseñar
 * que la pantalla solo consume la API. Apagado (por defecto) no guarda nada. Solo observa: no modifica ni reenvía.
 */
@Injectable({ providedIn: 'root' })
export class DemostracionServicio {
  private readonly encendido = signal(this.leer());
  private readonly registro = signal<Llamada[]>([]);
  private siguienteId = 1;

  readonly activo = this.encendido.asReadonly();
  /** Más reciente primero. */
  readonly llamadas = this.registro.asReadonly();

  alternar(): void {
    const nuevo = !this.encendido();
    this.encendido.set(nuevo);
    if (!nuevo) {
      this.limpiar();
    }
    try {
      localStorage.setItem(CLAVE, String(nuevo));
    } catch {
      // Sin almacenamiento: funciona igual, solo no se recuerda.
    }
  }

  registrar(llamada: Omit<Llamada, 'id' | 'hora'>): void {
    if (!this.encendido()) {
      return;
    }
    const nueva: Llamada = { ...llamada, id: this.siguienteId++, hora: new Date() };
    this.registro.update((anteriores) => [nueva, ...anteriores].slice(0, MAXIMO));
  }

  limpiar(): void {
    this.registro.set([]);
  }

  private leer(): boolean {
    try {
      return localStorage.getItem(CLAVE) === 'true';
    } catch {
      return false;
    }
  }
}
