package sistemainventario.exception;

/**
 * Excepción de dominio que indica que una regla de negocio o un dato de
 * entrada no es válido (por ejemplo: nombre vacío, cantidad negativa o
 * stock insuficiente).
 *
 * <p>Es una excepción no comprobada para que las reglas se expresen en el
 * modelo y en los servicios sin contaminar cada firma con {@code throws}.
 * La capa {@code bridge} la captura y la traduce al formato
 * {@code "ERROR: mensaje"} que espera la vista web.</p>
 */
public class ValidationException extends RuntimeException {

    /**
     * @param mensaje descripción legible para el usuario final
     */
    public ValidationException(String mensaje) {
        super(mensaje);
    }
}
