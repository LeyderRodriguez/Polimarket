const SENSIBLE = /contrase|password|clave|token/i;

/** Copia del valor con los campos sensibles (contraseñas, tokens) reemplazados, para poder mostrarlo en pantalla. */
export function ocultarSensibles(valor: unknown): unknown {
  if (Array.isArray(valor)) {
    return valor.map(ocultarSensibles);
  }
  if (valor !== null && typeof valor === 'object') {
    return Object.fromEntries(
      Object.entries(valor).map(([campo, dato]) => [campo, SENSIBLE.test(campo) ? '••••' : ocultarSensibles(dato)]),
    );
  }
  return valor;
}
