import { Injectable, signal } from '@angular/core';

export type Tema = 'sistema' | 'claro' | 'oscuro';

const CLAVE = 'polimarket.tema';
const TEMAS: readonly Tema[] = ['sistema', 'claro', 'oscuro'];

/**
 * Tema de color de la aplicación: el del sistema (por defecto), claro u oscuro. La elección se recuerda en este
 * navegador. Se aplica con un atributo en `<html>` que `styles.css` convierte en colores; con «sistema» no hay atributo
 * y manda la preferencia del sistema operativo.
 */
@Injectable({ providedIn: 'root' })
export class TemaServicio {
  private readonly actual = signal<Tema>(this.leer());
  readonly tema = this.actual.asReadonly();

  constructor() {
    this.aplicar(this.actual());
  }

  elegir(tema: Tema): void {
    this.actual.set(tema);
    this.aplicar(tema);
    try {
      localStorage.setItem(CLAVE, tema);
    } catch {
      // Sin almacenamiento (modo privado, bloqueado): el tema sigue funcionando, solo no se recuerda.
    }
  }

  /** Recorre sistema → claro → oscuro → sistema. */
  alternar(): void {
    const siguiente = TEMAS[(TEMAS.indexOf(this.actual()) + 1) % TEMAS.length] ?? 'sistema';
    this.elegir(siguiente);
  }

  private leer(): Tema {
    try {
      const guardado = localStorage.getItem(CLAVE);
      return TEMAS.find((t) => t === guardado) ?? 'sistema';
    } catch {
      return 'sistema';
    }
  }

  private aplicar(tema: Tema): void {
    const raiz = document.documentElement;
    if (tema === 'sistema') {
      raiz.removeAttribute('data-tema');
    } else {
      raiz.setAttribute('data-tema', tema);
    }
  }
}
