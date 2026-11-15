import type { components } from './schema';

/**
 * Alias legibles de los tipos que genera `npm run api:types` a partir del backend.
 * Aquí no se define ningún campo a mano: si el backend cambia, cambia `schema.d.ts` y el compilador avisa.
 */
export type Perfil = components['schemas']['PerfilDto'];
export type SolicitudLogin = components['schemas']['SolicitudLogin'];
export type Rol = Perfil['rol'];
/** Un producto con su stock físico, lo reservado por ventas pendientes y lo disponible. Los montos viajan como texto. */
export type ProductoDisponible = components['schemas']['DisponibilidadDto'];

export type Cliente = components['schemas']['ClienteDto'];
export type SolicitudCliente = components['schemas']['SolicitudCliente'];
/** Un tercero es la persona o empresa con sus datos comunes; un cliente es un tercero con ese rol. */
export type Tercero = components['schemas']['TerceroDto'];
export type SolicitudTercero = components['schemas']['SolicitudTercero'];
export type TipoPersona = SolicitudTercero['tipo'];

export type SolicitudVenta = components['schemas']['SolicitudVenta'];
export type Venta = components['schemas']['VentaDto'];

/** Forma de todos los errores que devuelve el backend (`{ codigo, mensaje }`) más el estado HTTP. */
export interface ErrorApi {
  estado: number;
  codigo: string;
  mensaje: string;
}
