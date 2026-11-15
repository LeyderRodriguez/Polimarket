const formatoPesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

/**
 * Muestra un monto que llegó como texto («5000.00») en pesos colombianos («$ 5.000»).
 * Solo sirve para mostrar: con los montos nunca se calcula en el cliente, los totales los calcula el backend.
 */
export function pesos(monto: string): string {
  return formatoPesos.format(Number(monto));
}

/** Minúsculas y sin tildes, para que «azucar» encuentre «Azúcar». */
export function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
}
