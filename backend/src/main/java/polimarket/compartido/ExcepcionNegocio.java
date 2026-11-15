package polimarket.compartido;

/**
 * Error de negocio que cualquier componente puede lanzar. La capa HTTP lo traduce a un código y
 * un estado, así los componentes no dependen de HTTP.
 */
public class ExcepcionNegocio extends RuntimeException {

    public enum Categoria { DATOS_INVALIDOS, NO_AUTENTICADO, SIN_PERMISO, NO_ENCONTRADO, CONFLICTO }

    private final Categoria categoria;
    private final String codigo;

    public ExcepcionNegocio(Categoria categoria, String codigo, String mensaje) {
        super(mensaje);
        this.categoria = categoria;
        this.codigo = codigo;
    }

    public static ExcepcionNegocio datosInvalidos(String mensaje) {
        return new ExcepcionNegocio(Categoria.DATOS_INVALIDOS, "DATOS_INVALIDOS", mensaje);
    }

    public static ExcepcionNegocio noAutenticado(String codigo, String mensaje) {
        return new ExcepcionNegocio(Categoria.NO_AUTENTICADO, codigo, mensaje);
    }

    public static ExcepcionNegocio sinPermiso(String codigo, String mensaje) {
        return new ExcepcionNegocio(Categoria.SIN_PERMISO, codigo, mensaje);
    }

    public static ExcepcionNegocio noEncontrado(String mensaje) {
        return new ExcepcionNegocio(Categoria.NO_ENCONTRADO, "NO_ENCONTRADO", mensaje);
    }

    public static ExcepcionNegocio conflicto(String codigo, String mensaje) {
        return new ExcepcionNegocio(Categoria.CONFLICTO, codigo, mensaje);
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getCodigo() {
        return codigo;
    }
}
