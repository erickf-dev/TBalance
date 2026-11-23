package sistemainventario.dispositivo;

import java.awt.image.BufferedImage;

/**
 * Cámara de la que se toman cuadros para leer códigos QR.
 *
 * <p><b>DIP / Bajo acoplamiento:</b> el escáner de ventas depende de esta
 * interfaz y no de una librería de cámaras concreta, por eso puede probarse
 * con imágenes guardadas en lugar de una webcam real.</p>
 */
public interface Camera {

    /**
     * Enciende la cámara.
     *
     * @throws sistemainventario.exception.ValidationException si no hay
     *         cámara disponible o no se pudo abrir
     */
    void abrir();

    /** @return el cuadro actual, o {@code null} si todavía no hay imagen */
    BufferedImage capturar();

    /** Apaga la cámara; no falla si ya estaba apagada. */
    void cerrar();

    /**
     * Avisa en la cámara lo que pasó con la última lectura. La webcam no hace
     * nada; el celular vibra y muestra el mensaje.
     *
     * @param mensaje  texto para el usuario
     * @param correcto {@code true} si el producto se agregó a la venta
     */
    default void avisar(String mensaje, boolean correcto) {
        // por defecto, sin aviso en la cámara
    }
}
