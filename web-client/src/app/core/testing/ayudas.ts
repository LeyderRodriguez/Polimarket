import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { Rol } from '../api/tipos';
import { SesionServicio } from '../auth/sesion.servicio';
import { erroresInterceptor } from '../http/errores.interceptor';

/** Mismos proveedores que la aplicación real, pero con un backend simulado que cada prueba controla. */
export const proveedoresDePrueba = () => [
  provideRouter([]),
  provideHttpClient(withInterceptors([erroresInterceptor])),
  provideHttpClientTesting(),
];

/** Deja la sesión abierta con ese rol, como si el backend hubiera reconocido la cookie. */
export async function iniciarComo(rol: Rol): Promise<void> {
  const sesion = TestBed.inject(SesionServicio);
  const http = TestBed.inject(HttpTestingController);
  const restaurando = sesion.restaurar();
  http.expectOne('/api/auth/me').flush({ id: 1, nombre: 'Persona de prueba', rol });
  await restaurando;
}
