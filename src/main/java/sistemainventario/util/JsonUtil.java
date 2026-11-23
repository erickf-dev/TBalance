package sistemainventario.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Punto único de configuración de Gson para todo el sistema.
 *
 * <p>Se usa tanto para persistir los archivos {@code .json} como para
 * intercambiar datos con la vista web a través del bridge.</p>
 */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    /** Sin espacios ni saltos de línea: para lo que viaja a la pantalla (pesa casi la mitad). */
    private static final Gson GSON_COMPACTO = new GsonBuilder()
            .serializeNulls()
            .create();

    private JsonUtil() {
    }

    /**
     * @param objeto objeto a serializar
     * @return representación JSON compacta (sin formato)
     */
    public static String toJsonCompacto(Object objeto) {
        return GSON_COMPACTO.toJson(objeto);
    }

    /**
     * @param objeto objeto a serializar
     * @return representación JSON con formato legible
     */
    public static String toJson(Object objeto) {
        return GSON.toJson(objeto);
    }

    /**
     * @param json  texto JSON
     * @param clase tipo de destino
     * @return la instancia deserializada
     */
    public static <T> T fromJson(String json, Class<T> clase) {
        return GSON.fromJson(json, clase);
    }

    /**
     * @param json  texto JSON con un arreglo
     * @param clase tipo de los elementos
     * @return lista deserializada; vacía si el texto está vacío
     */
    public static <T> List<T> fromJsonList(String json, Class<T> clase) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        Type tipoLista = TypeToken.getParameterized(List.class, clase).getType();
        List<T> resultado = GSON.fromJson(json, tipoLista);
        return resultado != null ? resultado : new ArrayList<>();
    }

    public static Gson getGson() {
        return GSON;
    }
}
