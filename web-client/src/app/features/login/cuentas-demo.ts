/**
 * Cuentas que crea el backend con sus datos de demostración (`polimarket.demo.cargar-datos`, activo por defecto).
 * El proyecto no distingue entre desarrollo y producción, así que los botones del login se muestran siempre.
 */
export interface CuentaDemo {
  correo: string;
  etiqueta: string;
  detalle: string;
  /** Qué cliente usa la cuenta: las de escritorio (RH y Bodega) el cliente web las rechaza. */
  cliente: 'web' | 'escritorio';
}

/** Clave ficticia de todas las cuentas de demostración (backend: `polimarket.demo.clave`). */
export const CLAVE_DEMO = 'Demo-1234';

export const CUENTAS_DEMO: readonly CuentaDemo[] = [
  { correo: 'ana@polimarket.test', etiqueta: 'Ana · vendedora', detalle: 'Vendedora con aval aprobado: puede vender', cliente: 'web' },
  { correo: 'luis@polimarket.test', etiqueta: 'Luis · vendedor', detalle: 'Vendedor con aval pendiente: no puede vender', cliente: 'web' },
  { correo: 'entregas@polimarket.test', etiqueta: 'Entregas', detalle: 'Personal de entregas: despacha las ventas', cliente: 'web' },
  { correo: 'rh@polimarket.test', etiqueta: 'RH', detalle: 'Recursos Humanos: usa el cliente de escritorio, aquí se rechaza', cliente: 'escritorio' },
  { correo: 'bodega@polimarket.test', etiqueta: 'Bodega', detalle: 'Bodega: usa el cliente de escritorio, aquí se rechaza', cliente: 'escritorio' },
];
