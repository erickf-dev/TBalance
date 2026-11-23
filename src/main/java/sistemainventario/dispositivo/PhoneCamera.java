package sistemainventario.dispositivo;

import java.awt.image.BufferedImage;

/**
 * {@link Camera} que toma las imágenes del celular conectado por Wi-Fi
 * ({@link PhoneLink}). Para el escáner de ventas es una cámara más.
 */
public class PhoneCamera implements Camera {

    /** Nombre con el que la vista elige esta cámara. */
    public static final String NOMBRE = "__celular__";

    private final PhoneLink enlace;

    /** @param enlace conexión con el celular */
    public PhoneCamera(PhoneLink enlace) {
        this.enlace = enlace;
    }

    @Override
    public void abrir() {
        enlace.iniciar();
    }

    @Override
    public BufferedImage capturar() {
        return enlace.tomarFotoNueva();
    }

    @Override
    public void cerrar() {
        enlace.detener();
    }

    @Override
    public void avisar(String mensaje, boolean correcto) {
        enlace.avisar(mensaje, correcto);
    }
}
