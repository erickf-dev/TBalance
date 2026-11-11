package sistemainventario.bridge;

import com.google.gson.JsonParseException;
import sistemainventario.exception.PersistenceException;
import sistemainventario.exception.ValidationException;
import sistemainventario.util.JsonUtil;

import java.util.function.Supplier;

/**
 * Formato de las respuestas que el bridge devuelve a JavaScript.
 *
 * <p>La vista espera {@code "OK"}, un JSON o un texto que empiece por
 * {@code "ERROR"}. En P2 cada método del bridge repetía su propio
 * {@code try/catch}; ahora el manejo de excepciones está centralizado aquí
 * (<b>SRP</b> y eliminación de código duplicado).</p>
 */
public final class BridgeResponse {

    public static final String OK = "OK";
    private static final String PREFIJO_ERROR = "ERROR: ";

    private BridgeResponse() {
    }

    /** @return el objeto serializado como JSON */
    public static String json(Object objeto) {
        return JsonUtil.toJson(objeto);
    }

    /** @return mensaje de error en el formato que entiende la vista */
    public static String error(String mensaje) {
        return PREFIJO_ERROR + mensaje;
    }

    /**
     * Ejecuta una acción y traduce cualquier excepción a {@code "ERROR: ..."}.
     *
     * @param accion operación que produce la respuesta
     * @return la respuesta de la acción o el mensaje de error
     */
    public static String ejecutar(Supplier<String> accion) {
        try {
            return accion.get();
        } catch (ValidationException e) {
            return error(e.getMessage());
        } catch (JsonParseException e) {
            return error("Los datos enviados no tienen un formato valido");
        } catch (PersistenceException e) {
            registrar(e);
            return error(e.getMessage());
        } catch (RuntimeException e) {
            registrar(e);
            return error("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Ejecuta una acción sin resultado y responde {@code "OK"} si no falla.
     *
     * @param accion operación a ejecutar
     * @return {@code "OK"} o el mensaje de error
     */
    public static String ejecutarOk(Runnable accion) {
        return ejecutar(() -> {
            accion.run();
            return OK;
        });
    }

    /**
     * Escapa un texto para insertarlo como literal en un script JavaScript.
     *
     * @param texto texto original
     * @return texto entre comillas simples y escapado, o {@code null}
     */
    public static String literalJs(String texto) {
        if (texto == null) {
            return "null";
        }
        String escapado = texto.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        return "'" + escapado + "'";
    }

    private static void registrar(RuntimeException e) {
        System.err.println("[Bridge] " + e.getMessage());
    }
}
