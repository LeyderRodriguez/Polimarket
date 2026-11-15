import { HttpErrorResponse } from '@angular/common/http';
import { TimeoutError } from 'rxjs';
import type { ErrorApi } from '../api/tipos';

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

/** Convierte cualquier fallo (HTTP, sin conexión, tiempo agotado) en el `ErrorApi` que usa toda la aplicación. */
export function aErrorApi(error: unknown): ErrorApi {
  if (esErrorApi(error)) {
    return error; // ya viene normalizado (por ejemplo, el que lanza SesionServicio)
  }
  if (error instanceof TimeoutError) {
    return { estado: 0, codigo: 'SIN_RESPUESTA', mensaje: 'El servidor tardó demasiado en responder.' };
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
