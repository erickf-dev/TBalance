package sistemainventario.model;

import java.util.Optional;

/**
 * Datos que se leen de una etiqueta QR de TBalance.
 *
 * <p>Formato del texto (una línea por dato, el nombre no va porque ya está
 * impreso al lado del código):</p>
 * <pre>
 * TBalance
 * ID: 1001
 * N: 2001
 * $ 2.500
 * </pre>
 * <p>La línea {@code N:} es el negocio dueño de la etiqueta. Las etiquetas
 * impresas antes de la versión 3.5 no la tienen y se siguen aceptando.</p>
 *
 * @param productoId ID del producto
 * @param perfilId   ID del negocio, o {@code null} en las etiquetas antiguas
 * @param precio     precio impreso (por ejemplo {@code $ 2.500}), o {@code null}
 */
public record QrLabel(String productoId, String perfilId, String precio) {

    /** Primera línea de toda etiqueta QR de TBalance. */
    public static final String ENCABEZADO = "TBalance";
    private static final String PREFIJO_ID = "ID:";
    private static final String PREFIJO_NEGOCIO = "N:";
    private static final String PREFIJO_PRECIO = "$";

    /**
     * @param producto producto de la etiqueta
     * @param perfilId negocio dueño del producto (puede ser {@code null})
     * @return texto que se codifica en el QR
     */
    public static String texto(Product producto, String perfilId) {
        StringBuilder texto = new StringBuilder(ENCABEZADO).append('\n')
                .append(PREFIJO_ID).append(' ').append(producto.getId()).append('\n');
        if (perfilId != null && !perfilId.isBlank()) {
            texto.append(PREFIJO_NEGOCIO).append(' ').append(perfilId).append('\n');
        }
        return texto.append(producto.getPrecioVentaFormateado()).toString();
    }

    /**
     * @param contenido texto leído del código QR
     * @return los datos de la etiqueta, o vacío si no es una etiqueta de TBalance
     *         o le falta el ID (etiqueta dañada o incompleta)
     */
    public static Optional<QrLabel> leer(String contenido) {
        if (contenido == null || !contenido.strip().startsWith(ENCABEZADO)) {
            return Optional.empty();
        }
        String id = null;
        String negocio = null;
        String precio = null;
        for (String linea : contenido.strip().split("\\R")) {
            String valor = linea.strip();
            if (valor.startsWith(PREFIJO_ID) && id == null) {
                id = valor.substring(PREFIJO_ID.length()).strip();
            } else if (valor.startsWith(PREFIJO_NEGOCIO) && negocio == null) {
                negocio = valor.substring(PREFIJO_NEGOCIO.length()).strip();
            } else if (valor.startsWith(PREFIJO_PRECIO) && precio == null) {
                precio = valor;
            }
        }
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new QrLabel(id, negocio == null || negocio.isEmpty() ? null : negocio, precio));
    }

    /**
     * @param texto texto leído del código QR
     * @return {@code true} si empieza como una etiqueta de TBalance (aunque esté incompleta)
     */
    public static boolean pareceDeTBalance(String texto) {
        return texto != null && texto.strip().startsWith(ENCABEZADO);
    }

    /**
     * @param perfilActivo negocio abierto en la aplicación
     * @return {@code true} si la etiqueta es de otro negocio (las antiguas, sin negocio, no lo son)
     */
    public boolean esDeOtroNegocio(String perfilActivo) {
        return perfilId != null && perfilActivo != null && !perfilId.equals(perfilActivo);
    }
}
