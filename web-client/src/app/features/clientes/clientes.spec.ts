import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { esperarTurno, proveedoresDePrueba } from '../../core/testing/ayudas';
import { CLIENTES_DE_PRUEBA } from '../../core/testing/datos';
import { RegistroCliente } from '../../shared/registro-cliente/registro-cliente';
import { Clientes } from './clientes';

describe('Clientes', () => {
  let fixture: ComponentFixture<Clientes>;
  let el: HTMLElement;
  let http: HttpTestingController;

  const filas = () => [...el.querySelectorAll('tbody tr')].map((f) => f.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  const refrescar = async () => {
    await esperarTurno();
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [Clientes], providers: proveedoresDePrueba() });
    fixture = TestBed.createComponent(Clientes);
    el = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('lista los clientes con sus preferencias', async () => {
    http.expectOne('/api/clientes').flush(CLIENTES_DE_PRUEBA);
    await refrescar();

    expect(filas()).toHaveLength(2);
    expect(filas()[0]).toContain('María Pérez');
    expect(filas()[0]).toContain('sin gluten');
    expect(filas()[1]).toContain('Carlos Ruiz');
    expect(filas()[1]).toContain('—');
  });

  it('muestra el error del backend y no deja datos viejos', async () => {
    http
      .expectOne('/api/clientes')
      .flush({ codigo: 'VENDEDOR_SIN_AVAL', mensaje: 'El vendedor no tiene aval vigente.' }, { status: 403, statusText: 'Forbidden' });
    await refrescar();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('VENDEDOR_SIN_AVAL');
    expect(filas()[0]).toContain('No se pudieron cargar los clientes.');
  });

  it('al registrar un cliente cierra el diálogo, lo avisa y recarga la lista', async () => {
    http.expectOne('/api/clientes').flush(CLIENTES_DE_PRUEBA);
    await refrescar();

    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Registrar cliente'))!.click();
    fixture.detectChanges();
    const dialogo = fixture.debugElement.query(By.directive(RegistroCliente));
    expect(dialogo).not.toBeNull();

    (dialogo.componentInstance as RegistroCliente).registrado.emit({ id: 3, terceroId: 13, nombre: 'Sofía Mejía', preferencias: [] });
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.directive(RegistroCliente))).toBeNull();
    expect(el.querySelector('[role="status"]')?.textContent).toContain('Sofía Mejía');

    http.expectOne('/api/clientes').flush([...CLIENTES_DE_PRUEBA, { id: 3, terceroId: 13, nombre: 'Sofía Mejía', preferencias: [] }]);
    await refrescar();
    expect(filas()).toHaveLength(3);
  });
});
