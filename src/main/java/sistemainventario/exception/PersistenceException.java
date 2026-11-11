package sistemainventario.exception;

/**
 * Error técnico al leer o escribir los archivos JSON de datos.
 *
 * <p>Sustituye el patrón de P2 de imprimir en consola y continuar, que podía
 * ocultar pérdidas de información. Ahora el error se propaga hasta la capa
 * {@code bridge}, que informa al usuario.</p>
 */
public class PersistenceException extends RuntimeException {

    /**
     * @param mensaje descripción del error
     * @param causa   excepción original de E/S o de formato JSON
     */
    public PersistenceException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
