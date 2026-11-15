import { ComponentFixture, TestBed } from '@angular/core/testing';
import { proveedoresDePrueba } from '../testing/ayudas';
import { DemostracionServicio } from './demostracion.servicio';
import { PanelDemostracion } from './panel-demostracion';

describe('PanelDemostracion', () => {
  let fixture: ComponentFixture<PanelDemostracion>;
  let el: HTMLElement;
  let demo: DemostracionServicio;

  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [PanelDemostracion], providers: proveedoresDePrueba() });
    demo = TestBed.inject(DemostracionServicio);
    demo.alternar();
    fixture = TestBed.createComponent(PanelDemostracion);
    el = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => localStorage.clear());

  it('explica qué hacer mientras no hay llamadas', () => {
    expect(el.textContent).toContain('Aún no hay llamadas');
  });

  it('lista las llamadas y marca las que fallaron', async () => {
    demo.registrar({ metodo: 'GET', url: '/api/productos', estado: 200, duracionMs: 12, codigo: null, enviado: null, recibido: [] });
    demo.registrar({ metodo: 'POST', url: '/api/ventas', estado: 403, duracionMs: 30, codigo: 'VENDEDOR_SIN_AVAL', enviado: {}, recibido: null });
    await fixture.whenStable();

    const filas = [...el.querySelectorAll('tbody tr')];
    expect(filas).toHaveLength(2);
    expect(filas[0]?.textContent).toContain('VENDEDOR_SIN_AVAL');
    expect(filas[0]?.classList).toContain('error');
    expect(filas[1]?.classList).not.toContain('error');
  });

  it('al elegir una llamada muestra lo enviado y lo recibido, y al elegirla otra vez lo oculta', async () => {
    demo.registrar({ metodo: 'POST', url: '/api/ventas', estado: 201, duracionMs: 5, codigo: null, enviado: { clienteId: 3 }, recibido: { total: '5000.00' } });
    await fixture.whenStable();

    const abrir = () => el.querySelector<HTMLButtonElement>('tbody .enlace')!.click();
    abrir();
    await fixture.whenStable();
    expect(el.querySelector('.detalle')?.textContent).toContain('"clienteId": 3');
    expect(el.querySelector('.detalle')?.textContent).toContain('"total": "5000.00"');

    abrir();
    await fixture.whenStable();
    expect(el.querySelector('.detalle')).toBeNull();
  });

  it('«Limpiar» vacía la lista', async () => {
    demo.registrar({ metodo: 'GET', url: '/api/x', estado: 200, duracionMs: 1, codigo: null, enviado: null, recibido: null });
    await fixture.whenStable();

    el.querySelector<HTMLButtonElement>('header button')!.click();
    await fixture.whenStable();

    expect(el.querySelectorAll('tbody .enlace')).toHaveLength(0);
  });
});
