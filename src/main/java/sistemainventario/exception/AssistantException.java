package sistemainventario.exception;

/**
 * Error al hablar con el modelo de lenguaje (sin internet, clave inválida,
 * límite gratuito alcanzado...). El mensaje está escrito para el usuario.
 */
public class AssistantException extends RuntimeException {

    public AssistantException(String mensaje) {
        super(mensaje);
    }

    public AssistantException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
