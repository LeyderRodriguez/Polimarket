import { Component, input } from '@angular/core';
import type { ErrorApi } from '../../core/api/tipos';

/** Muestra el error que devolvió la API tal cual: su código y el mensaje del servidor. No sabe nada de HTTP. */
@Component({
  selector: 'app-aviso-error',
  template: `
    @if (error(); as e) {
      <div class="banner bad" role="alert"><code>{{ e.codigo }}</code> {{ e.mensaje }}</div>
    }
  `,
})
export class AvisoError {
  readonly error = input<ErrorApi | null>(null);
}
