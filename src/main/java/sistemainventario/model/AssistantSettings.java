package sistemainventario.model;

/**
 * Configuración del asistente: la clave de API de Google AI Studio y el modelo
 * de Gemini que se usa.
 *
 * <p>Se guarda solo en el computador de cada usuario
 * ({@code ~/.inventariopro/asistente.json}), fuera de los datos de los perfiles,
 * de las copias de seguridad y del repositorio.</p>
 *
 * @param clave  clave de API
 * @param modelo nombre del modelo, por ejemplo {@code gemini-3.5-flash-lite}
 */
public record AssistantSettings(String clave, String modelo) {
}
