package sistemainventario.bridge;

/**
 * Transición visual que hace la ventana de JavaFX por encima de la página HTML.
 *
 * <p>El visor web de JavaFX dibuja la página con el procesador, así que una
 * animación grande hecha en CSS se vuelve a pintar completa en cada cuadro.
 * Las animaciones de JavaFX, en cambio, las hace la tarjeta gráfica. Por eso
 * los cambios grandes (de sección, de ventana o de tema) se hacen así: la
 * ventana toma una imagen de la zona que va a cambiar, la pone encima de la
 * página y la desvanece mientras debajo ya se ve el contenido nuevo.</p>
 *
 * <p><b>DIP:</b> {@link JavaBridge} depende de esta interfaz y no de las clases
 * de JavaFX; la implementación está en la capa de vista.</p>
 */
@FunctionalInterface
public interface ScreenTransition {

    /**
     * Desvanece una imagen de la zona indicada, encima de la página.
     *
     * @param x            borde izquierdo de la zona, en píxeles de la página
     * @param y            borde superior de la zona, en píxeles de la página
     * @param ancho        ancho de la zona
     * @param alto         alto de la zona
     * @param milisegundos duración del desvanecimiento
     */
    void desvanecerZona(double x, double y, double ancho, double alto, int milisegundos);
}
