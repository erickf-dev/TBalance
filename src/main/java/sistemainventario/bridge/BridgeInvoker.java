package sistemainventario.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ejecuta un método público de {@link JavaBridge} a partir de su nombre y de sus
 * argumentos en JSON, y devuelve el resultado también en JSON.
 *
 * <p>Lo usa el servidor local cuando la interfaz se muestra en Edge: la página
 * llama {@code window.javaBridge.metodo(...)} igual que en JavaFX, y esa llamada
 * llega aquí como texto.</p>
 *
 * <p><b>SRP:</b> solo traduce JSON a una llamada Java y el resultado a JSON.
 * <b>Seguridad:</b> solo se pueden llamar métodos públicos declarados en
 * {@link JavaBridge} (no los heredados de {@code Object}).</p>
 */
public class BridgeInvoker {

    private final Object puente;
    private final AccessPolicy politica;
    private final Gson gson = new Gson();
    /** Métodos públicos del puente agrupados por nombre (puede haber sobrecargas). */
    private final Map<String, List<Method>> metodos = new HashMap<>();

    /**
     * @param puente objeto cuyos métodos públicos se exponen a la página
     */
    public BridgeInvoker(Object puente) {
        this(puente, null);
    }

    /**
     * @param puente   objeto cuyos métodos públicos se exponen a la página
     * @param politica revisa la sesión y el permiso de cada llamada ({@code null}: sin revisar)
     */
    public BridgeInvoker(Object puente, AccessPolicy politica) {
        this.puente = puente;
        this.politica = politica;
        for (Method m : puente.getClass().getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) {
                metodos.computeIfAbsent(m.getName(), k -> new java.util.ArrayList<>()).add(m);
            }
        }
    }

    /**
     * @param nombre        nombre del método de {@link JavaBridge}
     * @param argumentosJson arreglo JSON con los argumentos, por ejemplo {@code ["123", 4]}
     * @return objeto JSON {@code {"ok":true,"v":resultado}} o {@code {"ok":false,"error":"..."}}
     */
    public String invocar(String nombre, String argumentosJson) {
        if (LOTE.equals(nombre)) {
            return invocarLote(argumentosJson);
        }
        JsonObject respuesta = new JsonObject();
        try {
            JsonArray argumentos = argumentosJson == null || argumentosJson.isBlank()
                    ? new JsonArray() : JsonParser.parseString(argumentosJson).getAsJsonArray();
            Method metodo = buscar(nombre, argumentos.size());
            Class<?>[] tipos = metodo.getParameterTypes();
            Object[] valores = new Object[tipos.length];
            for (int i = 0; i < tipos.length; i++) {
                valores[i] = convertir(argumentos.get(i), tipos[i]);
            }
            AuditTrail.Momento momento = politica == null ? null : politica.antesDeLlamar();
            Optional<String> rechazo = politica == null ? Optional.empty() : politica.revisar(nombre);
            if (rechazo.isPresent()) {
                politica.anotar(nombre, argumentos, null, rechazo.get(), momento);
                // mismo formato que los errores de los controladores: la página ya los muestra
                respuesta.addProperty("ok", true);
                respuesta.addProperty("v", "ERROR: " + rechazo.get());
                respuesta.addProperty("sinPermiso", true);
                return gson.toJson(respuesta);
            }
            Object resultado = metodo.invoke(puente, valores);
            if (politica != null) {
                politica.anotar(nombre, argumentos, resultado, null, momento);
                resultado = politica.filtrar(nombre, resultado);
            }
            respuesta.addProperty("ok", true);
            respuesta.add("v", resultado == null ? null : gson.toJsonTree(resultado));
        } catch (InvocationTargetException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            respuesta.addProperty("ok", false);
            respuesta.addProperty("error", String.valueOf(causa.getMessage()));
        } catch (RuntimeException | IllegalAccessException e) {
            respuesta.addProperty("ok", false);
            respuesta.addProperty("error", String.valueOf(e.getMessage()));
        }
        return gson.toJson(respuesta);
    }

    /** Nombre especial para pedir varias lecturas en un solo viaje. */
    public static final String LOTE = "__lote";

    /**
     * Ejecuta varias llamadas seguidas y devuelve sus respuestas en el mismo
     * orden. Cada una pasa por la misma revisión de permisos que si llegara
     * sola; así la pantalla recarga todo con un viaje en lugar de veinte.
     *
     * @param argumentosJson {@code [["obtenerProductos", []], ["obtenerVentas", []], ...]}
     * @return arreglo JSON con la respuesta de cada llamada
     */
    private String invocarLote(String argumentosJson) {
        JsonArray salida = new JsonArray();
        JsonArray llamadas;
        try {
            JsonArray args = JsonParser.parseString(argumentosJson).getAsJsonArray();
            llamadas = args.size() == 1 && args.get(0).isJsonArray() && args.get(0).getAsJsonArray().size() > 0
                    && args.get(0).getAsJsonArray().get(0).isJsonArray() ? args.get(0).getAsJsonArray() : args;
        } catch (RuntimeException e) {
            JsonObject error = new JsonObject();
            error.addProperty("ok", false);
            error.addProperty("error", "Lote mal formado");
            return gson.toJson(error);
        }
        for (JsonElement llamada : llamadas) {
            JsonArray partes = llamada.getAsJsonArray();
            String metodo = partes.get(0).getAsString();
            String argumentos = partes.size() > 1 ? partes.get(1).toString() : "[]";
            salida.add(LOTE.equals(metodo) ? JsonParser.parseString("{\"ok\":false,\"error\":\"Lote dentro de lote\"}")
                    : JsonParser.parseString(invocar(metodo, argumentos)));
        }
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("ok", true);
        respuesta.add("v", salida);
        return gson.toJson(respuesta);
    }

    private Method buscar(String nombre, int cantidad) {
        List<Method> candidatos = metodos.get(nombre);
        if (candidatos != null) {
            for (Method m : candidatos) {
                if (m.getParameterCount() == cantidad) {
                    return m;
                }
            }
        }
        throw new IllegalArgumentException("El puente no tiene el método " + nombre + " con " + cantidad + " argumentos");
    }

    private Object convertir(JsonElement valor, Class<?> tipo) {
        if (valor == null || valor.isJsonNull()) {
            if (tipo == boolean.class) {
                return false;
            }
            return tipo.isPrimitive() ? gson.fromJson("0", tipo) : null;
        }
        if (tipo == String.class) {
            return valor.isJsonPrimitive() ? valor.getAsString() : valor.toString();
        }
        if (tipo == int.class || tipo == Integer.class) {
            return (int) Math.round(valor.getAsDouble());
        }
        if (tipo == long.class || tipo == Long.class) {
            return Math.round(valor.getAsDouble());
        }
        return gson.fromJson(valor, tipo);
    }
}
