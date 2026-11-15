import { Component, computed, inject } from '@angular/core';
import { Tema, TemaServicio } from '../../core/tema/tema.servicio';

const ETIQUETAS: Record<Tema, string> = { sistema: 'Sistema', claro: 'Claro', oscuro: 'Oscuro' };

/** Botón que cambia el tema de color: sistema, claro u oscuro. Toma el color del texto de donde se coloque. */
@Component({
  selector: 'app-selector-tema',
  template: `
    <button type="button" class="tema" [attr.aria-label]="'Cambiar el tema de color. Actual: ' + etiqueta()" (click)="tema.alternar()">
      Tema: {{ etiqueta() }}
    </button>
  `,
  styles: `
    .tema {
      font: inherit;
      font-size: 12px;
      color: inherit;
      background: transparent;
      border: 1px solid color-mix(in srgb, currentColor 35%, transparent);
      border-radius: 12px;
      padding: 2px 10px;
      cursor: pointer;
    }
    .tema:hover {
      border-color: currentColor;
    }
  `,
})
export class SelectorTema {
  protected readonly tema = inject(TemaServicio);
  protected readonly etiqueta = computed(() => ETIQUETAS[this.tema.tema()]);
}
