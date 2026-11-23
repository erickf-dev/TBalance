package sistemainventario.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sistemainventario.AppContext;
import sistemainventario.model.AuditEntry;
import sistemainventario.model.Profile;
import sistemainventario.model.Rol;
import sistemainventario.model.User;
import sistemainventario.util.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decide qué llamadas del puente quedan en el historial y arma el texto de cada
 * una. Como toda la interfaz pasa por {@link BridgeInvoker}, aquí se anota todo
 * lo que cambia datos sin tocar cada controlador.
 *
 * <p>Nunca guarda contraseñas: el detalle se arma solo con una lista cerrada de campos.</p>
 */
public class AuditTrail {

    /** Acciones que se anotan y su nombre para el historial. */
    private static final Map<String, String> ACCIONES = Map.ofEntries(
            Map.entry("crearVenta", "Registró una venta"),
            Map.entry("anularVenta", "Anuló una venta"),
            Map.entry("registrarEntrada", "Registró una entrada"),
            Map.entry("anularEntrada", "Anuló una entrada"),
            Map.entry("registrarSalida", "Registró una salida"),
            Map.entry("anularSalida", "Anuló una salida"),
            Map.entry("registrarAbono", "Registró un abono de un fiado"),
            Map.entry("anularAbono", "Anuló un abono de un fiado"),
            Map.entry("guardarProducto", "Guardó un producto"),
            Map.entry("cambiarEstadoProducto", "producto"),
            Map.entry("guardarCategoria", "Guardó una categoría"),
            Map.entry("cambiarEstadoCategoria", "categoría"),
            Map.entry("guardarProveedor", "Guardó un proveedor"),
            Map.entry("cambiarEstadoProveedor", "proveedor"),
            Map.entry("guardarUsuario", "Guardó un usuario"),
            Map.entry("cambiarEstadoUsuario", "usuario"),
            Map.entry("restablecerClaveUsuario", "Le puso contraseña nueva a un usuario"),
            Map.entry("cambiarMiClave", "Cambió su contraseña"),
            Map.entry("crearPerfil", "Creó un negocio"),
            Map.entry("editarPerfil", "Editó un negocio"),
            Map.entry("guardarQrCobro", "Cambió el QR de cobro de un negocio"),
            Map.entry("deshabilitarPerfil", "Deshabilitó un negocio"),
            Map.entry("habilitarPerfil", "Habilitó un negocio"),
            Map.entry("crearRespaldo", "Creó una copia de seguridad"),
            Map.entry("restaurarRespaldo", "Restauró una copia de seguridad"),
            Map.entry("eliminarRespaldo", "Eliminó una copia de seguridad"),
            Map.entry("guardarAlmacenamiento", "Cambió dónde se guardan los datos"),
            Map.entry("asistenteConfigurar", "Configuró el asistente"),
            Map.entry("asistenteConfirmar", "Confirmó una acción del asistente"),
            Map.entry("exportarInformeExcel", "Exportó un informe"),
            Map.entry("crearPrimerDueno", "Creó la cuenta del dueño"),
            Map.entry("iniciarSesion", "Inició sesión"),
            Map.entry("cerrarSesion", "Cerró sesión"));

    /** Campos que se pueden copiar al detalle (nunca la clave). */
    private static final List<String> CAMPOS = List.of("nombre", "nombreProducto", "productoNombre", "cantidad",
            "total", "monto", "metodoPago", "cliente", "clienteDocumento", "tipo", "motivo", "usuario", "rol", "referencia");

    /** Quién y dónde, tomado antes de la llamada (cerrar sesión, por ejemplo, la borra). */
    public record Momento(Optional<User> persona, String perfilId) {
    }

    private final AppContext contexto;

    /** @param contexto contexto de la aplicación */
    public AuditTrail(AppContext contexto) {
        this.contexto = contexto;
    }

    /** @return quién está y en qué negocio, justo antes de una llamada */
    public Momento antes() {
        return new Momento(contexto.getSesion().actual(), contexto.getPerfilActivoId());
    }

    /**
     * Anota la llamada si es de las que cambian datos y salió bien, o si fue un
     * intento sin permiso o un ingreso fallido.
     *
     * @param metodo      método llamado
     * @param argumentos  argumentos en JSON
     * @param resultado   respuesta del método ({@code null} si la política la rechazó)
     * @param rechazo     motivo del rechazo de la política, o {@code null}
     * @param momento     quién y dónde antes de la llamada
     */
    public void anotar(String metodo, JsonArray argumentos, Object resultado, String rechazo, Momento momento) {
        String accion = ACCIONES.get(metodo);
        if (accion == null || contexto.getAuditoria() == null) {
            return;
        }
        String texto = resultado == null ? "" : String.valueOf(resultado);
        boolean fallo = texto.startsWith("ERROR");
        Optional<User> persona = momento.persona().isPresent() ? momento.persona() : contexto.getSesion().actual();
        if ("iniciarSesion".equals(metodo)) {
            String usuario = argumento(argumentos, 0);
            if (fallo) {
                registrar(Optional.empty(), null, "Intento de ingreso fallido",
                        "Usuario: " + usuario + ". " + texto.replaceFirst("^ERROR:\\s*", ""));
            } else {
                registrar(contexto.getSesion().actual(), null, accion, "Usuario: " + usuario);
            }
            return;
        }
        if (rechazo != null) {
            registrar(persona, momento.perfilId(), "Intentó sin permiso", nombreLegible(metodo, argumentos) + ". " + rechazo);
            return;
        }
        if (fallo) {
            return;
        }
        String perfil = esGeneral(metodo) ? null : momento.perfilId();
        registrar(persona, perfil, nombreLegible(metodo, argumentos), detalle(metodo, argumentos, texto));
    }

    private void registrar(Optional<User> persona, String perfilId, String accion, String detalle) {
        String nombreNegocio = perfilId == null ? null
                : contexto.getGestorPerfiles().buscarPorId(perfilId).map(Profile::getNombre).orElse(perfilId);
        String rol = persona.flatMap(p -> perfilId == null ? Optional.<Rol>empty() : p.rolEn(perfilId))
                .map(Rol::getEtiqueta).orElse(persona.map(AuditTrail::rolGeneral).orElse(""));
        String corto = detalle == null ? "" : detalle.length() > 480 ? detalle.substring(0, 480) + "…" : detalle;
        contexto.getAuditoria().registrar(new AuditEntry(DateUtil.ahora(), persona.map(User::getId).orElse(null),
                persona.map(User::getNombre).orElse(""), rol, perfilId, nombreNegocio, accion, corto));
    }

    /** Fuera de un negocio: dueño si lo es de alguno; si no, el rol que tiene en sus negocios. */
    private static String rolGeneral(User persona) {
        if (persona.esDuenoDeAlguno()) {
            return Rol.DUENO.getEtiqueta();
        }
        return persona.getAccesos().stream().map(a -> a.getRol().getEtiqueta()).findFirst().orElse("");
    }

    private static boolean esGeneral(String metodo) {
        return metodo.contains("Respaldo") || metodo.equals("guardarAlmacenamiento") || metodo.equals("cerrarSesion")
                || metodo.equals("cambiarMiClave") || metodo.equals("crearPrimerDueno") || metodo.equals("asistenteConfigurar");
    }

    private static String nombreLegible(String metodo, JsonArray argumentos) {
        String accion = ACCIONES.get(metodo);
        if (metodo.startsWith("cambiarEstado")) {
            boolean activo = "true".equalsIgnoreCase(argumento(argumentos, 1));
            return (activo ? "Habilitó " : "Deshabilitó ") + (accion.equals("categoría") ? "una " : "un ") + accion;
        }
        if (metodo.startsWith("guardar") && argumentos.size() > 0) {
            JsonObject datos = objeto(argumentos.get(0));
            boolean nuevo = datos == null || !datos.has("id") || datos.get("id").isJsonNull() || datos.get("id").getAsString().isBlank();
            if (accion.startsWith("Guardó")) {
                return (nuevo ? "Creó" : "Editó") + accion.substring("Guardó".length());
            }
        }
        return accion;
    }

    private static String detalle(String metodo, JsonArray argumentos, String resultado) {
        List<String> partes = new ArrayList<>();
        if (metodo.startsWith("anular")) {
            partes.add("ID " + argumento(argumentos, 0));
            String motivo = argumento(argumentos, 1);
            if (!motivo.isBlank()) {
                partes.add("Motivo: " + motivo);
            }
            return String.join(". ", partes);
        }
        if (metodo.startsWith("cambiarEstado") || metodo.equals("restablecerClaveUsuario")
                || metodo.endsWith("Perfil") && !metodo.equals("editarPerfil") && !metodo.equals("crearPerfil")) {
            return "ID " + argumento(argumentos, 0);
        }
        if (metodo.contains("Respaldo")) {
            return argumento(argumentos, 0);
        }
        if (metodo.equals("exportarInformeExcel")) {
            return "Del " + argumento(argumentos, 0) + " al " + argumento(argumentos, 1);
        }
        JsonObject datos = objeto(resultado);
        if (datos == null && argumentos.size() > 0) {
            datos = objeto(argumentos.get(0));
        }
        if (datos == null) {
            return "";
        }
        if (datos.has("id") && datos.get("id").isJsonPrimitive()) {
            partes.add("ID " + datos.get("id").getAsString());
        }
        for (String campo : CAMPOS) {
            JsonElement valor = datos.get(campo);
            if (valor != null && valor.isJsonPrimitive() && !valor.getAsString().isBlank()
                    && !(campo.equals("clienteDocumento") && valor.getAsString().startsWith("N:"))) {
                boolean dinero = campo.equals("total") || campo.equals("monto");
                partes.add(etiqueta(campo) + ": " + (dinero ? pesos(valor.getAsDouble()) : valor.getAsString()));
            }
        }
        JsonElement items = datos.get("items");
        if (items != null && items.isJsonArray()) {
            List<String> productos = new ArrayList<>();
            for (JsonElement item : items.getAsJsonArray()) {
                JsonObject i = objeto(item);
                if (i != null && i.has("nombreProducto")) {
                    productos.add((i.has("cantidad") ? i.get("cantidad").getAsString() + " " : "") + i.get("nombreProducto").getAsString());
                }
            }
            if (!productos.isEmpty()) {
                partes.add("Productos: " + String.join(", ", productos));
            }
        }
        return String.join(". ", partes);
    }

    private static String etiqueta(String campo) {
        return switch (campo) {
            case "nombreProducto", "productoNombre" -> "Producto";
            case "metodoPago" -> "Pago";
            case "monto" -> "Valor";
            case "referencia" -> "Comprobante";
            case "clienteDocumento" -> "Cédula";
            default -> Character.toUpperCase(campo.charAt(0)) + campo.substring(1);
        };
    }

    private static String pesos(double valor) {
        return "$ " + String.format(java.util.Locale.forLanguageTag("es-CO"), "%,.0f", valor);
    }

    private static String argumento(JsonArray argumentos, int i) {
        if (argumentos == null || argumentos.size() <= i || argumentos.get(i).isJsonNull()) {
            return "";
        }
        JsonElement e = argumentos.get(i);
        return e.isJsonPrimitive() ? e.getAsString() : e.toString();
    }

    private static JsonObject objeto(Object valor) {
        try {
            JsonElement e = valor instanceof JsonElement je ? je : JsonParser.parseString(String.valueOf(valor));
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                e = JsonParser.parseString(e.getAsString());
            }
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (RuntimeException noEsJson) {
            return null;
        }
    }
}
