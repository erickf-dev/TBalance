package sistemainventario.ia;

import sistemainventario.exception.AssistantException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Modelo de lenguaje en la nube que usa el asistente.
 *
 * <p><b>DIP / OCP:</b> el asistente depende de esta interfaz; hoy la implementa
 * {@link GeminiClient} y se puede agregar otro proveedor sin tocar el resto.
 * Los mensajes usan el formato de "contenidos" de Gemini: objetos con
 * {@code role} ("user" o "model") y una lista de {@code parts}.</p>
 */
public interface LanguageModel {

    /**
     * Pide la siguiente respuesta del modelo.
     *
     * @param instrucciones instrucciones de sistema (rol, reglas, fecha)
     * @param contenidos    conversación hasta ahora
     * @param funciones     declaraciones de las funciones que el modelo puede pedir
     * @return la respuesta: texto y/o llamadas a funciones
     * @throws AssistantException si el servicio no respondió o rechazó la petición
     */
    ModelReply generar(String instrucciones, List<JsonObject> contenidos, JsonArray funciones);

    /**
     * Respuesta del modelo.
     *
     * @param contenido contenido completo tal como lo devolvió el modelo (se guarda en la conversación)
     * @param texto     texto para el usuario (puede estar vacío si solo pidió funciones)
     * @param llamadas  funciones que el modelo pidió ejecutar
     */
    record ModelReply(JsonObject contenido, String texto, List<FunctionCall> llamadas) {
    }

    /**
     * @param id        identificador de la llamada (algunos modelos lo envían; puede ser {@code null})
     * @param nombre    nombre de la función
     * @param argumentos argumentos en JSON
     */
    record FunctionCall(String id, String nombre, JsonObject argumentos) {
    }
}
