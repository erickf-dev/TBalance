package sistemainventario.service;

import sistemainventario.exception.AssistantException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import sistemainventario.exception.ValidationException;
import sistemainventario.ia.GeminiClient;
import sistemainventario.ia.LanguageModel;
import sistemainventario.model.Profile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Conversación con el asistente de la tienda.
 *
 * <p>Ciclo: el usuario escribe → el modelo responde o pide funciones →
 * {@link AssistantTools} las ejecuta con los datos reales → el modelo redacta
 * la respuesta. Los cambios quedan como {@link AssistantProposal} hasta que el
 * usuario los confirma con {@link #confirmar(String)}.</p>
 *
 * <p>Las funciones se ejecutan con {@code hiloDeDatos}, el mismo hilo (o
 * candado) que usan las demás llamadas de la vista, para no leer ni escribir
 * los datos al mismo tiempo que la interfaz.</p>
 */
public class AssistantService {

    private static final int MAXIMO_PASOS = 6;
    private static final int MAXIMO_MENSAJES = 40;

    private final LanguageModel modelo;
    private final Supplier<ProfileServices> servicios;
    private final Supplier<Profile> perfil;
    private final Executor hiloDeDatos;
    private final List<JsonObject> historial = new ArrayList<>();
    /** Se consultan desde dos hilos (la conversación y la confirmación), por eso son colecciones seguras. */
    private final Map<String, AssistantProposal> pendientes = new ConcurrentHashMap<>();
    private final List<String> notas = new CopyOnWriteArrayList<>();

    /**
     * @param modelo      modelo de lenguaje
     * @param servicios   servicios del perfil activo
     * @param perfil      perfil activo (nombre y tipo de negocio)
     * @param hiloDeDatos ejecuta el acceso a los datos de forma segura
     */
    public AssistantService(LanguageModel modelo, Supplier<ProfileServices> servicios,
                            Supplier<Profile> perfil, Executor hiloDeDatos) {
        this.modelo = modelo;
        this.servicios = servicios;
        this.perfil = perfil;
        this.hiloDeDatos = hiloDeDatos;
    }

    /**
     * Respuesta para la vista.
     *
     * @param texto      lo que dice el asistente
     * @param propuestas cambios por confirmar que se crearon en este turno
     */
    public record Reply(String texto, List<AssistantProposal> propuestas) {
    }

    /**
     * @param mensaje lo que escribió el usuario
     * @return la respuesta del asistente
     * @throws sistemainventario.ia.AssistantException si falla la conexión con el modelo
     */
    public synchronized Reply conversar(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new ValidationException("Escriba un mensaje");
        }
        String texto = mensaje.trim();
        if (!notas.isEmpty()) {
            texto = "[" + String.join(" ", notas) + "]\n" + texto;
            notas.clear();
        }
        historial.add(GeminiClient.textoComoContenido("user", texto));
        List<AssistantProposal> nuevas = new ArrayList<>();
        JsonArray funciones = AssistantTools.declaraciones();
        for (int paso = 0; paso < MAXIMO_PASOS; paso++) {
            LanguageModel.ModelReply respuesta = modelo.generar(instrucciones(), historial, funciones);
            historial.add(respuesta.contenido());
            if (respuesta.llamadas().isEmpty()) {
                recortar();
                String textoFinal = respuesta.texto().isBlank() && !nuevas.isEmpty()
                        ? "Revise y confirme abajo." : respuesta.texto();
                return new Reply(textoFinal.isBlank() ? "Listo." : textoFinal, nuevas);
            }
            historial.add(ejecutarFunciones(respuesta.llamadas(), nuevas));
        }
        recortar();
        return new Reply("No alcancé a completar esa solicitud. ¿Puede decirla en partes más pequeñas?", nuevas);
    }

    private JsonObject ejecutarFunciones(List<LanguageModel.FunctionCall> llamadas, List<AssistantProposal> nuevas) {
        JsonArray partes = new JsonArray();
        for (LanguageModel.FunctionCall llamada : llamadas) {
            JsonObject[] resultado = new JsonObject[1];
            hiloDeDatos.execute(() -> {
                AssistantTools herramientas = new AssistantTools(servicios.get(), propuesta -> {
                    pendientes.put(propuesta.getId(), propuesta);
                    nuevas.add(propuesta);
                });
                resultado[0] = herramientas.ejecutar(llamada.nombre(), llamada.argumentos());
            });
            JsonObject respuesta = new JsonObject();
            if (llamada.id() != null) {
                respuesta.addProperty("id", llamada.id());
            }
            respuesta.addProperty("name", llamada.nombre());
            respuesta.add("response", resultado[0]);
            JsonObject parte = new JsonObject();
            parte.add("functionResponse", respuesta);
            partes.add(parte);
        }
        JsonObject contenido = new JsonObject();
        contenido.addProperty("role", "user");
        contenido.add("parts", partes);
        return contenido;
    }

    /**
     * Hace el cambio propuesto. Debe llamarse desde el hilo de la interfaz.
     *
     * @param id identificador de la propuesta
     * @return descripción de lo que se hizo
     */
    public String confirmar(String id) {
        AssistantProposal propuesta = pendientes.remove(id);
        if (propuesta == null) {
            throw new ValidationException("Ese cambio ya se confirmó o se descartó");
        }
        propuesta.ejecutar();
        notas.add("El usuario confirmó y quedó registrado: " + propuesta.getTitulo() + ".");
        return propuesta.getTitulo();
    }

    /** @param id propuesta que el usuario descartó */
    public void descartar(String id) {
        AssistantProposal propuesta = pendientes.remove(id);
        if (propuesta != null) {
            notas.add("El usuario descartó: " + propuesta.getTitulo() + ".");
        }
    }

    /** Empieza una conversación nueva. */
    public synchronized void reiniciar() {
        historial.clear();
        pendientes.clear();
        notas.clear();
    }

    /** Mantiene la conversación corta: se quitan los mensajes más viejos desde un mensaje del usuario. */
    private void recortar() {
        while (historial.size() > MAXIMO_MENSAJES) {
            historial.remove(0);
            while (!historial.isEmpty() && !esTextoDelUsuario(historial.get(0))) {
                historial.remove(0);
            }
        }
    }

    private static boolean esTextoDelUsuario(JsonObject contenido) {
        return "user".equals(contenido.has("role") ? contenido.get("role").getAsString() : "")
                && contenido.getAsJsonArray("parts").get(0).getAsJsonObject().has("text");
    }

    private String instrucciones() {
        Profile p = perfil.get();
        String negocio = p == null ? "una tienda" : p.getNombre() + " (" + p.getPerfilNegocio().getEtiqueta() + ")";
        String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "CO")));
        return "Eres el asistente de TBalance, el sistema de inventario de " + negocio + ", una pyme colombiana. "
                + "Hoy es " + hoy + ". Hablas en español de Colombia, claro y breve, como un ayudante de confianza del tendero.\n"
                + "Reglas:\n"
                + "1. Nunca inventes productos, cantidades, precios ni ventas: consulta siempre con las funciones.\n"
                + "2. Para registrar ventas, entradas de mercancía, salidas, cambios de precio o productos nuevos usa las "
                + "funciones proponer_*. Eso NO registra nada: muestra una tarjeta para que el usuario confirme. Dile que "
                + "revise y confirme; nunca digas que ya quedó registrado.\n"
                + "3. Si un producto es ambiguo o no existe, pregunta cuál es o sugiere crearlo.\n"
                + "4. Cuando llega un proveedor (\"¿qué le falta de Postobón?\", \"vino el de Alpina\"), usa productos_por_reponer "
                + "con ese proveedor o marca y responde con una lista corta: producto, stock actual y cuánto pedir. Si luego "
                + "dicen que llegó el pedido, propone la entrada con esas cantidades.\n"
                + "5. Precios en pesos colombianos sin decimales, por ejemplo $12.500.\n"
                + "6. Respuestas cortas. Usa listas con guiones cuando haya varios productos. Puedes usar **negrita** para cifras clave.\n"
                + "7. Si piden algo que no es del negocio, responde amablemente que solo ayudas con la tienda.";
    }
}
