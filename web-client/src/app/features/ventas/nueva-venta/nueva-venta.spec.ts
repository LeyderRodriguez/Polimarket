import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import type { ProductoDisponible } from '../../../core/api/tipos';
import { esperarTurno, proveedoresDePrueba } from '../../../core/testing/ayudas';
import { CLIENTES_DE_PRUEBA, PRODUCTOS_DE_PRUEBA } from '../../../core/testing/datos';
import { RegistroCliente } from '../../../shared/registro-cliente/registro-cliente';
import { NuevaVenta } from './nueva-venta';

describe('NuevaVenta', () => {
  let fixture: ComponentFixture<NuevaVenta>;
  let el: HTMLElement;
  let http: HttpTestingController;

  const refrescar = async () => {
    await esperarTurno();
    fixture.detectChanges();
  };
  const cargar = async (productos: ProductoDisponible[] = PRODUCTOS_DE_PRUEBA) => {
    fixture.detectChanges(); // arranca y pide clientes y productos
    http.expectOne('/api/clientes').flush(CLIENTES_DE_PRUEBA);
    http.expectOne('/api/productos').flush(productos);
    await refrescar();
  };
  const escribir = (selector: string, valor: string) => {
    const campo = el.querySelector<HTMLInputElement>(selector)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const boton = (texto: string) =>
    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes(texto))!;
  const opciones = (selector: string) => [...el.querySelectorAll(`${selector} option`)].map((o) => o.textContent?.replace(/\s+/g, ' ').trim());
  const filas = () => [...el.querySelectorAll('tbody tr')].map((f) => f.textContent?.replace(/\s+/g, ' ').trim() ?? '');

  /** Agrega Arroz (el primero que se ofrece) con esa cantidad. */
  const agregarArroz = (cantidad: number) => {
    escribir('#cantidad', String(cantidad));
    boton('Agregar').click();
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [NuevaVenta], providers: proveedoresDePrueba() });
    fixture = TestBed.createComponent(NuevaVenta);
    el = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('carga los clientes y ofrece solo los productos que tienen disponible', async () => {
    await cargar();

    expect(opciones('#cliente')).toEqual(['María Pérez', 'Carlos Ruiz']);
    expect(opciones('#producto')).toEqual(['ARR-01 · Arroz', 'ACE-01 · Aceite']); // Azúcar está agotado
    expect(el.textContent).toContain('Disponible de Arroz: 8');
  });

  it('al agregar un producto lo pasa al carrito y deja de ofrecerlo', async () => {
    await cargar();
    agregarArroz(3);

    expect(filas()).toHaveLength(1);
    expect(filas()[0]).toContain('Arroz');
    expect(el.querySelector<HTMLInputElement>('tbody input')!.value).toBe('3');
    expect(opciones('#producto')).toEqual(['ACE-01 · Aceite']);

    boton('Quitar').click();
    fixture.detectChanges();
    expect(filas()[0]).toContain('Aún no agregó productos.');
    expect(opciones('#producto')).toEqual(['ARR-01 · Arroz', 'ACE-01 · Aceite']);
  });

  it('no deja confirmar sin productos ni dirección de entrega', async () => {
    await cargar();
    expect(boton('Confirmar venta').disabled).toBe(true);

    agregarArroz(1);
    expect(boton('Confirmar venta').disabled).toBe(true); // falta la dirección

    escribir('#direccion', 'Calle 10 # 20-30');
    expect(boton('Confirmar venta').disabled).toBe(false);
  });

  it('confirma la venta: envía el pedido y muestra el total que calculó el servidor', async () => {
    await cargar();
    escribir('#direccion', '  Calle 10 # 20-30  ');
    agregarArroz(2);

    boton('Confirmar venta').click();
    const peticion = http.expectOne((r) => r.url === '/api/ventas' && r.method === 'POST');
    expect(peticion.request.body).toEqual({
      clienteId: 1,
      direccionEntrega: 'Calle 10 # 20-30',
      productos: [{ productoId: 1, cantidad: 2 }],
    });
    peticion.flush({ ventaId: 7, estado: 'PENDIENTE_ENTREGA', total: '10000.00', reservaEstado: 'ACTIVA' });
    await esperarTurno();
    // La venta reservó stock: la pantalla vuelve a pedir lo disponible.
    http.expectOne('/api/productos').flush(PRODUCTOS_DE_PRUEBA);
    await refrescar();

    const aviso = el.querySelector('.banner.ok')?.textContent?.replace(/\s+/g, ' ');
    expect(aviso).toContain('Venta #7 registrada');
    expect(aviso).toContain('PENDIENTE_ENTREGA');
    expect(aviso).toContain('$ 10.000');
    expect(filas()[0]).toContain('Aún no agregó productos.');
  });

  it('muestra el error del backend (stock insuficiente) y conserva el carrito para corregirlo', async () => {
    await cargar();
    escribir('#direccion', 'Calle 1');
    agregarArroz(50);

    boton('Confirmar venta').click();
    http
      .expectOne((r) => r.url === '/api/ventas')
      .flush({ codigo: 'STOCK_INSUFICIENTE', mensaje: 'Solo hay 8 unidades de Arroz.' }, { status: 409, statusText: 'Conflict' });
    await refrescar();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('STOCK_INSUFICIENTE');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Solo hay 8 unidades de Arroz.');
    expect(filas()[0]).toContain('Arroz'); // el carrito sigue ahí
    expect(boton('Confirmar venta').disabled).toBe(false);
  });

  it('si el vendedor no tiene aval muestra el error y oculta el formulario', async () => {
    fixture.detectChanges();
    const sinAval = { codigo: 'VENDEDOR_SIN_AVAL', mensaje: 'El vendedor no tiene aval vigente.' };
    http.expectOne('/api/clientes').flush(sinAval, { status: 403, statusText: 'Forbidden' });
    http.expectOne('/api/productos').flush(sinAval, { status: 403, statusText: 'Forbidden' });
    await refrescar();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('VENDEDOR_SIN_AVAL');
    expect(el.querySelector('#direccion')).toBeNull();
  });

  it('un cliente recién registrado queda seleccionado y el diálogo se cierra', async () => {
    await cargar();
    boton('Registrar cliente nuevo').click();
    fixture.detectChanges();

    const dialogo = fixture.debugElement.query(By.directive(RegistroCliente));
    (dialogo.componentInstance as RegistroCliente).registrado.emit({ id: 3, terceroId: 13, nombre: 'Sofía Mejía', preferencias: [] });
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.directive(RegistroCliente))).toBeNull();
    expect(opciones('#cliente')).toContain('Sofía Mejía');
    expect(el.querySelector<HTMLSelectElement>('#cliente')!.selectedOptions[0]?.textContent).toContain('Sofía Mejía');
  });
});
