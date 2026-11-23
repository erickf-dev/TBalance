package sistemainventario.dispositivo;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import sistemainventario.exception.ValidationException;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * {@link Camera} implementada con la webcam del computador mediante la
 * librería <i>webcam-capture</i>.
 *
 * <p><b>Bajo acoplamiento:</b> es la única clase que conoce esa librería.</p>
 */
public class WebcamCamera implements Camera {

    /** Tiempo máximo para detectar la cámara al abrir. */
    private static final long ESPERA_DETECCION_MS = 8000;
    /** Ancho máximo del cuadro: suficiente para leer etiquetas pequeñas sin ser lento. */
    private static final int ANCHO_MAXIMO = 1280;

    private final String nombre;
    private Webcam webcam;

    /** Usa la cámara predeterminada. */
    public WebcamCamera() {
        this(null);
    }

    /** @param nombre nombre de la cámara (como lo da {@link #disponibles()}), o vacío para la predeterminada */
    public WebcamCamera(String nombre) {
        this.nombre = nombre == null || nombre.isBlank() ? null : nombre;
    }

    /**
     * @return nombres de las cámaras conectadas, la predeterminada primero
     * @throws ValidationException si el sistema no permite buscar cámaras
     */
    public static List<String> disponibles() {
        try {
            return Webcam.getWebcams(ESPERA_DETECCION_MS).stream().map(Webcam::getName).toList();
        } catch (TimeoutException e) {
            throw new ValidationException("Las cámaras no respondieron a tiempo");
        } catch (WebcamException | LinkageError e) {
            throw new ValidationException("No se pudieron buscar las cámaras: " + e.getMessage());
        }
    }

    @Override
    public void abrir() {
        try {
            webcam = nombre == null ? Webcam.getDefault(ESPERA_DETECCION_MS) : buscar(nombre);
            if (webcam == null) {
                throw new ValidationException("No se encontro ninguna camara conectada");
            }
            elegirResolucion();
            if (!webcam.open()) {
                throw new ValidationException("No se pudo encender la camara. Cierra otras aplicaciones que la esten usando");
            }
        } catch (TimeoutException e) {
            throw new ValidationException("La camara no respondio a tiempo");
        } catch (WebcamException e) {
            throw new ValidationException("No se pudo usar la camara: " + e.getMessage());
        } catch (LinkageError e) {
            // La librería usa código nativo; si el sistema no lo admite, se informa en vez de cerrar el hilo.
            throw new ValidationException("Este computador no permite usar la camara desde Java: " + e.getMessage());
        }
    }

    private static Webcam buscar(String nombre) throws TimeoutException {
        return Webcam.getWebcams(ESPERA_DETECCION_MS).stream()
                .filter(w -> w.getName().equals(nombre)).findFirst()
                .orElseThrow(() -> new ValidationException("La cámara " + nombre
                        + " no está conectada. Revise el cable o la aplicación del celular."));
    }

    /** Usa la resolución más alta que la cámara ofrezca sin pasar de {@link #ANCHO_MAXIMO}. */
    private void elegirResolucion() {
        Arrays.stream(webcam.getViewSizes())
                .filter(tamano -> tamano.width <= ANCHO_MAXIMO)
                .max(Comparator.comparingInt((Dimension tamano) -> tamano.width * tamano.height))
                .ifPresent(webcam::setViewSize);
    }

    @Override
    public BufferedImage capturar() {
        return webcam != null && webcam.isOpen() ? webcam.getImage() : null;
    }

    @Override
    public void cerrar() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}
