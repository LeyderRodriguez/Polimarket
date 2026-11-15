import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import type { ErrorApi } from '../api/tipos';
import { SesionServicio } from '../auth/sesion.servicio';

function esErrorApi(valor: unknown): valor is ErrorApi {
  const candidato = valor as Partial<ErrorApi> | null;
  return (
    typeof candidato === 'object' &&
    candidato !== null &&
    typeof candidato.estado === 'number' &&
    typeof candidato.codigo === 'string' &&
    typeof candidato.mensaje === 'string'
  );
}

/** Convierte cualquier fallo HTTP en el `ErrorApi` que usa toda la aplicación. */
export function aErrorApi(error: unknown): ErrorApi {
  if (esErrorApi(error)) {
    return error; // ya viene normalizado (por ejemplo, el que lanza SesionServicio)
  }
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return {
        estado: 0,
        codigo: 'SIN_CONEXION',
        mensaje: 'No se pudo conectar con el servidor. Revise que el backend esté en marcha.',
      };
    }
    const cuerpo = error.error as Partial<ErrorApi> | null;
    if (cuerpo && typeof cuerpo === 'object' && typeof cuerpo.codigo === 'string') {
      return { estado: error.status, codigo: cuerpo.codigo, mensaje: cuerpo.mensaje ?? '' };
    }
    return { estado: error.status, codigo: 'ERROR_INESPERADO', mensaje: 'El servidor respondió con un error inesperado.' };
  }
  return { estado: 0, codigo: 'ERROR_INESPERADO', mensaje: 'Ocurrió un error inesperado.' };
}

/**
 * Un solo lugar para los errores de la API: los pasa a `ErrorApi` y, si la sesión caducó mientras la persona
 * trabajaba, la limpia y la devuelve al login. El 401 de «credenciales inválidas» no entra aquí porque ocurre sin sesión.
 */
export const erroresInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const sesion = inject(SesionServicio);
  const router = inject(Router);
  return siguiente(peticion).pipe(
    catchError((fallo: unknown) => {
      const error = aErrorApi(fallo);
      const sesionCaducada =
        error.estado === 401 && (error.codigo === 'NO_AUTENTICADO' || error.codigo === 'CUENTA_DESACTIVADA');
      if (sesionCaducada && sesion.autenticado()) {
        sesion.limpiar();
        void router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};
