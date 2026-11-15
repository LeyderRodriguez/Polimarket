import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import type { Cliente, Tercero } from '../../core/api/tipos';
import { esperarTurno, proveedoresDePrueba } from '../../core/testing/ayudas';
import { RegistroCliente } from './registro-cliente';

const SOFIA: Tercero = {
  id: 9,
  tipo: 'NATURAL',
  tipoDocumento: 'CC',
  numeroDocumento: '1003',
  nombres: 'Sofía',
  apellidos: 'Mejía',
  razonSocial: '',
  telefono: '',
  correo: '',
  direccion: '',
};

describe('RegistroCliente', () => {
  let fixture: ComponentFixture<RegistroCliente>;
  let el: HTMLElement;
  let http: HttpTestingController;
  let registrados: Cliente[];
  let cierres: number;

  const escribir = (selector: string, valor: string) => {
    const campo = el.querySelector<HTMLInputElement>(selector)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  };
  const enviar = async () => {
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    await esperarTurno();
    fixture.detectChanges();
  };
  const terceroPorDocumento = (documento: string): TestRequest =>
    http.expectOne((r) => r.url === '/api/terceros' && r.params.get('numeroDocumento') === documento);
  const responder = async (peticion: TestRequest, cuerpo: object, estado = 200) => {
    peticion.flush(cuerpo, { status: estado, statusText: estado === 200 ? 'OK' : 'Error' });
    await esperarTurno();
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [RegistroCliente], providers: proveedoresDePrueba() });
    fixture = TestBed.createComponent(RegistroCliente);
    el = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
    registrados = [];
    cierres = 0;
    fixture.componentInstance.registrado.subscribe((c) => registrados.push(c));
    fixture.componentInstance.cerrado.subscribe(() => cierres++);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('exige el número de documento antes de buscar', async () => {
    await enviar();

    http.expectNone('/api/terceros');
    expect(el.textContent).toContain('Escriba el número de documento.');
  });

  it('si el tercero ya existe salta los datos, lo muestra sin contacto y registra el cliente', async () => {
    escribir('input[formControlName="numeroDocumento"]', '1003');
    await enviar();
    await responder(terceroPorDocumento('1003'), SOFIA);

    expect(el.textContent).toContain('Sofía Mejía');
    expect(el.querySelector('.pasos li.omitido')?.textContent).toContain('Datos del tercero');
    expect(el.textContent).toContain('Por privacidad');

    escribir('input[placeholder]', 'sin gluten, sin lactosa');
    await enviar();
    const peticion = http.expectOne('/api/clientes');
    expect(peticion.request.body).toEqual({ terceroId: 9, preferencias: ['sin gluten', 'sin lactosa'] });
    await responder(peticion, { id: 5, terceroId: 9, nombre: 'Sofía Mejía', preferencias: ['sin gluten', 'sin lactosa'] });

    expect(registrados).toHaveLength(1);
    expect(registrados[0]?.nombre).toBe('Sofía Mejía');
  });

  it('si el tercero no existe pide sus datos, lo registra y luego registra el cliente', async () => {
    escribir('input[formControlName="numeroDocumento"]', '9999');
    await enviar();
    await responder(
      terceroPorDocumento('9999'),
      { codigo: 'NO_ENCONTRADO', mensaje: 'No existe un tercero con ese documento.' },
      404,
    );
    expect(el.textContent).toContain('Nombres');

    await enviar(); // sin llenar nada: no debe llamar al backend
    http.expectNone('/api/terceros');
    expect(el.textContent).toContain('Los nombres son obligatorios.');

    escribir('input[formControlName="nombres"]', 'Camilo');
    escribir('input[formControlName="apellidos"]', 'Torres');
    escribir('input[formControlName="telefono"]', '3001112233');
    await enviar();
    const registro = http.expectOne((r) => r.url === '/api/terceros' && r.method === 'POST');
    expect(registro.request.body).toMatchObject({
      tipo: 'NATURAL',
      tipoDocumento: 'CC',
      numeroDocumento: '9999',
      nombres: 'Camilo',
      apellidos: 'Torres',
      telefono: '3001112233',
    });
    await responder(registro, { ...SOFIA, id: 20, numeroDocumento: '9999', nombres: 'Camilo', apellidos: 'Torres' });

    expect(el.textContent).toContain('Camilo Torres');
    await enviar();
    await responder(http.expectOne('/api/clientes'), { id: 6, terceroId: 20, nombre: 'Camilo Torres', preferencias: [] });
    expect(registrados[0]?.id).toBe(6);
  });

  it('pide la razón social cuando es una empresa', async () => {
    escribir('input[formControlName="numeroDocumento"]', '9001');
    await enviar();
    await responder(terceroPorDocumento('9001'), { codigo: 'NO_ENCONTRADO', mensaje: 'No existe.' }, 404);

    const tipo = el.querySelector<HTMLSelectElement>('select[formControlName="tipo"]')!;
    tipo.value = 'JURIDICA';
    tipo.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(el.querySelector('input[formControlName="razonSocial"]')).not.toBeNull();
    expect(el.querySelector('input[formControlName="nombres"]')).toBeNull();
  });

  it('muestra el error del backend sin cerrar el diálogo, por ejemplo si ya era cliente', async () => {
    escribir('input[formControlName="numeroDocumento"]', '1003');
    await enviar();
    await responder(terceroPorDocumento('1003'), SOFIA);
    await enviar();
    await responder(
      http.expectOne('/api/clientes'),
      { codigo: 'CLIENTE_EXISTENTE', mensaje: 'Ese tercero ya es cliente.' },
      409,
    );

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('CLIENTE_EXISTENTE');
    expect(registrados).toHaveLength(0);
    expect(cierres).toBe(0);
  });

  it('no repite la petición si se envía otra vez mientras la primera sigue en curso', async () => {
    escribir('input[formControlName="numeroDocumento"]', '1003');
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    el.querySelector('form')!.dispatchEvent(new Event('submit')); // por ejemplo, un segundo Enter

    await responder(terceroPorDocumento('1003'), SOFIA); // expectOne falla si hubiera dos
    expect(el.textContent).toContain('Sofía Mejía');
  });

  it('se cierra con Escape y con Cancelar', () => {
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(cierres).toBe(1);

    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Cancelar'))!.click();
    expect(cierres).toBe(2);
  });
});
