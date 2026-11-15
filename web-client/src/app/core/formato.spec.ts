import { normalizar, pesos } from './formato';

describe('formato', () => {
  it('muestra los montos en pesos colombianos sin decimales', () => {
    // Intl separa el símbolo con un espacio que no se rompe; se normaliza para comparar.
    expect(pesos('5000.00').replace(/\s/g, ' ')).toBe('$ 5.000');
    expect(pesos('12500.00').replace(/\s/g, ' ')).toBe('$ 12.500');
  });

  it('ignora tildes y mayúsculas al buscar', () => {
    expect(normalizar('Azúcar')).toBe('azucar');
    expect(normalizar('ARROZ')).toBe('arroz');
  });
});
