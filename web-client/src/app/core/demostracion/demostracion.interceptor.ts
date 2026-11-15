import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import type { ErrorApi } from '../api/tipos';
import { DemostracionServicio } from './demostracion.servicio';
import { ocultarSensibles } from './ocultar';

/**
 * Anota cada llamada para el modo demostración. Va **antes** del interceptor de errores para ver el resultado final
 * (los fallos ya como `ErrorApi`, con el código del backend). Apagado, no hace nada.
 */
export const demostracionInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const demo = inject(DemostracionServicio);
  if (!demo.activo()) {
    return siguiente(peticion);
  }
  const inicio = performance.now();
  const base = { metodo: peticion.method, url: peticion.urlWithParams, enviado: ocultarSensibles(peticion.body) };
  const duracionMs = () => Math.round(performance.now() - inicio);

  return siguiente(peticion).pipe(
    tap({
      next: (evento) => {
        if (evento instanceof HttpResponse) {
          demo.registrar({
            ...base,
            estado: evento.status,
            duracionMs: duracionMs(),
            codigo: null,
            recibido: ocultarSensibles(evento.body),
          });
        }
      },
      error: (fallo: unknown) => {
        const error = fallo as Partial<ErrorApi>;
        demo.registrar({
          ...base,
          estado: error.estado || null,
          duracionMs: duracionMs(),
          codigo: error.codigo ?? null,
          recibido: error.codigo ? { codigo: error.codigo, mensaje: error.mensaje } : null,
        });
      },
    }),
  );
};
