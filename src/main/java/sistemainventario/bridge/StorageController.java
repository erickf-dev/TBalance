package sistemainventario.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sistemainventario.AppContext;
import sistemainventario.model.DatabaseEngine;
import sistemainventario.model.DatabaseSettings;

/**
 * Casos de uso de "Dónde se guardan los datos": ver la configuración, probar la
 * conexión y cambiar entre archivos, PostgreSQL y Oracle.
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

    /** @return JSON con el motor, la URL, el usuario, el almacenamiento en uso, el aviso pendiente y los motores */
    public String obtenerAlmacenamiento() {
        return BridgeResponse.ejecutar(() -> {
            DatabaseSettings config = contexto.getAjustesBaseDeDatos();
            JsonObject json = new JsonObject();
            json.addProperty("motor", config.motor().name());
            json.addProperty("url", config.url());
            json.addProperty("usuario", config.usuario());
            json.addProperty("tieneClave", !config.clave().isEmpty());
            json.addProperty("enUso", contexto.getAlmacenEnUso());
            json.addProperty("aviso", contexto.getAvisoAlmacenamiento());
            json.addProperty("avisoEsError", config.usaBaseDeDatos()
                    && !contexto.getAlmacenEnUso().equals(config.motor().getEtiqueta()));
            JsonArray motores = new JsonArray();
            for (DatabaseEngine motor : DatabaseEngine.values()) {
                JsonObject item = new JsonObject();
                item.addProperty("id", motor.name());
                item.addProperty("etiqueta", motor.getEtiqueta());
                item.addProperty("urlPorDefecto", motor.getUrlPorDefecto());
                motores.add(item);
            }
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
            if (!contexto.getUsuarios().esPrimeraVez()) {
                throw new sistemainventario.exception.ValidationException(
                        "Este computador ya tiene usuarios. Entre como dueño y cambie la base en Copias de seguridad.");
            }
            DatabaseSettings config = leer(datosJson);
            if (!config.usaBaseDeDatos()) {
                throw new sistemainventario.exception.ValidationException("Elija PostgreSQL u Oracle");
            }
            contexto.getAlmacenamiento().probar(config);
            String mensaje = contexto.cambiarAlmacenamiento(config);
            notificador.ejecutarScript("onPerfilesActualizados()");
            return mensaje;
        });
    }

    private DatabaseSettings leer(String datosJson) {
        JsonObject json = JsonParser.parseString(datosJson).getAsJsonObject();
        DatabaseEngine motor = DatabaseEngine.desde(texto(json, "motor"));
        String clave = texto(json, "clave");
        DatabaseSettings actual = contexto.getAjustesBaseDeDatos();
        if (clave.isEmpty() && actual.motor() == motor) {
            clave = actual.clave();
        }
        return new DatabaseSettings(motor, texto(json, "url"), texto(json, "usuario"), clave);
    }

    private static String texto(JsonObject json, String campo) {
        return json.has(campo) && !json.get(campo).isJsonNull() ? json.get(campo).getAsString() : "";
    }
}
