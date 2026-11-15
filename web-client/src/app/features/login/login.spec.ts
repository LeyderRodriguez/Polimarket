import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { esperarTurno, proveedoresDePrueba } from '../../core/testing/ayudas';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let el: HTMLElement;
  let http: HttpTestingController;

  const escribir = (id: string, valor: string) => {
    const campo = el.querySelector<HTMLInputElement>(`#${id}`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  };
  const enviar = async () => {
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({ imports: [Login], providers: proveedoresDePrueba() });
    fixture = TestBed.createComponent(Login);
    el = fixture.nativeElement as HTMLElement;
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('al elegir una cuenta de demostración llena el correo y la contraseña', async () => {
    const botones = [...el.querySelectorAll<HTMLButtonElement>('.chip')];
    expect(botones.length).toBeGreaterThanOrEqual(3);

    botones.find((b) => b.textContent?.includes('Entregas'))!.click();
    await fixture.whenStable();

    expect(el.querySelector<HTMLInputElement>('#correo')!.value).toBe('entregas@polimarket.test');
    expect(el.querySelector<HTMLInputElement>('#contrasena')!.value).not.toBe('');
  });

  it('no llama al backend si faltan datos, y lo explica', async () => {
    await enviar();
    fixture.detectChanges();

    http.expectNone('/api/auth/login');
    expect(el.textContent).toContain('Escriba un correo válido.');
    expect(el.textContent).toContain('Escriba su contraseña.');
  });

  it('muestra el código y el mensaje que devuelve el backend cuando la clave es incorrecta', async () => {
    escribir('correo', 'ana@polimarket.test');
    escribir('contrasena', 'mal');
    await enviar();
    http
      .expectOne('/api/auth/login')
      .flush({ codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'Correo o contraseña incorrectos.' }, { status: 401, statusText: 'Unauthorized' });
    await esperarTurno();
    fixture.detectChanges();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('CREDENCIALES_INVALIDAS');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Correo o contraseña incorrectos.');
  });

  it('lleva a la pantalla de inicio del rol cuando entra bien', async () => {
    const ir = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    escribir('correo', 'ana@polimarket.test');
    escribir('contrasena', 'clave');
    await enviar();
    http.expectOne('/api/auth/login').flush({ id: 4, nombre: 'Ana Pérez', rol: 'VENDEDOR' });
    await esperarTurno();

    expect(ir).toHaveBeenCalledWith('/disponibilidad');
  });
});
