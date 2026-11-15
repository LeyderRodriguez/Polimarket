import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AccionConfirmada } from './accion-confirmada';

@Component({
  imports: [AccionConfirmada],
  template: `
    <app-accion-confirmada
      texto="Anular"
      pregunta="¿Anular la venta?"
      textoConfirmar="Sí, anular"
      [peligro]="true"
      [deshabilitado]="bloqueada()"
      (confirmada)="veces = veces + 1"
    />
  `,
})
class Anfitrion {
  veces = 0;
  readonly bloqueada = signal(false);
}

describe('AccionConfirmada', () => {
  const crear = () => {
    const fixture = TestBed.createComponent(Anfitrion);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const boton = (texto: string) =>
      [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.trim() === texto);
    return { fixture, el, boton };
  };

  it('el primer clic pide confirmación y no ejecuta nada', () => {
    const { fixture, el, boton } = crear();

    boton('Anular')!.click();
    fixture.detectChanges();

    expect(el.textContent).toContain('¿Anular la venta?');
    expect(boton('Sí, anular')).toBeDefined();
    expect(fixture.componentInstance.veces).toBe(0);
  });

  it('mueve el foco al botón de confirmar y, si se cancela, de vuelta al inicial', async () => {
    const { fixture, boton } = crear();

    boton('Anular')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(document.activeElement).toBe(boton('Sí, anular'));

    boton('No')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(document.activeElement).toBe(boton('Anular'));
  });

  it('confirmar ejecuta una sola vez y vuelve al botón inicial', () => {
    const { fixture, el, boton } = crear();

    boton('Anular')!.click();
    fixture.detectChanges();
    boton('Sí, anular')!.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.veces).toBe(1);
    expect(el.textContent).not.toContain('¿Anular la venta?');
    expect(boton('Anular')).toBeDefined();
  });

  it('«No» cancela sin ejecutar', () => {
    const { fixture, boton } = crear();

    boton('Anular')!.click();
    fixture.detectChanges();
    boton('No')!.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.veces).toBe(0);
    expect(boton('Anular')).toBeDefined();
  });

  it('no deja empezar mientras está deshabilitada', () => {
    const { fixture, boton } = crear();
    fixture.componentInstance.bloqueada.set(true);
    fixture.detectChanges();

    expect(boton('Anular')!.disabled).toBe(true);
  });
});
