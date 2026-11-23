package sistemainventario.service;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.Product;
import sistemainventario.model.QrLabel;
import sistemainventario.util.QrReader;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Caso de uso "registrar venta por QR": convierte un cuadro de la cámara en
 * el producto cuya etiqueta aparece en él.
 *
 * <p><b>Alta cohesión:</b> leer el QR lo hace {@link QrReader}, interpretar
 * el texto lo hace {@link QrLabel} y buscar el
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
    private Function<String, Optional<String>> nombreNegocio = id -> Optional.empty();
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
     * @param nombres da el nombre de un negocio a partir de su ID, para que el
     *                mensaje de "etiqueta de otro negocio" diga cuál es
     * @return este escáner
     */
    public QrSaleScanner conNombresDeNegocio(Function<String, Optional<String>> nombres) {
        this.nombreNegocio = nombres;
        return this;
    }

    /**
     * Producto reconocido en una etiqueta.
     *
     * @param producto producto de la etiqueta
     * @param aviso    advertencia para el usuario (por ejemplo, el precio impreso
     *                 ya no es el actual), o {@code null}
     */
    public record Lectura(Product producto, String aviso) {
    }

    /**
     * Valida la etiqueta antes de agregar el producto a la venta: que sea de
     * TBalance, que esté completa, que sea del negocio abierto, que el producto
     * exista, esté habilitado y tenga unidades.
     *
     * @param cuadro       imagen tomada de la cámara
     * @param inventario   inventario del perfil activo
     * @param perfilActivo ID del negocio abierto
     * @return el producto de la etiqueta, o vacío si no hay un QR nuevo en el cuadro
     * @throws ValidationException si la etiqueta no sirve para esta venta (el mensaje dice por qué)
     */
    public Optional<Lectura> procesar(BufferedImage cuadro, InventoryService inventario, String perfilActivo) {
        Optional<String> leido = lector.leer(cuadro);
        if (leido.isEmpty() || esRepeticion(leido.get())) {
            return Optional.empty();
        }
        return Optional.of(validar(leido.get(), inventario, perfilActivo));
    }

    /**
     * Interpreta y valida el texto de una etiqueta (separado de la cámara para probarlo).
     *
     * @param texto        texto leído del QR
     * @param inventario   inventario del perfil activo
     * @param perfilActivo ID del negocio abierto
     * @return el producto y, si hace falta, un aviso
     * @throws ValidationException si la etiqueta no sirve para esta venta
     */
    public Lectura validar(String texto, InventoryService inventario, String perfilActivo) {
        if (!QrLabel.pareceDeTBalance(texto)) {
            throw new ValidationException("Ese código QR no es una etiqueta de TBalance");
        }
        QrLabel etiqueta = QrLabel.leer(texto).orElseThrow(() -> new ValidationException(
                "La etiqueta está incompleta o dañada: vuelva a imprimirla desde Productos"));
        if (etiqueta.esDeOtroNegocio(perfilActivo)) {
            String negocio = nombreNegocio.apply(etiqueta.perfilId()).orElse(etiqueta.perfilId());
            throw new ValidationException("Esta etiqueta es de otro negocio (" + negocio
                    + "). Cambie de negocio o use la etiqueta correcta");
        }
        Product producto = inventario.buscarPorId(etiqueta.productoId())
                .orElseThrow(() -> new ValidationException("El producto " + etiqueta.productoId()
                        + " de esta etiqueta no existe en este negocio"));
        if (!producto.isActivo()) {
            throw new ValidationException("El producto " + producto.getNombre() + " está deshabilitado");
        }
        if (producto.estaAgotado()) {
            throw new ValidationException(producto.getNombre() + " está agotado");
        }
        String aviso = null;
        if (etiqueta.precio() != null && !etiqueta.precio().equals(producto.getPrecioVentaFormateado())) {
            aviso = "El precio cambió: la etiqueta dice " + etiqueta.precio() + " y ahora es "
                    + producto.getPrecioVentaFormateado() + ". Vuelva a imprimir la etiqueta";
        }
        return new Lectura(producto, aviso);
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
