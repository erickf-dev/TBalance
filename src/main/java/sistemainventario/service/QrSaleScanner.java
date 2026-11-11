package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Product;
import sistemainventario.util.QrReader;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Caso de uso "registrar venta por QR": convierte un cuadro de la cámara en
 * el producto cuya etiqueta aparece en él.
 *
 * <p><b>Alta cohesión:</b> leer el QR lo hace {@link QrReader}, interpretar
 * el texto lo hace {@link Product#idDesdeEtiquetaQR(String)} y buscar el
 * producto lo hace {@link InventoryService}; esta clase solo los coordina y
 * evita que una etiqueta quieta frente a la cámara se cuente muchas veces.</p>
 */
public class QrSaleScanner {

    /**
     * Tiempo que una etiqueta debe desaparecer de la cámara para volver a
     * contarse; así una etiqueta quieta no suma una unidad por cada cuadro.
     */
    public static final long ESPERA_REPETIDO_MS = 2000;

    private final QrReader lector;
    private final LongSupplier reloj;
    private String ultimoContenido;
    private long ultimaLectura;

    public QrSaleScanner(QrReader lector) {
        this(lector, System::currentTimeMillis);
    }

    /**
     * @param lector lector de códigos QR
     * @param reloj  fuente de la hora en milisegundos (se reemplaza en pruebas)
     */
    public QrSaleScanner(QrReader lector, LongSupplier reloj) {
        this.lector = lector;
        this.reloj = reloj;
    }

    /**
     * @param cuadro     imagen tomada de la cámara
     * @param inventario inventario del perfil activo
     * @return el producto de la etiqueta, o vacío si no hay un QR nuevo en el cuadro
     * @throws ValidationException si el QR no es de TBalance o el producto no existe
     */
    public Optional<Product> procesar(BufferedImage cuadro, InventoryService inventario) {
        Optional<String> leido = lector.leer(cuadro);
        if (leido.isEmpty() || esRepeticion(leido.get())) {
            return Optional.empty();
        }
        String id = Product.idDesdeEtiquetaQR(leido.get())
                .orElseThrow(() -> new ValidationException("Ese codigo QR no es una etiqueta de TBalance"));
        Product producto = inventario.buscarPorId(id)
                .orElseThrow(() -> new ValidationException("El producto de esta etiqueta no existe en el perfil activo"));
        return Optional.of(producto);
    }

    /**
     * Registra la lectura y dice si es la misma etiqueta que sigue frente a
     * la cámara desde la lectura anterior.
     */
    private boolean esRepeticion(String contenido) {
        long ahora = reloj.getAsLong();
        boolean repetida = contenido.equals(ultimoContenido) && ahora - ultimaLectura < ESPERA_REPETIDO_MS;
        ultimoContenido = contenido;
        ultimaLectura = ahora;
        return repetida;
    }
}
