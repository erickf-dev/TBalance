package sistemainventario.bridge;

/**
 * Canal para que Java avise a la vista web de un cambio de estado.
 *
 * <p><b>Bajo acoplamiento / DIP:</b> los controladores dependen de esta
 * interfaz y no de {@code WebEngine}; así pueden probarse sin JavaFX.</p>
 */
@FunctionalInterface
public interface ViewNotifier {

    /**
     * @param script código JavaScript a ejecutar en la vista
     */
    void ejecutarScript(String script);
}
