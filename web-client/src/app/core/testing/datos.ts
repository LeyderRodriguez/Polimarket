import type { Cliente, ProductoDisponible } from '../api/tipos';

/** Respuestas de ejemplo del backend, compartidas por las pruebas de las pantallas. */
export const PRODUCTOS_DE_PRUEBA: ProductoDisponible[] = [
  { id: 1, codigo: 'ARR-01', nombre: 'Arroz', precio: '5000.00', stockFisico: 10, cantidadReservada: 2, cantidadDisponible: 8 },
  { id: 2, codigo: 'AZU-01', nombre: 'Azúcar', precio: '4200.00', stockFisico: 4, cantidadReservada: 4, cantidadDisponible: 0 },
  { id: 3, codigo: 'ACE-01', nombre: 'Aceite', precio: '12500.00', stockFisico: 20, cantidadReservada: 3, cantidadDisponible: 17 },
];

export const CLIENTES_DE_PRUEBA: Cliente[] = [
  { id: 1, terceroId: 11, nombre: 'María Pérez', preferencias: ['sin gluten'] },
  { id: 2, terceroId: 12, nombre: 'Carlos Ruiz', preferencias: [] },
];
