package polimarket.recursoshumanos;

/**
 * Contrato que otros componentes usan para saber si un vendedor puede operar. El aval vigente es
 * el último registrado: si está APROBADO el vendedor está autorizado.
 */
public interface IAutorizacion {

    /** Verdadero solo si el último aval del vendedor está APROBADO. Un vendedor inexistente no está autorizado. */
    boolean estaAutorizado(long vendedorId);

    /** Lanza NO_ENCONTRADO si el vendedor no existe. */
    AvalDto consultarAval(long vendedorId);

    /** Aprueba el aval. El responsable es el tercero (empleado) autenticado, no un dato del cliente. */
    AvalDto aprobar(long vendedorId, long responsableTerceroId, String observacion);

    /** Rechaza el aval, o lo revoca si estaba aprobado. */
    AvalDto rechazar(long vendedorId, long responsableTerceroId, String observacion);
}
