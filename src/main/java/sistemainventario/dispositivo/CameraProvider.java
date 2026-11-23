package sistemainventario.dispositivo;

import java.util.List;

/**
 * Cámaras disponibles en el computador. Permite elegir una cámara externa,
 * por ejemplo un celular conectado con una aplicación de cámara web (Iriun,
 * DroidCam o el "Usar como cámara web" de Windows).
 */
public interface CameraProvider {

    /** @return nombres de las cámaras conectadas, la predeterminada primero */
    List<String> listar();

    /**
     * @param nombre nombre de la cámara elegida, o vacío para la predeterminada
     * @return la cámara, sin abrir
     */
    Camera crear(String nombre);
}
