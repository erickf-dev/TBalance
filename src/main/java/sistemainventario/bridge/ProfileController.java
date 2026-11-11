package sistemainventario.bridge;

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

    public String listarPerfiles() {
        return BridgeResponse.json(perfiles().listarTodos());
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
            Profile creado = perfiles().crear(JsonUtil.fromJson(jsonPerfil, Profile.class));
            notificador.ejecutarScript("onPerfilesActualizados()");
            return BridgeResponse.json(creado);
        });
    }

    public String editarPerfil(String jsonPerfil) {
        return BridgeResponse.ejecutarOk(() -> {
            perfiles().actualizar(JsonUtil.fromJson(jsonPerfil, Profile.class));
            notificador.ejecutarScript("onPerfilesActualizados()");
        });
    }

    public String eliminarPerfil(String id) {
        return BridgeResponse.ejecutarOk(() -> {
            contexto.eliminarPerfil(id);
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

    /** @return el tipo de negocio del perfil activo, o {@code NINGUNO} */
    private BusinessType tipoNegocioActivo() {
        return perfiles().getPerfilActivo().map(Profile::getPerfilNegocio).orElse(BusinessType.NINGUNO);
    }

    private ProfileService perfiles() {
        return contexto.getGestorPerfiles();
    }
}
