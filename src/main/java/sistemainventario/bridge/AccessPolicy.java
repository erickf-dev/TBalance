package sistemainventario.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sistemainventario.AppContext;
import sistemainventario.model.Permiso;
import sistemainventario.model.Rol;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Revisa en Java, antes de cada llamada de la página, que la persona tenga la
 * sesión abierta y el permiso que pide esa acción en el negocio abierto.
 *
 * <p>La interfaz ya oculta lo que el rol no puede usar, pero eso no basta: una
 * página se puede modificar. Por eso el puente vuelve a decidir aquí (<b>GRASP
 * Indirección / Protected Variations</b>: toda la política de acceso vive en un
 * solo lugar y los controladores no la repiten).</p>
 */
public class AccessPolicy {

    /** Métodos que se pueden llamar sin sesión (pantalla de inicio de sesión). */
    private static final Set<String> SIN_SESION = Set.of(
            "estadoSesion", "iniciarSesion", "crearPrimerDueno", "cerrarSesion",
            "transicionPantalla", "mostrarToast", "obtenerTiposNegocio", "revisarCambios",
            "conectarBaseExistente");

    /** Permiso que pide cada método; los que no están aquí solo piden sesión. */
    private static final Map<String, Permiso> PERMISOS = Map.ofEntries(
            Map.entry("crearVenta", Permiso.VENDER),
            Map.entry("iniciarEscaneoQR", Permiso.VENDER),
            Map.entry("iniciarEscaneoQRCon", Permiso.VENDER),
            Map.entry("prepararCelular", Permiso.VENDER),
            Map.entry("guardarProducto", Permiso.EDITAR_CATALOGO),
            Map.entry("guardarCategoria", Permiso.EDITAR_CATALOGO),
            Map.entry("guardarProveedor", Permiso.EDITAR_CATALOGO),
            Map.entry("obtenerEtiquetaQR", Permiso.EDITAR_CATALOGO),
            Map.entry("guardarEtiquetaQR", Permiso.EDITAR_CATALOGO),
            Map.entry("generarHojaEtiquetas", Permiso.EDITAR_CATALOGO),
            Map.entry("cambiarEstadoProducto", Permiso.DESHABILITAR),
            Map.entry("cambiarEstadoCategoria", Permiso.DESHABILITAR),
            Map.entry("cambiarEstadoProveedor", Permiso.DESHABILITAR),
            Map.entry("obtenerProductosDeshabilitados", Permiso.DESHABILITAR),
            Map.entry("obtenerCategoriasDeshabilitadas", Permiso.DESHABILITAR),
            Map.entry("obtenerProveedoresDeshabilitados", Permiso.DESHABILITAR),
            Map.entry("obtenerEntradas", Permiso.MOVIMIENTOS),
            Map.entry("obtenerSalidas", Permiso.MOVIMIENTOS),
            Map.entry("registrarEntrada", Permiso.MOVIMIENTOS),
            Map.entry("registrarSalida", Permiso.MOVIMIENTOS),
            Map.entry("obtenerEntradasAnuladas", Permiso.MOVIMIENTOS),
            Map.entry("obtenerSalidasAnuladas", Permiso.MOVIMIENTOS),
            Map.entry("anularEntrada", Permiso.ANULAR),
            Map.entry("anularSalida", Permiso.ANULAR),
            Map.entry("obtenerInforme", Permiso.INFORMES),
            Map.entry("exportarInformeExcel", Permiso.INFORMES),
            Map.entry("obtenerCostos", Permiso.COSTOS),
            Map.entry("listarFiados", Permiso.VENDER),
            Map.entry("resumenFiados", Permiso.VENDER),
            Map.entry("obtenerQrCobro", Permiso.VENDER),
            Map.entry("obtenerFiado", Permiso.VENDER),
            Map.entry("registrarAbono", Permiso.VENDER),
            Map.entry("anularAbono", Permiso.ANULAR),
            Map.entry("listarUsuarios", Permiso.USUARIOS),
            Map.entry("guardarUsuario", Permiso.USUARIOS),
            Map.entry("cambiarEstadoUsuario", Permiso.USUARIOS),
            Map.entry("restablecerClaveUsuario", Permiso.USUARIOS),
            Map.entry("listarRespaldos", Permiso.RESPALDOS),
            Map.entry("crearRespaldo", Permiso.RESPALDOS),
            Map.entry("restaurarRespaldo", Permiso.RESPALDOS),
            Map.entry("eliminarRespaldo", Permiso.RESPALDOS),
            Map.entry("obtenerCarpetaRespaldos", Permiso.RESPALDOS),
            Map.entry("obtenerAlmacenamiento", Permiso.CONFIGURACION),
            Map.entry("probarAlmacenamiento", Permiso.CONFIGURACION),
            Map.entry("guardarAlmacenamiento", Permiso.CONFIGURACION),
            Map.entry("asistenteConfigurar", Permiso.CONFIGURACION),
            Map.entry("asistenteProbarClave", Permiso.CONFIGURACION),
            Map.entry("listarHistorial", Permiso.HISTORIAL));

    /**
     * Métodos que no necesitan un negocio abierto (los negocios se revisan uno por
     * uno en {@link ProfileController}). Crear el primer negocio, por ejemplo.
     */
    private static final Set<String> SIN_NEGOCIO = Set.of(
            "listarPerfiles", "obtenerPerfilActivo", "seleccionarPerfil", "crearPerfil", "editarPerfil",
            "deshabilitarPerfil", "habilitarPerfil", "cambiarMiClave", "recargarEstado", "asistenteEstado");

    /** Respuestas que llevan precios de compra o ganancias: se limpian si el rol no los ve. */
    private static final Set<String> CON_COSTOS = Set.of(
            "obtenerProductos", "buscarProductos", "filtrarProductosPorCategoria", "obtenerProductosVencidos",
            "obtenerProductosPorVencer", "obtenerVentas", "obtenerVentasAnuladas", "obtenerEstadisticas",
            "listarFiados", "obtenerFiado");
    private static final Set<String> CAMPOS_COSTO = Set.of("precioCompra", "costoUnitario", "utilidadEstimada", "utilidad");

    private final AppContext contexto;

    private final AuditTrail historial;

    /** @param contexto contexto de la aplicación */
    public AccessPolicy(AppContext contexto) {
        this.contexto = contexto;
        this.historial = new AuditTrail(contexto);
    }

    /** @return quién y dónde, justo antes de una llamada (para el historial) */
    public AuditTrail.Momento antesDeLlamar() {
        return historial.antes();
    }

    /**
     * Anota en el historial la llamada si corresponde. Un error aquí nunca
     * interrumpe lo que la persona estaba haciendo.
     */
    public void anotar(String metodo, com.google.gson.JsonArray argumentos, Object resultado, String rechazo,
                       AuditTrail.Momento momento) {
        try {
            historial.anotar(metodo, argumentos, resultado, rechazo, momento);
        } catch (RuntimeException e) {
            System.err.println("[TBalance] No se pudo anotar en el historial: " + e.getMessage());
        }
    }

    /**
     * @param metodo nombre del método del puente
     * @return mensaje de rechazo, o vacío si se puede llamar
     */
    public Optional<String> revisar(String metodo) {
        if (SIN_SESION.contains(metodo)) {
            return Optional.empty();
        }
        if (!contexto.getSesion().registrarActividad()) {
            return Optional.of("Su sesión se cerró. Vuelva a iniciar sesión.");
        }
        if (SIN_NEGOCIO.contains(metodo)) {
            return Optional.empty();
        }
        Optional<Rol> rol = contexto.rolActual();
        if (rol.isEmpty()) {
            return contexto.getPerfilActivoId() == null ? Optional.empty() : Optional.of("No tiene acceso a este negocio");
        }
        Permiso pedido = PERMISOS.get(metodo);
        if (pedido != null && !rol.get().puede(pedido)) {
            return Optional.of("Su rol (" + rol.get().getEtiqueta() + ") no permite esta acción");
        }
        return Optional.empty();
    }

    /**
     * Quita precios de compra y ganancias de la respuesta si el rol no los puede ver
     * (el cajero ve precios de venta, pero no cuánto costó la mercancía).
     *
     * @param metodo    método llamado
     * @param resultado respuesta del método
     * @return la respuesta, limpia si hace falta
     */
    public Object filtrar(String metodo, Object resultado) {
        if (!CON_COSTOS.contains(metodo) || !(resultado instanceof String texto)
                || contexto.rolActual().map(r -> r.puede(Permiso.VER_GANANCIA)).orElse(true)) {
            return resultado;
        }
        try {
            JsonElement datos = JsonParser.parseString(texto);
            limpiar(datos);
            return datos.toString();
        } catch (RuntimeException noEsJson) {
            return resultado;
        }
    }

    private static void limpiar(JsonElement elemento) {
        if (elemento instanceof JsonObject objeto) {
            CAMPOS_COSTO.forEach(objeto::remove);
            objeto.entrySet().forEach(e -> limpiar(e.getValue()));
        } else if (elemento instanceof JsonArray lista) {
            lista.forEach(AccessPolicy::limpiar);
        }
    }
}
