import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import type { VentaResumen } from '../../../core/api/tipos';
import { esperarTurno, proveedoresDePrueba } from '../../../core/testing/ayudas';
import { MisVentas } from './mis-ventas';

const VENTAS: VentaResumen[] = [
  { ventaId: 3, cliente: 'María Pérez', estado: 'DESPACHADA', total: '10000.00' },
  { ventaId: 5, cliente: 'Laura Gómez', estado: 'PENDIENTE_ENTREGA', total: '24000.00' },
  { ventaId: 4, cliente: 'Carlos Ruiz', estado: 'ANULADA', total: '5000.00' },
];

describe('MisVentas', () => {
  let fixture: ComponentFixture<MisVentas>;
  let el: HTMLElement;
  let http: HttpTestingController;

  const filas = () => [...el.querySelectorAll('tbody tr')].map((f) => f.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  const boton = (texto: string) =>
    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.trim() === texto);
  const refrescar = async () => {
    await esperarTurno();
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [MisVentas], providers: proveedoresDePrueba() });
    fixture = TestBed.createComponent(MisVentas);
    el = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('lista las ventas de la más reciente a la más antigua, con su estado y su total en pesos', async () => {
    http.expectOne('/api/ventas/mis-ventas').flush(VENTAS);
    await refrescar();

    expect(filas()).toHaveLength(3);
    expect(filas()[0]).toContain('5');
    expect(filas()[0]).toContain('Laura Gómez');
    expect(filas()[0]).toContain('Pendiente de entrega');
    expect(filas()[0]).toContain('$ 24.000');
    expect(filas()[1]).toContain('Anulada');
    expect(filas()[2]).toContain('Despachada');
  });

  it('muestra tal cual un estado nuevo que el backend agregue, sin romper la pantalla', async () => {
    const futura = { ventaId: 9, cliente: 'Ana', estado: 'ENTREGADA', total: '1000.00' } as unknown as VentaResumen;
    http.expectOne('/api/ventas/mis-ventas').flush([futura]);
    await refrescar();

    expect(filas()[0]).toContain('ENTREGADA');
  });

  it('solo ofrece anular las ventas que siguen pendientes de entrega', async () => {
    http.expectOne('/api/ventas/mis-ventas').flush(VENTAS);
    await refrescar();

    expect(el.querySelectorAll('tbody button')).toHaveLength(1);
    expect(filas()[0]).toContain('Anular');
  });

  it('anular pide confirmación, avisa y vuelve a pedir la lista', async () => {
    http.expectOne('/api/ventas/mis-ventas').flush(VENTAS);
    await refrescar();

    boton('Anular')!.click();
    fixture.detectChanges();
    http.expectNone((r) => r.url.endsWith('/cancelacion')); // todavía no: falta confirmar
    expect(el.textContent).toContain('¿Anular la venta #5?');

    boton('Sí, anular')!.click();
    const peticion = http.expectOne((r) => r.method === 'POST' && r.url === '/api/ventas/5/cancelacion');
    peticion.flush({ ventaId: 5, estado: 'ANULADA', total: '24000.00', reservaEstado: 'LIBERADA' });
    await esperarTurno();
    http
      .expectOne('/api/ventas/mis-ventas')
      .flush(VENTAS.map((v) => (v.ventaId === 5 ? { ...v, estado: 'ANULADA' as const } : v)));
    await refrescar();

    expect(el.querySelector('[role="status"]')?.textContent).toContain('Venta #5 anulada');
    expect(el.querySelectorAll('tbody button')).toHaveLength(0);
    expect(filas()[0]).toContain('Anulada');
  });

  it('muestra el error del backend si ya no se puede anular y refresca la lista', async () => {
    http.expectOne('/api/ventas/mis-ventas').flush(VENTAS);
    await refrescar();

    boton('Anular')!.click();
    fixture.detectChanges();
    boton('Sí, anular')!.click();
    http
      .expectOne('/api/ventas/5/cancelacion')
      .flush({ codigo: 'ESTADO_INVALIDO', mensaje: 'La venta ya fue despachada.' }, { status: 409, statusText: 'Conflict' });
    await esperarTurno();
    http
      .expectOne('/api/ventas/mis-ventas')
      .flush(VENTAS.map((v) => (v.ventaId === 5 ? { ...v, estado: 'DESPACHADA' as const } : v)));
    await refrescar();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('ESTADO_INVALIDO');
    expect(el.querySelector('[role="status"]')).toBeNull();
    expect(filas()[0]).toContain('Despachada');
  });

  it('muestra el error de carga y no deja datos viejos', async () => {
    http.expectOne('/api/ventas/mis-ventas').flush({ codigo: 'ERROR_INTERNO', mensaje: 'Fallo.' }, { status: 500, statusText: 'Error' });
    await refrescar();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('ERROR_INTERNO');
    expect(filas()[0]).toContain('No se pudieron cargar las ventas.');
  });

  it('avisa cuando todavía no hay ventas', async () => {
    http.expectOne('/api/ventas/mis-ventas').flush([]);
    await refrescar();

    expect(filas()[0]).toContain('Aún no tiene ventas.');
  });
});
