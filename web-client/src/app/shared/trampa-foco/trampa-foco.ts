import { Directive, ElementRef, OnDestroy, inject } from '@angular/core';

const ENFOCABLES =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Para diálogos modales: el teclado no puede salir del diálogo (Tab y Mayús+Tab dan la vuelta dentro de él) y, al
 * cerrarlo, el foco vuelve al elemento que lo abrió. Sin esto, quien navega con teclado o lector de pantalla se pierde
 * en la página que queda detrás.
 */
@Directive({
  selector: '[appTrampaFoco]',
  host: { '(keydown)': 'alTeclear($event)' },
})
export class TrampaFoco implements OnDestroy {
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  /** El elemento que tenía el foco cuando se abrió el diálogo (normalmente, el botón que lo abrió). */
  private readonly origen = document.activeElement as HTMLElement | null;

  protected alTeclear(evento: KeyboardEvent): void {
    if (evento.key !== 'Tab') {
      return;
    }
    const enfocables = [...this.el.nativeElement.querySelectorAll<HTMLElement>(ENFOCABLES)];
    const primero = enfocables[0];
    const ultimo = enfocables[enfocables.length - 1];
    if (!primero || !ultimo) {
      evento.preventDefault();
      return;
    }
    const activo = document.activeElement;
    const dentro = activo !== null && this.el.nativeElement.contains(activo);
    if (evento.shiftKey && (!dentro || activo === primero)) {
      evento.preventDefault();
      ultimo.focus();
    } else if (!evento.shiftKey && (!dentro || activo === ultimo)) {
      evento.preventDefault();
      primero.focus();
    }
  }

  ngOnDestroy(): void {
    if (this.origen?.isConnected) {
      this.origen.focus();
    }
  }
}
