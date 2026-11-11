package sistemainventario.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * Convierte imágenes en texto que la vista web puede mostrar directamente
 * en una etiqueta {@code <img>}.
 */
public final class ImageEncoder {

    private ImageEncoder() {
    }

    /**
     * Reduce la imagen si es más ancha que el máximo y la codifica en JPEG,
     * un formato liviano para enviar varios cuadros de cámara por segundo.
     *
     * @param imagen      imagen original
     * @param anchoMaximo ancho máximo en píxeles
     * @return URI {@code data:image/jpeg;base64,...}
     */
    public static String aJpegDataUri(BufferedImage imagen, int anchoMaximo) {
        double escala = Math.min(1.0, (double) anchoMaximo / imagen.getWidth());
        int ancho = Math.max(1, (int) Math.round(imagen.getWidth() * escala));
        int alto = Math.max(1, (int) Math.round(imagen.getHeight() * escala));
        BufferedImage rgb = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(imagen, 0, 0, ancho, alto, null);
        g.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            ImageIO.write(rgb, "jpg", salida);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
    }
}
