import { HttpClient } from '@angular/common/http';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import type { ErrorApi } from '../api/tipos';
import { SesionServicio } from '../auth/sesion.servicio';
import { iniciarComo, proveedoresDePrueba } from '../testing/ayudas';
import { aErrorApi } from './errores.interceptor';

describe('erroresInterceptor', () => {
  let cliente: HttpClient;
  let http: HttpTestingController;
  let ir: ReturnType<typeof vi.spyOn>;

  const fallar = () =>
    firstValueFrom(cliente.get('/api/productos')).then(
      () => null,
      (error: ErrorApi) => error,
    );

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: proveedoresDePrueba() });
    cliente = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    ir = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  it('convierte el cuerpo de error del backend en un ErrorApi', async () => {
    const resultado = fallar();
    http
      .expectOne('/api/productos')
      .flush({ codigo: 'STOCK_INSUFICIENTE', mensaje: 'Solo hay 4.' }, { status: 409, statusText: 'Conflict' });

    expect(await resultado).toEqual({ estado: 409, codigo: 'STOCK_INSUFICIENTE', mensaje: 'Solo hay 4.' });
  });

  it('avisa con un mensaje legible cuando no hay conexión', async () => {
    const resultado = fallar();
    http.expectOne('/api/productos').error(new ProgressEvent('error'));

    expect(await resultado).toMatchObject({ estado: 0, codigo: 'SIN_CONEXION' });
  });

  it('deja pasar un ErrorApi que ya viene normalizado y da un mensaje genérico a cualquier otro fallo', () => {
    const rechazo: ErrorApi = { estado: 403, codigo: 'ROL_NO_SOPORTADO', mensaje: 'No es para este cliente.' };
    expect(aErrorApi(rechazo)).toBe(rechazo);
    expect(aErrorApi(new Error('algo raro'))).toMatchObject({ codigo: 'ERROR_INESPERADO' });
  });

  it('no inventa un código cuando la respuesta de error no trae el formato del backend', async () => {
    const resultado = fallar();
    http.expectOne('/api/productos').flush('<html>Bad gateway</html>', { status: 502, statusText: 'Bad Gateway' });

    expect(await resultado).toMatchObject({ estado: 502, codigo: 'ERROR_INESPERADO' });
  });

  it('limpia la sesión y vuelve al login cuando caduca mientras se trabaja', async () => {
    await iniciarComo('VENDEDOR');
    const resultado = fallar();
    http
      .expectOne('/api/productos')
      .flush({ codigo: 'NO_AUTENTICADO', mensaje: 'Debe iniciar sesión.' }, { status: 401, statusText: 'Unauthorized' });
    await resultado;

    expect(TestBed.inject(SesionServicio).autenticado()).toBe(false);
    expect(ir).toHaveBeenCalledWith('/login');
  });

  it('no redirige si ya no había sesión (por ejemplo, un intento de login fallido)', async () => {
    const resultado = fallar();
    http
      .expectOne('/api/productos')
      .flush({ codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'Correo o contraseña incorrectos.' }, { status: 401, statusText: 'Unauthorized' });
    await resultado;

    expect(ir).not.toHaveBeenCalled();
  });
});
