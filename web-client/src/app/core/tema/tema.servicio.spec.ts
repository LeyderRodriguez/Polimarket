import { TestBed } from '@angular/core/testing';
import { TemaServicio } from './tema.servicio';

describe('TemaServicio', () => {
  const atributo = () => document.documentElement.getAttribute('data-tema');

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-tema');
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    document.documentElement.removeAttribute('data-tema');
  });

  it('por defecto sigue al sistema: no pone ningún atributo', () => {
    const tema = TestBed.inject(TemaServicio);

    expect(tema.tema()).toBe('sistema');
    expect(atributo()).toBeNull();
  });

  it('al elegir oscuro o claro pone el atributo y lo recuerda; al volver a «sistema» lo quita', () => {
    const tema = TestBed.inject(TemaServicio);

    tema.elegir('oscuro');
    expect(atributo()).toBe('oscuro');
    expect(localStorage.getItem('polimarket.tema')).toBe('oscuro');

    tema.elegir('claro');
    expect(atributo()).toBe('claro');

    tema.elegir('sistema');
    expect(atributo()).toBeNull();
  });

  it('recuerda la elección anterior al abrir la aplicación de nuevo', () => {
    localStorage.setItem('polimarket.tema', 'claro');

    const tema = TestBed.inject(TemaServicio);

    expect(tema.tema()).toBe('claro');
    expect(atributo()).toBe('claro');
  });

  it('ignora un valor guardado que no es un tema', () => {
    localStorage.setItem('polimarket.tema', 'violeta');

    expect(TestBed.inject(TemaServicio).tema()).toBe('sistema');
  });

  it('funciona aunque el navegador bloquee el almacenamiento', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });

    const tema = TestBed.inject(TemaServicio);
    tema.elegir('oscuro');

    expect(tema.tema()).toBe('oscuro');
    expect(atributo()).toBe('oscuro');
  });

  it('alternar recorre sistema, claro, oscuro y otra vez sistema', () => {
    const tema = TestBed.inject(TemaServicio);

    tema.alternar();
    expect(tema.tema()).toBe('claro');
    tema.alternar();
    expect(tema.tema()).toBe('oscuro');
    tema.alternar();
    expect(tema.tema()).toBe('sistema');
  });
});
