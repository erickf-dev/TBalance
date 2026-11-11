package sistemainventario.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import sistemainventario.exception.ValidationException;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Genera imágenes de códigos QR usando la librería ZXing.
 *
 * <p><b>SRP / Bajo acoplamiento:</b> es la única clase del sistema que conoce
 * ZXing. Si se cambia de librería, solo se modifica esta clase.</p>
 *
 * <p>El contenido se codifica en UTF-8 y con corrección de errores baja
 * (nivel L): así el código queda con menos módulos, más grandes, y la webcam
 * de un portátil lo lee desde más lejos. En las pruebas, un QR de 25 x 25
 * módulos se leyó a distancias donde uno de 41 x 41 fallaba.</p>
 */
public class QrGenerator {

    private static final int NEGRO = 0xFF000000;
    private static final int BLANCO = 0xFFFFFFFF;

    /**
     * @param contenido  texto que mostrará el celular al escanear el código
     * @param tamanoPx   ancho y alto de la imagen en píxeles
     * @return imagen del código QR
     * @throws ValidationException si el contenido está vacío o es demasiado largo
     */
    public BufferedImage generar(String contenido, int tamanoPx) {
        if (TextUtil.esVacio(contenido)) {
            throw new ValidationException("No hay datos para generar el codigo QR");
        }
        try {
            BitMatrix matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx, opciones());
            return aImagen(matriz);
        } catch (WriterException | IllegalArgumentException e) {
            throw new ValidationException("No se pudo generar el codigo QR: " + e.getMessage());
        }
    }

    private Map<EncodeHintType, Object> opciones() {
        Map<EncodeHintType, Object> opciones = new EnumMap<>(EncodeHintType.class);
        opciones.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        opciones.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        opciones.put(EncodeHintType.MARGIN, 1);
        return opciones;
    }

    private BufferedImage aImagen(BitMatrix matriz) {
        BufferedImage imagen = new BufferedImage(matriz.getWidth(), matriz.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matriz.getWidth(); x++) {
            for (int y = 0; y < matriz.getHeight(); y++) {
                imagen.setRGB(x, y, matriz.get(x, y) ? NEGRO : BLANCO);
            }
        }
        return imagen;
    }
}
