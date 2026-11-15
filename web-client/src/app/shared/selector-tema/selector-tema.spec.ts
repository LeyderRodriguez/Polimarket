import { TestBed } from '@angular/core/testing';
import { SelectorTema } from './selector-tema';

describe('SelectorTema', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-tema');
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-tema');
  });

  it('muestra el tema actual y lo cambia al pulsarlo', async () => {
    const fixture = TestBed.createComponent(SelectorTema);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const boton = el.querySelector('button')!;

    expect(boton.textContent).toContain('Sistema');

    boton.click();
    fixture.detectChanges();
    expect(boton.textContent).toContain('Claro');
    expect(document.documentElement.getAttribute('data-tema')).toBe('claro');

    boton.click();
    fixture.detectChanges();
    expect(boton.textContent).toContain('Oscuro');
    expect(document.documentElement.getAttribute('data-tema')).toBe('oscuro');
    expect(boton.getAttribute('aria-label')).toContain('Oscuro');
  });
});
