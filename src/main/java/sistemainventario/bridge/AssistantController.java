package sistemainventario.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import sistemainventario.AppContext;
import sistemainventario.exception.AssistantException;
import sistemainventario.exception.ValidationException;
import sistemainventario.model.Profile;
import sistemainventario.service.AssistantProposal;
import sistemainventario.service.AssistantService;
import sistemainventario.service.AssistantSettingsService;
import sistemainventario.util.AssistantLog;
import sistemainventario.util.JsonUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Casos de uso del asistente: configurarlo, conversar y confirmar o descartar
 * los cambios que propone.
 *
 * <p>La consulta al modelo tarda unos segundos, así que {@link #asistenteEnviar}
 * responde enseguida y trabaja en segundo plano; cuando termina avisa a la vista
 * con {@code onAsistenteRespuesta(...)} por el {@link ViewNotifier}. Así la
 * interfaz y sus animaciones siguen funcionando mientras tanto.</p>
 *
 * <p><b>GRASP Controller:</b> la configuración la resuelve
 * {@link AssistantSettingsService} y la conversación {@link AssistantService};
 * este controlador solo convierte a JSON y avisa a la vista.</p>
 */
public class AssistantController {

    private final AppContext contexto;
    private final ViewNotifier notificador;
    private final Executor hiloDeDatos;
    private final AssistantSettingsService ajustes;
    private final ExecutorService segundoPlano = Executors.newSingleThreadExecutor(r -> {
        Thread hilo = new Thread(r, "tbalance-asistente");
        hilo.setDaemon(true);
        return hilo;
    });
    private AssistantService asistente;
    private String perfilDeLaConversacion;

    /**
     * @param contexto    servicios de la aplicación
     * @param notificador avisos hacia la vista
     * @param hiloDeDatos ejecuta el acceso a los datos de forma segura
     */
    public AssistantController(AppContext contexto, ViewNotifier notificador, Executor hiloDeDatos) {
        this.contexto = contexto;
        this.notificador = notificador;
        this.hiloDeDatos = hiloDeDatos;
        this.ajustes = contexto.getAjustesAsistente();
    }

    /** @return {@code {"configurado":bool,"modelo":"..."}} */
    public String asistenteEstado() {
        JsonObject r = new JsonObject();
        r.addProperty("configurado", ajustes.leer().isPresent());
        r.addProperty("modelo", ajustes.modeloActual());
        return r.toString();
    }

    /**
     * @param clave  clave de API de Google AI Studio (vacía para borrarla)
     * @param modelo modelo (vacío para el de por defecto)
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String asistenteConfigurar(String clave, String modelo) {
        return BridgeResponse.ejecutarOk(() -> {
            ajustes.configurar(clave, modelo);
            asistente = null;
        });
    }

    /**
     * Envía un mensaje; la respuesta llega después con {@code onAsistenteRespuesta}.
     *
     * @param mensaje texto del usuario
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String asistenteEnviar(String mensaje) {
        return BridgeResponse.ejecutar(() -> {
            AssistantService servicio = asistente();
            segundoPlano.submit(() -> {
                JsonObject r = new JsonObject();
                try {
                    AssistantService.Reply respuesta = servicio.conversar(mensaje);
                    r.addProperty("texto", respuesta.texto());
                    JsonArray propuestas = new JsonArray();
                    for (AssistantProposal p : respuesta.propuestas()) {
                        propuestas.add(JsonUtil.getGson().toJsonTree(p));
                    }
                    r.add("propuestas", propuestas);
                } catch (AssistantException e) {
                    r.addProperty("error", e.getMessage());
                } catch (Throwable e) {
                    // cualquier falla (incluso de Java) debe llegar a la vista; si no, quedaría "escribiendo" para siempre
                    AssistantLog.registrar("Error del asistente: " + e);
                    r.addProperty("error", "El asistente tuvo un problema: " + e.getClass().getSimpleName()
                            + (e.getMessage() == null ? "" : " — " + e.getMessage()));
                }
                notificador.ejecutarScript("onAsistenteRespuesta(" + BridgeResponse.literalJs(r.toString()) + ")");
            });
            return BridgeResponse.OK;
        });
    }

    /**
     * Prueba la clave y la conexión con un mensaje corto; el resultado llega con
     * {@code onAsistentePrueba({"ok":bool,"mensaje":"..."})}.
     *
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String asistenteProbar() {
        return BridgeResponse.ejecutar(() -> {
            ajustes.requerir();
            probarEnSegundoPlano(() -> ajustes.probarConexion(this::avisarModelo));
            return BridgeResponse.OK;
        });
    }

    /**
     * Prueba la clave escrita en la pantalla sin guardarla; el resultado llega con
     * {@code onAsistentePrueba(...)} igual que {@link #asistenteProbar()}.
     *
     * @param clave  clave pegada por el usuario
     * @param modelo modelo elegido
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String asistenteProbarClave(String clave, String modelo) {
        return BridgeResponse.ejecutar(() -> {
            if (clave == null || clave.isBlank()) {
                throw new ValidationException("Pegue la clave de API");
            }
            probarEnSegundoPlano(() -> ajustes.probarClave(clave, modelo));
            return BridgeResponse.OK;
        });
    }

    private void probarEnSegundoPlano(Supplier<AssistantSettingsService.ConnectionTest> prueba) {
        segundoPlano.submit(() -> {
            JsonObject r = new JsonObject();
            try {
                AssistantSettingsService.ConnectionTest resultado = prueba.get();
                r.addProperty("ok", true);
                r.addProperty("modelo", resultado.modelo());
                r.addProperty("mensaje", resultado.mensaje());
            } catch (Throwable e) {
                AssistantLog.registrar("Prueba fallida: " + e);
                r.addProperty("ok", false);
                r.addProperty("mensaje", e instanceof AssistantException ? e.getMessage()
                        : e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            notificador.ejecutarScript("onAsistentePrueba(" + BridgeResponse.literalJs(r.toString()) + ")");
        });
    }

    /** @return descripción de lo registrado o {@code "ERROR: ..."} */
    public String asistenteConfirmar(String propuestaId) {
        return BridgeResponse.ejecutar(() -> asistente().confirmar(propuestaId));
    }

    /** @return {@code "OK"} */
    public String asistenteDescartar(String propuestaId) {
        return BridgeResponse.ejecutarOk(() -> asistente().descartar(propuestaId));
    }

    /** @return {@code "OK"} */
    public String asistenteReiniciar() {
        return BridgeResponse.ejecutarOk(() -> {
            if (asistente != null) {
                asistente.reiniciar();
            }
        });
    }

    /** Crea el asistente con la clave guardada; empieza de cero si cambió el perfil activo. */
    private AssistantService asistente() {
        // la conversación es de una persona en un negocio: si cambia cualquiera de los dos, empieza de cero
        String perfilActual = contexto.getGestorPerfiles().getPerfilActivo().map(Profile::getId).orElse("")
                + "/" + contexto.getSesion().actual().map(u -> u.getId()).orElse("");
        if (asistente == null || !perfilActual.equals(perfilDeLaConversacion)) {
            asistente = new AssistantService(ajustes.crearModelo(this::avisarModelo),
                    contexto::requerirServiciosActivos,
                    () -> contexto.getGestorPerfiles().getPerfilActivo().orElse(null),
                    hiloDeDatos,
                    () -> contexto.rolActual().map(r -> r.getPermisos()).orElse(java.util.EnumSet.noneOf(sistemainventario.model.Permiso.class)));
            perfilDeLaConversacion = perfilActual;
        }
        return asistente;
    }

    /** Avisa a la vista que Google retiró el modelo guardado y se pasó a otro. */
    private void avisarModelo(String modelo) {
        JsonObject aviso = new JsonObject();
        aviso.addProperty("modelo", modelo);
        notificador.ejecutarScript("onAsistenteModelo(" + BridgeResponse.literalJs(aviso.toString()) + ")");
    }
}
