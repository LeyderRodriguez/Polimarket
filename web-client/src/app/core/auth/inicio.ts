import type { Rol } from '../api/tipos';

/** Pantalla a la que se llega tras entrar, según el rol. */
export function inicioDe(rol: Rol): string {
  return rol === 'ENTREGAS' ? '/entregas' : '/disponibilidad';
}
