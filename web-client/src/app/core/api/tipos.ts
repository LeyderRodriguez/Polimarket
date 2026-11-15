import type { components } from './schema';

/**
 * Alias legibles de los tipos que genera `npm run api:types` a partir del backend.
 * Aquí no se define ningún campo a mano: si el backend cambia, cambia `schema.d.ts` y el compilador avisa.
 */
export type Perfil = components['schemas']['PerfilDto'];
export type SolicitudLogin = components['schemas']['SolicitudLogin'];
export type Rol = Perfil['rol'];

/** Forma de todos los errores que devuelve el backend (`{ codigo, mensaje }`) más el estado HTTP. */
export interface ErrorApi {
  estado: number;
  codigo: string;
  mensaje: string;
}
