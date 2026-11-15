import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SesionServicio } from '../auth/sesion.servicio';
import { aErrorApi } from './error-api';

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
