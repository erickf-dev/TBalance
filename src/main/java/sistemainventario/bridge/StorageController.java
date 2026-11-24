package sistemainventario.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sistemainventario.AppContext;
import sistemainventario.model.DatabaseEngine;
import sistemainventario.model.DatabaseSettings;

/**
 * Casos de uso de "Conexión con Oracle": ver la configuración, probar la
 * conexión con Oracle y cambiarla.
 *
 * <p>La clave de la base nunca se envía a la vista: solo se informa si hay una
 * guardada. Si el usuario deja la clave vacía, se conserva la anterior.</p>
 */
public class StorageController {

    private final AppContext contexto;
    private final ViewNotifier notificador;

    /**
     * @param contexto    contexto de la aplicación
     * @param notificador canal para avisar a la vista
     */
    public StorageController(AppContext contexto, ViewNotifier notificador) {
        this.contexto = contexto;
        this.notificador = notificador;
    }

    /**
     * @return número que cambia cuando otro equipo guardó algo en la misma base;
     * la pantalla lo consulta cada pocos segundos para actualizarse sola
     */
    public String revisarCambios() {
        return BridgeResponse.ejecutar(() -> String.valueOf(contexto.revisarCambiosDeOtrosEquipos()));
    }

    /** @return JSON con la URL, el usuario, si hay conexión y el aviso pendiente */
    public String obtenerAlmacenamiento() {
        return BridgeResponse.ejecutar(() -> {
            DatabaseSettings config = contexto.getAjustesBaseDeDatos();
            JsonObject json = new JsonObject();
            json.addProperty("motor", "ORACLE");
            json.addProperty("url", config.url());
            json.addProperty("usuario", config.usuario());
            json.addProperty("tieneClave", !config.clave().isEmpty());
            json.addProperty("enUso", contexto.getAlmacenEnUso());
            json.addProperty("aviso", contexto.getAvisoAlmacenamiento());
            json.addProperty("avisoEsError", !contexto.estaConectado());
            JsonArray motores = new JsonArray();
            JsonObject oracle = new JsonObject();
            oracle.addProperty("id", "ORACLE");
            oracle.addProperty("etiqueta", DatabaseEngine.ORACLE.getEtiqueta());
            oracle.addProperty("urlPorDefecto", DatabaseEngine.ORACLE.getUrlPorDefecto());
            motores.add(oracle);
            json.add("motores", motores);
            return json.toString();
        });
    }

    /**
     * @param datosJson motor, URL, usuario y clave
     * @return el resultado de la prueba o "ERROR: ..."
     */
    public String probarAlmacenamiento(String datosJson) {
        return BridgeResponse.ejecutar(() -> contexto.getAlmacenamiento().probar(leer(datosJson)));
    }

    /**
     * Guarda la configuración y empieza a usarla (crea tablas y copia datos si hace falta).
     *
     * @param datosJson motor, URL, usuario y clave
     * @return mensaje para el usuario o "ERROR: ..."
     */
    public String guardarAlmacenamiento(String datosJson) {
        return BridgeResponse.ejecutar(() -> {
            String mensaje = contexto.cambiarAlmacenamiento(leer(datosJson));
            notificador.ejecutarScript("onPerfilesActualizados()");
            return mensaje;
        });
    }

    /**
     * Desde la pantalla de inicio de un computador nuevo (antes de que exista
     * algún usuario), se conecta a la base que ya usa el negocio, por ejemplo
     * en la nube. Después de esto, cada persona entra con su usuario.
     *
     * @param datosJson motor, URL, usuario y clave de la base
     * @return mensaje para el usuario o "ERROR: ..."
     */
    public String conectarBaseExistente(String datosJson) {
        return BridgeResponse.ejecutar(() -> {
            // sin sesión solo se puede conectar si todavía no hay conexión o la base no tiene usuarios
            if (contexto.estaConectado() && !contexto.getUsuarios().esPrimeraVez()) {
                throw new sistemainventario.exception.ValidationException(
                        "TBalance ya está conectado con Oracle. Entre como dueño y cambie la conexión en Copias de seguridad.");
            }
            DatabaseSettings config = leer(datosJson);
            String mensaje = contexto.cambiarAlmacenamiento(config);
            notificador.ejecutarScript("onPerfilesActualizados()");
            return mensaje;
        });
    }

    private DatabaseSettings leer(String datosJson) {
        JsonObject json = JsonParser.parseString(datosJson).getAsJsonObject();
        String clave = texto(json, "clave");
        if (clave.isEmpty()) {
            clave = contexto.getAjustesBaseDeDatos().clave();
        }
        return new DatabaseSettings(texto(json, "url"), texto(json, "usuario"), clave);
    }

    private static String texto(JsonObject json, String campo) {
        return json.has(campo) && !json.get(campo).isJsonNull() ? json.get(campo).getAsString() : "";
    }
}
