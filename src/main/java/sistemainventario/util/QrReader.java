package sistemainventario.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lee el texto de un código QR que aparece en una imagen (por ejemplo, un
 * cuadro de la cámara).
 *
 * <p><b>Bajo acoplamiento:</b> junto con {@link QrGenerator} es la única clase
 * que conoce la librería ZXing. <b>SRP:</b> solo decodifica; decidir qué
 * producto representa el texto le corresponde a otra clase.</p>
 */
public class QrReader {

    private final Map<DecodeHintType, Object> pistas = new EnumMap<>(DecodeHintType.class);

    public QrReader() {
        pistas.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
        pistas.put(DecodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        pistas.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }

    /**
     * @param imagen imagen donde se busca el código
     * @return el texto del QR, o vacío si la imagen no contiene uno legible
     */
    public Optional<String> leer(BufferedImage imagen) {
        if (imagen == null) {
            return Optional.empty();
        }
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        int[] pixeles = imagen.getRGB(0, 0, ancho, alto, null, 0, ancho);
        LuminanceSource fuente = new RGBLuminanceSource(ancho, alto, pixeles);
        BinaryBitmap mapa = new BinaryBitmap(new HybridBinarizer(fuente));
        try {
            Result resultado = new MultiFormatReader().decode(mapa, pistas);
            return Optional.of(resultado.getText());
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }
}
