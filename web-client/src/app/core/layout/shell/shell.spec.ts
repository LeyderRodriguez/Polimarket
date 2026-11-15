import { TestBed } from '@angular/core/testing';
import { iniciarComo, proveedoresDePrueba } from '../../testing/ayudas';
import { Shell } from './shell';

describe('Shell', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [Shell], providers: proveedoresDePrueba() }));

  async function mostrar(rol: 'VENDEDOR' | 'ENTREGAS'): Promise<HTMLElement> {
    await iniciarComo(rol);
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const opciones = (el: HTMLElement) => [...el.querySelectorAll('nav a')].map((a) => a.textContent?.trim());

  it('muestra el menú del vendedor', async () => {
    const el = await mostrar('VENDEDOR');
    expect(opciones(el)).toEqual(['Disponibilidad', 'Nueva venta', 'Mis ventas', 'Clientes']);
    expect(el.querySelector('.usuario')?.textContent).toContain('Persona de prueba');
  });

  it('muestra solo las entregas pendientes para el personal de entregas', async () => {
    const el = await mostrar('ENTREGAS');
    expect(opciones(el)).toEqual(['Entregas pendientes']);
  });
});
