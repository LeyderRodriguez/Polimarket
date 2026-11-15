import { HttpClient } from '@angular/common/http';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { proveedoresDePrueba } from '../testing/ayudas';
import { DemostracionServicio } from './demostracion.servicio';
import { ocultarSensibles } from './ocultar';

describe('ocultarSensibles', () => {
  it('reemplaza contraseñas y tokens, también dentro de objetos y listas, sin tocar el original', () => {
    const original = { correo: 'a@b.c', contrasena: 'secreta', items: [{ token: 'x', cantidad: 2 }] };

    expect(ocultarSensibles(original)).toEqual({
      correo: 'a@b.c',
      contrasena: '••••',
      items: [{ token: '••••', cantidad: 2 }],
    });
    expect(original.contrasena).toBe('secreta');
  });

  it('deja intactos los valores simples', () => {
    expect(ocultarSensibles(null)).toBeNull();
    expect(ocultarSensibles('hola')).toBe('hola');
  });
});

describe('Modo demostración', () => {
  let demo: DemostracionServicio;
  let http: HttpTestingController;
  let cliente: HttpClient;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: proveedoresDePrueba() });
    demo = TestBed.inject(DemostracionServicio);
    http = TestBed.inject(HttpTestingController);
    cliente = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('empieza apagado y no guarda ninguna llamada', async () => {
    const respuesta = firstValueFrom(cliente.get('/api/productos'));
    http.expectOne('/api/productos').flush([]);
    await respuesta;

    expect(demo.activo()).toBe(false);
    expect(demo.llamadas()).toEqual([]);
  });

  it('encendido, anota método, URL, estado y la respuesta, la más reciente primero', async () => {
    demo.alternar();
    const primera = firstValueFrom(cliente.get('/api/productos'));
    http.expectOne('/api/productos').flush([{ id: 1 }]);
    await primera;
    const segunda = firstValueFrom(cliente.post('/api/ventas', { clienteId: 3 }));
    http.expectOne('/api/ventas').flush({ ventaId: 9 }, { status: 201, statusText: 'Created' });
    await segunda;

    const [reciente, antigua] = demo.llamadas();
    expect(reciente).toMatchObject({ metodo: 'POST', url: '/api/ventas', estado: 201, enviado: { clienteId: 3 }, recibido: { ventaId: 9 } });
    expect(antigua).toMatchObject({ metodo: 'GET', url: '/api/productos', estado: 200, codigo: null });
  });

  it('anota los fallos con el código que devolvió el backend', async () => {
    demo.alternar();
    const intento = firstValueFrom(cliente.get('/api/clientes')).catch(() => undefined);
    http
      .expectOne('/api/clientes')
      .flush({ codigo: 'VENDEDOR_SIN_AVAL', mensaje: 'Sin aval.' }, { status: 403, statusText: 'Forbidden' });
    await intento;

    expect(demo.llamadas()[0]).toMatchObject({
      estado: 403,
      codigo: 'VENDEDOR_SIN_AVAL',
      recibido: { codigo: 'VENDEDOR_SIN_AVAL', mensaje: 'Sin aval.' },
    });
  });

  it('anota sin estado cuando el backend no responde', async () => {
    demo.alternar();
    const intento = firstValueFrom(cliente.get('/api/productos')).catch(() => undefined);
    http.expectOne('/api/productos').error(new ProgressEvent('error'));
    await intento;

    expect(demo.llamadas()[0]).toMatchObject({ estado: null, codigo: 'SIN_CONEXION' });
  });

  it('nunca guarda la contraseña que se envía', async () => {
    demo.alternar();
    const intento = firstValueFrom(cliente.post('/api/auth/login', { correo: 'a@b.c', contrasena: 'secreta' }));
    http.expectOne('/api/auth/login').flush({ id: 1 });
    await intento;

    expect(JSON.stringify(demo.llamadas())).not.toContain('secreta');
    expect(demo.llamadas()[0]?.enviado).toEqual({ correo: 'a@b.c', contrasena: '••••' });
  });

  it('guarda solo las últimas 50', () => {
    demo.alternar();
    for (let i = 0; i < 60; i++) {
      demo.registrar({ metodo: 'GET', url: `/api/${i}`, estado: 200, duracionMs: 1, codigo: null, enviado: null, recibido: null });
    }

    expect(demo.llamadas()).toHaveLength(50);
    expect(demo.llamadas()[0]?.url).toBe('/api/59');
  });

  it('al apagarlo borra el registro y se recuerda en el navegador', () => {
    demo.alternar();
    demo.registrar({ metodo: 'GET', url: '/api/x', estado: 200, duracionMs: 1, codigo: null, enviado: null, recibido: null });
    expect(localStorage.getItem('polimarket.demostracion')).toBe('true');

    demo.alternar();

    expect(demo.llamadas()).toEqual([]);
    expect(localStorage.getItem('polimarket.demostracion')).toBe('false');
  });
});
