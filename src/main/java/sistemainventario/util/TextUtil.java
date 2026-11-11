package sistemainventario.util;

import java.util.Locale;

/**
 * Utilidades puras para el manejo de texto.
 *
 * <p>Centraliza comprobaciones repetidas en el modelo (cadenas vacías y
 * búsquedas sin distinguir mayúsculas) para evitar duplicación de código.</p>
 */
public final class TextUtil {

    private TextUtil() {
    }

    /**
     * @param texto cadena a evaluar
     * @return {@code true} si es {@code null}, vacía o solo contiene espacios
     */
    public static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /**
     * Indica si {@code campo} contiene {@code consulta} ignorando mayúsculas.
     * Es tolerante a valores {@code null}.
     *
     * @param campo    texto donde se busca
     * @param consulta texto buscado (ya normalizado o no)
     * @return {@code true} si hay coincidencia
     */
    public static boolean contieneIgnorandoMayusculas(String campo, String consulta) {
        if (campo == null || consulta == null) {
            return false;
        }
        return normalizar(campo).contains(normalizar(consulta));
    }

    /**
     * @param texto texto de entrada
     * @return el texto sin espacios externos y en minúsculas; cadena vacía si es {@code null}
     */
    public static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * @param texto texto de entrada
     * @return el texto sin espacios externos, o {@code null} si era {@code null}
     */
    public static String recortar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
