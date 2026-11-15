import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import type { ErrorApi } from '../api/tipos';
import { esperarTurno, iniciarComo, proveedoresDePrueba } from '../testing/ayudas';
import { SesionServicio } from './sesion.servicio';

describe('SesionServicio', () => {
  let sesion: SesionServicio;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: proveedoresDePrueba() });
    sesion = TestBed.inject(SesionServicio);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('guarda el perfil al iniciar sesión y manda las credenciales al backend', async () => {
    const entrada = sesion.iniciarSesion('ana@polimarket.test', 'clave');
    const peticion = http.expectOne('/api/auth/login');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ correo: 'ana@polimarket.test', contrasena: 'clave' });
    peticion.flush({ id: 4, nombre: 'Ana Pérez', rol: 'VENDEDOR' });

    expect((await entrada).rol).toBe('VENDEDOR');
    expect(sesion.autenticado()).toBe(true);
    expect(sesion.usuario()?.nombre).toBe('Ana Pérez');
  });

  it('rechaza las cuentas de Recursos Humanos y Bodega y no deja la sesión abierta', async () => {
    const resultado = sesion.iniciarSesion('rh@polimarket.test', 'clave').then(
      () => null,
      (error: ErrorApi) => error,
    );
    http.expectOne('/api/auth/login').flush({ id: 1, nombre: 'RH', rol: 'RECURSOS_HUMANOS' });
    await esperarTurno();
    http.expectOne('/api/auth/logout').flush(null, { status: 204, statusText: 'No Content' });

    expect(await resultado).toMatchObject({ codigo: 'ROL_NO_SOPORTADO' });
    expect(sesion.autenticado()).toBe(false);
  });

  it('recupera la sesión existente al restaurar', async () => {
    await iniciarComo('ENTREGAS');
    expect(sesion.usuario()?.rol).toBe('ENTREGAS');
  });

  it('queda sin sesión si el backend no reconoce la cookie al restaurar', async () => {
    const restaurando = sesion.restaurar();
    http
      .expectOne('/api/auth/me')
      .flush({ codigo: 'NO_AUTENTICADO', mensaje: 'Debe iniciar sesión.' }, { status: 401, statusText: 'Unauthorized' });
    await restaurando;

    expect(sesion.autenticado()).toBe(false);
  });

  it('cierra la sesión local aunque el servidor no responda, y vuelve al login', async () => {
    await iniciarComo('VENDEDOR');
    const ir = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    const cerrando = sesion.cerrarSesion();
    http.expectOne('/api/auth/logout').error(new ProgressEvent('error'));
    await cerrando;

    expect(sesion.autenticado()).toBe(false);
    expect(ir).toHaveBeenCalledWith('/login');
  });
});
