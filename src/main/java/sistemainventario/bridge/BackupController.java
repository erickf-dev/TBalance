package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.service.BackupService;

/**
 * <b>GRASP Controller</b> de los casos de uso de copias de seguridad.
 *
 * <p>Las copias no dependen del perfil activo: incluyen todos los perfiles.</p>
 */
public class BackupController {

    private final AppContext contexto;
    private final ViewNotifier notificador;

    public BackupController(AppContext contexto, ViewNotifier notificador) {
        this.contexto = contexto;
        this.notificador = notificador;
    }

    public String listarRespaldos() {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(respaldos().listar()));
    }

    public String crearRespaldo() {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(respaldos().crear()));
    }

    /**
     * Restaura un respaldo y pide a la vista que recargue todos los datos.
     *
     * @param nombre archivo .zip
     * @return JSON con la copia automática creada antes de restaurar, o un error
     */
    public String restaurarRespaldo(String nombre) {
        return BridgeResponse.ejecutar(() -> {
            String copiaPrevia = BridgeResponse.json(contexto.restaurarRespaldo(nombre));
            notificador.ejecutarScript("onPerfilesActualizados()");
            return copiaPrevia;
        });
    }

    public String eliminarRespaldo(String nombre) {
        return BridgeResponse.ejecutarOk(() -> respaldos().eliminar(nombre));
    }

    public String obtenerCarpetaRespaldos() {
        return respaldos().getCarpetaRespaldos().toAbsolutePath().toString();
    }

    private BackupService respaldos() {
        return contexto.getGestorRespaldo();
    }
}
