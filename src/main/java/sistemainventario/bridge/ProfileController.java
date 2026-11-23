package sistemainventario.bridge;

import sistemainventario.exception.ValidationException;
import sistemainventario.AppContext;
import sistemainventario.model.Profile;
import sistemainventario.model.BusinessType;
import sistemainventario.service.ProfileService;
import sistemainventario.util.JsonUtil;
import sistemainventario.util.TextUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b>GRASP Controller</b> de los casos de uso de perfiles: recibe los
 * eventos de la vista, delega en {@link ProfileService} y avisa a la vista
 * cuando cambia el estado.
 */
public class ProfileController {

    private final AppContext contexto;
    private final ViewNotifier notificador;

    public ProfileController(AppContext contexto, ViewNotifier notificador) {
        this.contexto = contexto;
        this.notificador = notificador;
    }

    /** @return negocios a los que entra la persona con sesión (también los deshabilitados, para el dueño) */
    public String listarPerfiles() {
        return BridgeResponse.json(contexto.perfilesPermitidos());
    }

    public String obtenerPerfilActivo() {
        return perfiles().getPerfilActivo().map(BridgeResponse::json).orElse("{}");
    }

    public String obtenerSugerencias() {
        return BridgeResponse.json(tipoNegocioActivo().getSugerencias());
    }

    public boolean requiereControlVencimiento() {
        return perfiles().getPerfilActivo().map(Profile::requiereControlVencimiento).orElse(false);
    }

    public String obtenerTiposNegocio() {
        List<Map<String, Object>> tipos = Arrays.stream(BusinessType.values())
                .map(BusinessType::describir)
                .collect(Collectors.toList());
        return BridgeResponse.json(tipos);
    }

    public String crearPerfil(String jsonPerfil) {
        return BridgeResponse.ejecutar(() -> {
            if (!contexto.puedeCrearNegocios()) {
                throw new ValidationException("Solo el dueño puede crear negocios");
            }
            Profile creado = perfiles().crear(JsonUtil.fromJson(jsonPerfil, Profile.class));
            contexto.registrarNegocioCreado(creado.getId());
            notificador.ejecutarScript("onPerfilesActualizados()");
            return BridgeResponse.json(creado);
        });
    }

    public String editarPerfil(String jsonPerfil) {
        return BridgeResponse.ejecutarOk(() -> {
            Profile cambios = JsonUtil.fromJson(jsonPerfil, Profile.class);
            exigirDueno(cambios == null ? null : cambios.getId());
            perfiles().actualizar(cambios);
            notificador.ejecutarScript("onPerfilesActualizados()");
        });
    }

    /**
     * Borrado lógico del negocio: queda deshabilitado con todos sus datos.
     *
     * @param id ID del perfil
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String deshabilitarPerfil(String id) {
        return BridgeResponse.ejecutarOk(() -> {
            exigirDueno(id);
            contexto.deshabilitarPerfil(id);
            notificador.ejecutarScript("onPerfilesActualizados()");
        });
    }

    /**
     * @param id ID del perfil
     * @return {@code "OK"} o {@code "ERROR: ..."}
     */
    public String habilitarPerfil(String id) {
        return BridgeResponse.ejecutarOk(() -> {
            exigirDueno(id);
            contexto.habilitarPerfil(id);
            notificador.ejecutarScript("onPerfilesActualizados()");
        });
    }

    public String seleccionarPerfil(String id) {
        String perfilId = "null".equals(id) || TextUtil.esVacio(id) ? null : id;
        return BridgeResponse.ejecutarOk(() -> {
            contexto.activarPerfil(perfilId);
            notificador.ejecutarScript("onPerfilSeleccionado(" + BridgeResponse.literalJs(perfilId) + ")");
        });
    }

    /**
     * Guarda la imagen del QR de cobro (Bre-B) de un negocio, o la quita si llega vacía.
     *
     * @param perfilId negocio
     * @param dataUrl  imagen como "data:image/png;base64,..." (vacío para quitarla)
     * @return OK o el error
     */
    public String guardarQrCobro(String perfilId, String dataUrl) {
        return BridgeResponse.ejecutarOk(() -> {
            exigirDueno(perfilId);
            if (TextUtil.esVacio(dataUrl) || "null".equals(dataUrl)) {
                contexto.getQrCobro().eliminar(perfilId);
                return;
            }
            int coma = dataUrl.indexOf(',');
            if (!dataUrl.startsWith("data:image/") || coma < 0) {
                throw new ValidationException("Suba la imagen del QR en PNG o JPG");
            }
            byte[] bytes;
            try {
                bytes = java.util.Base64.getDecoder().decode(dataUrl.substring(coma + 1));
            } catch (IllegalArgumentException e) {
                throw new ValidationException("La imagen del QR está dañada");
            }
            contexto.getQrCobro().guardar(perfilId, bytes);
        });
    }

    /**
     * @param perfilId negocio ({@code null} o vacío para el abierto)
     * @return la imagen del QR de cobro como data URL, o vacío si no tiene
     */
    public String obtenerQrCobro(String perfilId) {
        return BridgeResponse.ejecutar(() -> {
            String id = TextUtil.esVacio(perfilId) || "null".equals(perfilId) ? contexto.getPerfilActivoId() : perfilId;
            return id == null ? "" : contexto.getQrCobro().leerComoDataUrl(id).orElse("");
        });
    }

    private void exigirDueno(String perfilId) {
        if (perfilId == null || !contexto.esDuenoDe(perfilId)) {
            throw new ValidationException("Solo el dueño del negocio puede hacer esto");
        }
    }

    /** @return el tipo de negocio del perfil activo, o {@code NINGUNO} */
    private BusinessType tipoNegocioActivo() {
        return perfiles().getPerfilActivo().map(Profile::getPerfilNegocio).orElse(BusinessType.NINGUNO);
    }

    private ProfileService perfiles() {
        return contexto.getGestorPerfiles();
    }
}
