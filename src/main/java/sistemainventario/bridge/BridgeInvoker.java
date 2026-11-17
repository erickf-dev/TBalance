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
    private final Gson gson = new Gson();
    /** Métodos públicos del puente agrupados por nombre (puede haber sobrecargas). */
    private final Map<String, List<Method>> metodos = new HashMap<>();

    /**
     * @param puente objeto cuyos métodos públicos se exponen a la página
     */
    public BridgeInvoker(Object puente) {
        this.puente = puente;
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
            Object resultado = metodo.invoke(puente, valores);
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
