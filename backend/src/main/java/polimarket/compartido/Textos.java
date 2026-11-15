package polimarket.compartido;

/** Utilidades de texto compartidas por los componentes. */
public final class Textos {

    private Textos() {
    }

    public static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    /** Recorta el texto; un texto vacío o nulo queda como nulo. */
    public static String limpiar(String s) {
        return vacio(s) ? null : s.trim();
    }
}
