package polimarket.terceros.api;

public record TerceroDto(
        Long id,
        TipoTercero tipo,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String razonSocial,
        String telefono,
        String correo,
        String direccion) {

    /** Nombre para mostrar: persona natural o razón social. */
    public String nombreCompleto() {
        return tipo == TipoTercero.JURIDICA ? razonSocial : (nombres + " " + apellidos).trim();
    }
}
