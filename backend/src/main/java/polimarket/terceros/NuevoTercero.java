package polimarket.terceros;

/** Datos para registrar un tercero. Natural: nombres y apellidos. Jurídica: razón social. */
public record NuevoTercero(
        TipoTercero tipo,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String razonSocial,
        String telefono,
        String correo,
        String direccion) {
}
