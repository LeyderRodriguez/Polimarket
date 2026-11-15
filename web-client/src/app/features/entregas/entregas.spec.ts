import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import type { VentaPendiente } from '../../core/api/tipos';
import { esperarTurno, proveedoresDePrueba } from '../../core/testing/ayudas';
import { Entregas } from './entregas';

const PENDIENTES: VentaPendiente[] = [
  {
    ventaId: 5,
    cliente: 'Laura Gómez',
    direccionEntrega: 'Carrera 7 # 8-9, Bogotá',
    productos: [
      { productoId: 3, nombre: 'Aceite', cantidad: 2 },
      { productoId: 1, nombre: 'Arroz', cantidad: 4 },
    ],
  },
  { ventaId: 6, cliente: 'Carlos Ruiz', direccionEntrega: 'Calle 1 # 2-3', productos: [{ productoId: 2, nombre: 'Azúcar', cantidad: 1 }] },
];

describe('Entregas', () => {
  let fixture: ComponentFixture<Entregas>;
  let el: HTMLElement;
  let http: HttpTestingController;

  const tarjetas = () => [...el.querySelectorAll('article')].map((t) => t.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  const boton = (texto: string) =>
    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.trim() === texto);
  const refrescar = async () => {
    await esperarTurno();
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [Entregas], providers: proveedoresDePrueba() });
    fixture = TestBed.createComponent(Entregas);
    el = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('muestra qué llevar, a quién y a dónde en una tarjeta por venta', async () => {
    http.expectOne('/api/ventas/pendientes-entrega').flush(PENDIENTES);
    await refrescar();

    expect(tarjetas()).toHaveLength(2);
    expect(tarjetas()[0]).toContain('Venta #5');
    expect(tarjetas()[0]).toContain('Laura Gómez');
    expect(tarjetas()[0]).toContain('Carrera 7 # 8-9, Bogotá');
    expect(tarjetas()[0]).toContain('2 × Aceite');
    expect(tarjetas()[0]).toContain('4 × Arroz');
  });

  it('despachar pide confirmación, avisa y vuelve a pedir la lista', async () => {
    http.expectOne('/api/ventas/pendientes-entrega').flush(PENDIENTES);
    await refrescar();

    boton('Despachar')!.click();
    fixture.detectChanges();
    http.expectNone((r) => r.url.endsWith('/despacho')); // falta confirmar
    expect(el.textContent).toContain('¿Despachar la venta #5?');

    boton('Sí, despachar')!.click();
    http
      .expectOne((r) => r.method === 'POST' && r.url === '/api/ventas/5/despacho')
      .flush({ entregaId: 9, ventaId: 5, estado: 'DESPACHADA', reservaEstado: 'CONSUMIDA' });
    await esperarTurno();
    http.expectOne('/api/ventas/pendientes-entrega').flush([PENDIENTES[1]]);
    await refrescar();

    expect(el.querySelector('[role="status"]')?.textContent).toContain('Venta #5 despachada (entrega #9)');
    expect(tarjetas()).toHaveLength(1);
    expect(tarjetas()[0]).toContain('Venta #6');
  });

  it('muestra el error del backend si otra persona ya la despachó y refresca la lista', async () => {
    http.expectOne('/api/ventas/pendientes-entrega').flush(PENDIENTES);
    await refrescar();

    boton('Despachar')!.click();
    fixture.detectChanges();
    boton('Sí, despachar')!.click();
    http
      .expectOne('/api/ventas/5/despacho')
      .flush({ codigo: 'ESTADO_INVALIDO', mensaje: 'La venta ya fue despachada.' }, { status: 409, statusText: 'Conflict' });
    await esperarTurno();
    http.expectOne('/api/ventas/pendientes-entrega').flush([PENDIENTES[1]]);
    await refrescar();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('ESTADO_INVALIDO');
    expect(el.querySelector('[role="status"]')).toBeNull();
    expect(tarjetas()).toHaveLength(1);
  });

  it('avisa cuando no hay entregas pendientes', async () => {
    http.expectOne('/api/ventas/pendientes-entrega').flush([]);
    await refrescar();

    expect(el.textContent).toContain('No hay entregas pendientes.');
  });

  it('muestra el error del backend si el rol no corresponde', async () => {
    http
      .expectOne('/api/ventas/pendientes-entrega')
      .flush({ codigo: 'SIN_PERMISO', mensaje: 'Su rol no puede hacer esto.' }, { status: 403, statusText: 'Forbidden' });
    await refrescar();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('SIN_PERMISO');
    expect(el.textContent).toContain('No se pudieron cargar las entregas.');
  });
});
