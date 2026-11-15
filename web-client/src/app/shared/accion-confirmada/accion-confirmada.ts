import { Component, ElementRef, Injector, afterNextRender, inject, input, output, signal } from '@angular/core';

/**
 * Botón para una acción que no se puede deshacer (anular una venta, despachar una entrega): el primer clic pide
 * confirmación ahí mismo y solo el segundo ejecuta. Evita los clics por error sin usar los diálogos del navegador.
 * Mueve el foco al botón nuevo en cada cambio, para que quien usa teclado o lector de pantalla no lo pierda.
 */
@Component({
  selector: 'app-accion-confirmada',
  template: `
    @if (pidiendo()) {
      <span class="confirmacion">
        <span role="alert">{{ pregunta() }}</span>
        <button type="button" class="btn" data-confirmar [class.peligro]="peligro()" (click)="confirmar()">
          {{ textoConfirmar() }}
        </button>
        <button type="button" class="btn" (click)="cancelar()">No</button>
      </span>
    } @else {
      <button
        type="button"
        class="btn"
        data-iniciar
        [class.peligro]="peligro()"
        [disabled]="deshabilitado()"
        (click)="pedir()"
      >
        {{ texto() }}
      </button>
    }
  `,
  styles: `
    .confirmacion {
      display: inline-flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: flex-end;
      gap: 6px;
      font-size: 13px;
    }
  `,
})
export class AccionConfirmada {
  readonly texto = input.required<string>();
  readonly pregunta = input('¿Está seguro?');
  readonly textoConfirmar = input('Sí');
  readonly peligro = input(false);
  readonly deshabilitado = input(false);
  /** Se emite solo después de confirmar. */
  readonly confirmada = output<void>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  protected readonly pidiendo = signal(false);

  protected pedir(): void {
    this.pidiendo.set(true);
    this.enfocar('[data-confirmar]');
  }

  protected cancelar(): void {
    this.pidiendo.set(false);
    this.enfocar('[data-iniciar]');
  }

  protected confirmar(): void {
    this.pidiendo.set(false);
    this.enfocar('[data-iniciar]');
    this.confirmada.emit();
  }

  private enfocar(selector: string): void {
    afterNextRender(() => this.host.nativeElement.querySelector<HTMLElement>(selector)?.focus(), {
      injector: this.injector,
    });
  }
}
