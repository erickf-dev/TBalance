package sistemainventario.ia;

import sistemainventario.util.AssistantLog;
import sistemainventario.exception.AssistantException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Cliente de la API de Google Gemini (capa gratuita) con llamadas a funciones.
 *
 * <p>Usa {@code POST /v1beta/models/<modelo>:generateContent} con la clave en
 * la cabecera {@code x-goog-api-key}. Solo usa {@link HttpClient} de Java y
 * Gson, que ya está en el proyecto.</p>
 *
 * <p>Google retira modelos con el tiempo (por ejemplo, desde 2026
 * {@code gemini-2.5-flash-lite} ya no se ofrece a claves nuevas). Si el modelo
 * configurado no existe, el cliente pregunta a Google qué modelos tiene esa
 * clave ({@code GET /v1beta/models}), elige el Flash-Lite estable más nuevo y
 * repite la consulta. Así el asistente sigue funcionando sin cambiar el código.</p>
 */
public class GeminiClient implements LanguageModel {

    /** Modelo liviano de la capa gratuita, con soporte de funciones. */
    public static final String MODELO_POR_DEFECTO = "gemini-3.5-flash-lite";
    /** Dirección del servicio; se puede cambiar con -Dtbalance.ia.url para probar con un servidor simulado. */
    private static final String URL_BASE = System.getProperty("tbalance.ia.url",
            "https://generativelanguage.googleapis.com/v1beta/models/");
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(45);

    private final String clave;
    private volatile String modelo;
    private final HttpClient http;
    private Consumer<String> alCambiarModelo = m -> { };

    /**
     * @param clave  clave de API de Google AI Studio
     * @param modelo nombre del modelo, por ejemplo {@value #MODELO_POR_DEFECTO}
     */
    public GeminiClient(String clave, String modelo) {
        this.clave = clave;
        this.modelo = modelo == null || modelo.isBlank() ? MODELO_POR_DEFECTO : modelo.trim();
        this.http = SecureConnection.crearCliente();
    }

    /** @return el modelo que se está usando (puede cambiar si Google retiró el configurado) */
    public String modelo() {
        return modelo;
    }

    /**
     * @param accion qué hacer cuando el cliente cambia a otro modelo (por ejemplo, guardarlo)
     */
    public void alCambiarModelo(Consumer<String> accion) {
        this.alCambiarModelo = accion == null ? m -> { } : accion;
    }

    @Override
    public ModelReply generar(String instrucciones, List<JsonObject> contenidos, JsonArray funciones) {
        JsonObject cuerpo = new JsonObject();
        cuerpo.add("systemInstruction", textoComoContenido(null, instrucciones));
        JsonArray lista = new JsonArray();
        contenidos.forEach(lista::add);
        cuerpo.add("contents", lista);
        if (funciones != null && funciones.size() > 0) {
            JsonObject herramientas = new JsonObject();
            herramientas.add("functionDeclarations", funciones);
            JsonArray tools = new JsonArray();
            tools.add(herramientas);
            cuerpo.add("tools", tools);
        }
        // Para Gemini 3.x Google recomienda no cambiar la temperatura (por defecto 1.0)
        JsonObject generacion = new JsonObject();
        generacion.addProperty("maxOutputTokens", 4096);
        cuerpo.add("generationConfig", generacion);

        HttpResponse<String> respuesta = enviar(cuerpo.toString());
        if (respuesta.statusCode() == 404) {
            Optional<String> otro = buscarModeloDisponible();
            if (otro.isPresent() && !otro.get().equals(modelo)) {
                AssistantLog.registrar("El modelo " + modelo + " no está disponible; se usa " + otro.get());
                modelo = otro.get();
                alCambiarModelo.accept(modelo);
                respuesta = enviar(cuerpo.toString());
            }
        }
        if (respuesta.statusCode() != 200) {
            String cuerpoError = respuesta.body() == null ? "" : respuesta.body();
            AssistantLog.registrar("Gemini respondió " + respuesta.statusCode() + " (" + modelo + "): "
                    + cuerpoError.substring(0, Math.min(cuerpoError.length(), 1500)).replace('\n', ' '));
            throw new AssistantException(mensajeDeError(respuesta.statusCode(), cuerpoError, modelo));
        }
        return leer(JsonParser.parseString(respuesta.body()).getAsJsonObject());
    }

    private HttpResponse<String> enviar(String cuerpo) {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + URLEncoder.encode(modelo, StandardCharsets.UTF_8) + ":generateContent"))
                .timeout(ESPERA_MAXIMA)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", clave)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        return ejecutar(peticion);
    }

    private HttpResponse<String> ejecutar(HttpRequest peticion) {
        try {
            return http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            AssistantLog.registrar("Fallo de conexión con " + modelo + ": " + e);
            throw new AssistantException(mensajeDeConexion(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssistantException("Se canceló la consulta al asistente.", e);
        }
    }

    /**
     * Pregunta a Google qué modelos puede usar esta clave y elige el más
     * liviano: el Flash-Lite estable de versión más alta; si no hay, el Flash
     * estable más alto. Se omiten las versiones de prueba y las de imagen o voz.
     *
     * @return el nombre del modelo elegido, o vacío si no se pudo averiguar
     */
    public Optional<String> buscarModeloDisponible() {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "?pageSize=1000"))
                .timeout(ESPERA_MAXIMA)
                .header("x-goog-api-key", clave)
                .GET()
                .build();
        HttpResponse<String> respuesta = ejecutar(peticion);
        if (respuesta.statusCode() != 200) {
            AssistantLog.registrar("No se pudo listar los modelos (" + respuesta.statusCode() + ")");
            return Optional.empty();
        }
        List<String> nombres = new ArrayList<>();
        JsonObject json = JsonParser.parseString(respuesta.body()).getAsJsonObject();
        JsonArray modelos = json.has("models") ? json.getAsJsonArray("models") : new JsonArray();
        for (JsonElement elemento : modelos) {
            JsonObject m = elemento.getAsJsonObject();
            JsonArray metodos = m.has("supportedGenerationMethods") ? m.getAsJsonArray("supportedGenerationMethods") : new JsonArray();
            if (metodos.toString().contains("\"generateContent\"") && m.has("name")) {
                nombres.add(m.get("name").getAsString().replaceFirst("^models/", ""));
            }
        }
        AssistantLog.registrar("Modelos disponibles para la clave: " + nombres);
        return elegirModelo(nombres);
    }

    /** Elige entre los nombres de modelo el Flash-Lite (o si no, el Flash) estable más nuevo. */
    static Optional<String> elegirModelo(List<String> nombres) {
        Comparator<String> porVersion = Comparator.comparingDouble(GeminiClient::version);
        for (String tipo : new String[]{"flash-lite", "flash"}) {
            Optional<String> elegido = nombres.stream()
                    .filter(n -> n.matches("gemini-\\d+(\\.\\d+)?-" + tipo))
                    .max(porVersion);
            if (elegido.isPresent()) {
                return elegido;
            }
        }
        return Optional.empty();
    }

    private static double version(String nombre) {
        String numero = nombre.replaceFirst("^gemini-", "").replaceFirst("-.*$", "");
        try {
            return Double.parseDouble(numero);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** @return un contenido de Gemini con un solo texto */
    public static JsonObject textoComoContenido(String rol, String texto) {
        JsonObject parte = new JsonObject();
        parte.addProperty("text", texto);
        JsonArray partes = new JsonArray();
        partes.add(parte);
        JsonObject contenido = new JsonObject();
        if (rol != null) {
            contenido.addProperty("role", rol);
        }
        contenido.add("parts", partes);
        return contenido;
    }

    private ModelReply leer(JsonObject json) {
        JsonArray candidatos = json.has("candidates") ? json.getAsJsonArray("candidates") : new JsonArray();
        if (candidatos.isEmpty() || !candidatos.get(0).getAsJsonObject().has("content")) {
            throw new AssistantException("El asistente no pudo responder a ese mensaje. Intente decirlo de otra forma.");
        }
        JsonObject candidato = candidatos.get(0).getAsJsonObject();
        JsonObject contenido = candidato.getAsJsonObject("content");
        if (!contenido.has("parts") || contenido.getAsJsonArray("parts").isEmpty()) {
            AssistantLog.registrar("Respuesta vacía de " + modelo + " (finishReason="
                    + (candidato.has("finishReason") ? candidato.get("finishReason").getAsString() : "?") + ")");
            throw new AssistantException("El asistente no devolvió respuesta. Intente de nuevo o dígalo de otra forma.");
        }
        if (!contenido.has("role")) {
            contenido.addProperty("role", "model");
        }
        StringBuilder texto = new StringBuilder();
        List<FunctionCall> llamadas = new ArrayList<>();
        JsonArray partes = contenido.has("parts") ? contenido.getAsJsonArray("parts") : new JsonArray();
        for (JsonElement elemento : partes) {
            JsonObject parte = elemento.getAsJsonObject();
            boolean pensamiento = parte.has("thought") && parte.get("thought").getAsBoolean();
            if (parte.has("text") && !pensamiento) {
                texto.append(parte.get("text").getAsString());
            }
            if (parte.has("functionCall")) {
                JsonObject llamada = parte.getAsJsonObject("functionCall");
                llamadas.add(new FunctionCall(
                        llamada.has("id") ? llamada.get("id").getAsString() : null,
                        llamada.get("name").getAsString(),
                        llamada.has("args") ? llamada.getAsJsonObject("args") : new JsonObject()));
            }
        }
        return new ModelReply(contenido, texto.toString().trim(), llamadas);
    }

    /** Explica en palabras simples por qué no se pudo conectar. */
    private static String mensajeDeConexion(IOException e) {
        Throwable causa = e;
        while (causa != null) {
            if (causa instanceof javax.net.ssl.SSLException || causa instanceof java.security.cert.CertificateException) {
                return "La conexión segura con Google fue rechazada (certificado). Suele pasar con antivirus que "
                        + "revisan las conexiones o redes con filtro. Detalle: " + causa.getClass().getSimpleName();
            }
            if (causa instanceof java.net.UnknownHostException) {
                return "No hay internet o no se encuentra el servidor de Google. Revise la conexión.";
            }
            if (causa instanceof java.net.http.HttpConnectTimeoutException || causa instanceof java.net.ConnectException) {
                return "No se pudo conectar con Google (tiempo agotado o conexión bloqueada por firewall o proxy).";
            }
            causa = causa.getCause();
        }
        return "No hay conexión con el asistente (" + e.getClass().getSimpleName() + "). Revise el internet e intente de nuevo.";
    }

    private static String mensajeDeError(int codigo, String cuerpo, String modelo) {
        String detalle = "";
        try {
            JsonObject error = JsonParser.parseString(cuerpo).getAsJsonObject().getAsJsonObject("error");
            detalle = error.get("message").getAsString();
        } catch (RuntimeException ignorado) {
            // el cuerpo no era JSON; se usa solo el código
        }
        if (codigo == 429) {
            return "Se alcanzó el límite gratuito del asistente por ahora. Espere un minuto e intente de nuevo.";
        }
        if (codigo == 400 && detalle.toLowerCase().contains("api key")) {
            return "La clave del asistente no es válida. Revísela en la configuración del asistente.";
        }
        if (codigo == 401 || codigo == 403) {
            return "Google rechazó la clave del asistente (" + codigo + "). Revise que esté bien copiada y activa.";
        }
        if (codigo == 404) {
            return "Google no ofrece el modelo " + modelo + " para esta clave y no se encontró otro compatible. "
                    + "Elija otro modelo en la configuración del asistente.";
        }
        if (codigo >= 500) {
            return "El servicio del asistente está ocupado. Intente de nuevo en un momento.";
        }
        return "El asistente respondió con un error (" + codigo + ")" + (detalle.isEmpty() ? "." : ": " + detalle);
    }
}
