import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import type { ProductoDisponible } from '../../core/api/tipos';
import { esperarTurno, proveedoresDePrueba } from '../../core/testing/ayudas';
import { Disponibilidad } from './disponibilidad';

const PRODUCTOS: ProductoDisponible[] = [
  { id: 1, codigo: 'ARR-01', nombre: 'Arroz', precio: '5000.00', stockFisico: 10, cantidadReservada: 2, cantidadDisponible: 8 },
  { id: 2, codigo: 'AZU-01', nombre: 'Azúcar', precio: '4200.00', stockFisico: 4, cantidadReservada: 4, cantidadDisponible: 0 },
  { id: 3, codigo: 'ACE-01', nombre: 'Aceite', precio: '12500.00', stockFisico: 20, cantidadReservada: 3, cantidadDisponible: 17 },
];

describe('Disponibilidad', () => {
  let fixture: ComponentFixture<Disponibilidad>;
  let el: HTMLElement;
  let http: HttpTestingController;

  const filas = () => [...el.querySelectorAll('tbody tr')].map((f) => f.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  const refrescar = async () => {
    await esperarTurno();
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [Disponibilidad], providers: proveedoresDePrueba() });
    fixture = TestBed.createComponent(Disponibilidad);
    el = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); // arranca y pide los productos
  });

  afterEach(() => http.verify());

  it('muestra cada producto con su precio en pesos, su stock y su estado', async () => {
    http.expectOne('/api/productos').flush(PRODUCTOS);
    await refrescar();

    expect(filas()).toHaveLength(3);
    expect(filas()[0]).toContain('ARR-01');
    expect(filas()[0]).toContain('Arroz');
    expect(filas()[0]).toContain('$ 5.000');
    expect(filas()[0]).toContain('Disponible');
    expect(filas()[1]).toContain('Agotado'); // disponible = 0
  });

  it('filtra por nombre o código sin importar tildes ni mayúsculas', async () => {
    http.expectOne('/api/productos').flush(PRODUCTOS);
    await refrescar();

    const buscador = el.querySelector<HTMLInputElement>('input[type="search"]')!;
    buscador.value = 'AZUCAR';
    buscador.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(filas()).toHaveLength(1);
    expect(filas()[0]).toContain('Azúcar');

    buscador.value = 'zzz';
    buscador.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(filas()[0]).toContain('Ningún producto coincide con «zzz».');
  });

  it('muestra el error del backend, por ejemplo cuando el vendedor no tiene aval', async () => {
    http
      .expectOne('/api/productos')
      .flush({ codigo: 'VENDEDOR_SIN_AVAL', mensaje: 'No tiene un aval vigente.' }, { status: 403, statusText: 'Forbidden' });
    await refrescar();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('VENDEDOR_SIN_AVAL');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No tiene un aval vigente.');
    expect(filas()[0]).toContain('No se pudieron cargar los productos.');
  });

  it('avisa cuando no hay productos registrados', async () => {
    http.expectOne('/api/productos').flush([]);
    await refrescar();

    expect(filas()[0]).toContain('No hay productos registrados.');
  });

  it('vuelve a pedir los productos al pulsar Actualizar y no deja datos viejos si falla', async () => {
    http.expectOne('/api/productos').flush(PRODUCTOS);
    await refrescar();

    el.querySelector<HTMLButtonElement>('button.btn')!.click();
    http.expectOne('/api/productos').flush({ codigo: 'ERROR_INTERNO', mensaje: 'Fallo.' }, { status: 500, statusText: 'Server Error' });
    await refrescar();

    expect(filas()).toHaveLength(1);
    expect(filas()[0]).toContain('No se pudieron cargar los productos.');
  });
});
