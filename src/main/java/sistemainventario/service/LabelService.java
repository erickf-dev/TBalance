package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Product;
import sistemainventario.repository.LabelRepository;
import sistemainventario.util.QrGenerator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Genera etiquetas con código QR para los productos: una etiqueta individual
 * (vista previa o imagen) y hojas listas para imprimir.
 *
 * <p><b>SRP / Bajo acoplamiento:</b> dibuja las etiquetas; delega la creación
 * del QR en {@link QrGenerator} y el guardado en {@link LabelRepository},
 * ambos recibidos por constructor. El texto del QR lo arma el propio
 * {@link Product} (Experto).</p>
 */
public class LabelService {

    /** Tamaño de una etiqueta en píxeles (a 150 ppp equivale a 6,8 x 4 cm). */
    public static final int ANCHO_ETIQUETA = 400;
    public static final int ALTO_ETIQUETA = 236;

    /** Hoja tamaño carta a 150 ppp: 3 columnas x 6 filas = 18 etiquetas con espacio para recortar. */
    private static final int ANCHO_HOJA = 1275;
    private static final int ALTO_HOJA = 1650;
    private static final int COLUMNAS = 3;
    private static final int FILAS = 6;
    private static final int MARGEN_HOJA = 25;

    private static final int TAMANO_QR = 216;
    private static final Color COLOR_MARCA = new Color(0xC0, 0x6A, 0x10);
    private static final Color COLOR_TEXTO = new Color(0x11, 0x18, 0x27);
    private static final Color COLOR_SECUNDARIO = new Color(0x6B, 0x72, 0x80);
    private static final Color COLOR_BORDE = new Color(0xD1, 0xD5, 0xDB);
    private static final DateTimeFormatter FORMATO_NOMBRE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final QrGenerator generadorQR;
    private final LabelRepository etiquetas;

    public LabelService(QrGenerator generadorQR, LabelRepository etiquetas) {
        this.generadorQR = Objects.requireNonNull(generadorQR);
        this.etiquetas = Objects.requireNonNull(etiquetas);
    }

    /**
     * @param producto producto a etiquetar
     * @return la etiqueta como imagen Base64 para mostrarla en la vista
     */
    public String vistaPrevia(Product producto) {
        return etiquetas.aDataUri(dibujarEtiqueta(producto));
    }

    /**
     * Guarda la etiqueta de un producto como imagen PNG.
     *
     * @param producto producto a etiquetar
     * @return ruta del archivo creado
     */
    public Path guardarEtiqueta(Product producto) {
        String nombre = "etiqueta_" + limpiar(producto.getId()) + "_" + marcaDeTiempo() + ".png";
        return etiquetas.guardar(nombre, dibujarEtiqueta(producto));
    }

    /**
     * Genera hojas tamaño carta con 18 etiquetas cada una, listas para imprimir.
     *
     * @param productos productos a etiquetar
     * @return rutas de las hojas creadas (una por cada 18 productos)
     * @throws ValidationException si no hay productos
     */
    public List<Path> guardarHojas(List<Product> productos) {
        if (productos == null || productos.isEmpty()) {
            throw new ValidationException("No hay productos para generar etiquetas");
        }
        String marca = marcaDeTiempo();
        int porHoja = COLUMNAS * FILAS;
        List<Path> hojas = new ArrayList<>();
        for (int inicio = 0, numero = 1; inicio < productos.size(); inicio += porHoja, numero++) {
            List<Product> grupo = productos.subList(inicio, Math.min(inicio + porHoja, productos.size()));
            hojas.add(etiquetas.guardar("etiquetas_" + marca + "_hoja" + numero + ".png", dibujarHoja(grupo)));
        }
        return hojas;
    }

    /** @return carpeta donde se guardan las etiquetas */
    public Path getCarpeta() {
        return etiquetas.getCarpeta();
    }

    /**
     * Dibuja una etiqueta: código QR a la izquierda; nombre, precio, ID y
     * categoría a la derecha.
     *
     * @param producto producto a etiquetar
     * @return imagen de la etiqueta
     */
    public BufferedImage dibujarEtiqueta(Product producto) {
        BufferedImage imagen = new BufferedImage(ANCHO_ETIQUETA, ALTO_ETIQUETA, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        try {
            prepararLienzo(g, ANCHO_ETIQUETA, ALTO_ETIQUETA);
            dibujarEn(g, producto, 0, 0);
        } finally {
            g.dispose();
        }
        return imagen;
    }

    private BufferedImage dibujarHoja(List<Product> productos) {
        BufferedImage hoja = new BufferedImage(ANCHO_HOJA, ALTO_HOJA, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = hoja.createGraphics();
        try {
            prepararLienzo(g, ANCHO_HOJA, ALTO_HOJA);
            int espacioX = (ANCHO_HOJA - 2 * MARGEN_HOJA - COLUMNAS * ANCHO_ETIQUETA) / (COLUMNAS - 1);
            int espacioY = (ALTO_HOJA - 2 * MARGEN_HOJA - FILAS * ALTO_ETIQUETA) / (FILAS - 1);
            for (int i = 0; i < productos.size(); i++) {
                int x = MARGEN_HOJA + (i % COLUMNAS) * (ANCHO_ETIQUETA + espacioX);
                int y = MARGEN_HOJA + (i / COLUMNAS) * (ALTO_ETIQUETA + espacioY);
                dibujarEn(g, productos.get(i), x, y);
            }
        } finally {
            g.dispose();
        }
        return hoja;
    }

    private void prepararLienzo(Graphics2D g, int ancho, int alto) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, ancho, alto);
    }

    private void dibujarEn(Graphics2D g, Product producto, int x, int y) {
        g.setColor(COLOR_BORDE);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x + 1, y + 1, ANCHO_ETIQUETA - 3, ALTO_ETIQUETA - 3, 18, 18);

        int margenQr = (ALTO_ETIQUETA - TAMANO_QR) / 2;
        g.drawImage(generadorQR.generar(producto.contenidoEtiquetaQR(), TAMANO_QR), x + margenQr, y + margenQr, null);

        int textoX = x + margenQr + TAMANO_QR + 12;
        int anchoTexto = x + ANCHO_ETIQUETA - textoX - 14;
        int lineaY = y + 52;
        g.setColor(COLOR_TEXTO);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        for (String linea : partirEnLineas(producto.getEtiqueta(), g.getFontMetrics(), anchoTexto, 3)) {
            g.drawString(linea, textoX, lineaY);
            lineaY += 23;
        }
        g.setColor(COLOR_MARCA);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        g.drawString(producto.getPrecioVentaFormateado(), textoX, y + ALTO_ETIQUETA - 62);
        g.setColor(COLOR_SECUNDARIO);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.drawString("ID " + producto.getId(), textoX, y + ALTO_ETIQUETA - 38);
        if (producto.getCategoria() != null) {
            List<String> categoria = partirEnLineas(producto.getCategoria(), g.getFontMetrics(), anchoTexto, 1);
            g.drawString(categoria.get(0), textoX, y + ALTO_ETIQUETA - 18);
        }
    }

    /** Parte un texto en líneas que caben en el ancho dado; recorta con "..." si sobra. */
    private List<String> partirEnLineas(String texto, FontMetrics medidas, int ancho, int maxLineas) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : (texto == null ? "" : texto.trim()).split("\\s+")) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (medidas.stringWidth(prueba) <= ancho || actual.length() == 0) {
                actual = new StringBuilder(prueba);
            } else {
                lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            }
        }
        lineas.add(actual.toString());
        if (lineas.size() > maxLineas) {
            lineas = new ArrayList<>(lineas.subList(0, maxLineas));
            lineas.set(maxLineas - 1, recortar(lineas.get(maxLineas - 1) + "...", medidas, ancho));
        }
        lineas.replaceAll(linea -> recortar(linea, medidas, ancho));
        return lineas;
    }

    private String recortar(String linea, FontMetrics medidas, int ancho) {
        String resultado = linea;
        while (resultado.length() > 4 && medidas.stringWidth(resultado) > ancho) {
            resultado = resultado.substring(0, resultado.length() - 4) + "...";
        }
        return resultado;
    }

    private static String marcaDeTiempo() {
        return LocalDateTime.now().format(FORMATO_NOMBRE);
    }

    private static String limpiar(String texto) {
        return texto == null ? "sin-id" : texto.replaceAll("[^A-Za-z0-9_\\-]", "-");
    }
}
