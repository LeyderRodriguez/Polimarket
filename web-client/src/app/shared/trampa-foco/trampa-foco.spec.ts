import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TrampaFoco } from './trampa-foco';

@Component({
  imports: [TrampaFoco],
  template: `
    <div appTrampaFoco>
      <button id="a" type="button">A</button>
      <button id="b" type="button">B</button>
      <button id="c" type="button" disabled>C</button>
    </div>
  `,
})
class Anfitrion {}

describe('TrampaFoco', () => {
  const tecla = (destino: HTMLElement, shiftKey = false) => {
    const evento = new KeyboardEvent('keydown', { key: 'Tab', shiftKey, bubbles: true, cancelable: true });
    destino.dispatchEvent(evento);
    return evento;
  };
  const boton = (id: string) => document.getElementById(id)!;

  afterEach(() => document.querySelectorAll('#origen').forEach((o) => o.remove()));

  it('con Tab en el último elemento vuelve al primero, y con Mayús+Tab en el primero va al último', () => {
    const fixture = TestBed.createComponent(Anfitrion);
    fixture.detectChanges();

    boton('b').focus(); // el último habilitado (C está deshabilitado)
    expect(tecla(boton('b')).defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(boton('a'));

    expect(tecla(boton('a'), true).defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(boton('b'));
  });

  it('deja que el navegador mueva el foco cuando no está en un borde', () => {
    const fixture = TestBed.createComponent(Anfitrion);
    fixture.detectChanges();

    boton('a').focus();
    expect(tecla(boton('a')).defaultPrevented).toBe(false); // de A a B lo hace el navegador
  });

  it('al cerrarse devuelve el foco a quien lo abrió', () => {
    const origen = document.createElement('button');
    origen.id = 'origen';
    document.body.appendChild(origen);
    origen.focus();

    const fixture = TestBed.createComponent(Anfitrion);
    fixture.detectChanges();
    boton('a').focus();
    expect(document.activeElement).toBe(boton('a'));

    fixture.destroy();
    expect(document.activeElement).toBe(origen);
  });
});
