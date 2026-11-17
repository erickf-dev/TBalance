package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.Profile;
import sistemainventario.model.SalesReport;
import sistemainventario.service.ProfileServices;
import sistemainventario.service.ReportExportService;
import sistemainventario.service.ReportService;

import java.nio.file.Path;

/**
 * Casos de uso de la sección Informes: consultar el informe de un período y
 * exportarlo a Excel.
 *
 * <p><b>GRASP Controller:</b> recibe la petición de la vista y la delega en
 * {@link ReportService} (cálculo) y {@link ReportExportService} (archivo).</p>
 */
public class ReportController {

    private final AppContext contexto;
    private final FileOpener abridor;

    public ReportController(AppContext contexto, FileOpener abridor) {
        this.contexto = contexto;
        this.abridor = abridor;
    }

    /**
     * @param desde         primer día (yyyy-MM-dd)
     * @param hasta         último día (yyyy-MM-dd)
     * @param anteriorDesde inicio del período de comparación (vacío: el anterior de igual duración)
     * @param anteriorHasta fin del período de comparación
     * @return el informe en JSON o {@code "ERROR: ..."}
     */
    public String obtenerInforme(String desde, String hasta, String anteriorDesde, String anteriorHasta) {
        return BridgeResponse.ejecutar(() -> BridgeResponse.json(informe(desde, hasta, anteriorDesde, anteriorHasta)));
    }

    /**
     * Guarda el informe como .xlsx en la carpeta Documentos/TBalance y lo abre.
     *
     * @return ruta del archivo o {@code "ERROR: ..."}
     */
    public String exportarInformeExcel(String desde, String hasta, String anteriorDesde, String anteriorHasta) {
        return BridgeResponse.ejecutar(() -> {
            String negocio = contexto.getGestorPerfiles().getPerfilActivo().map(Profile::getNombre).orElse("TBalance");
            Path archivo = contexto.getExportadorInformes()
                    .exportarExcel(informe(desde, hasta, anteriorDesde, anteriorHasta), negocio);
            abridor.abrir(archivo);
            return archivo.toAbsolutePath().toString();
        });
    }

    private SalesReport informe(String desde, String hasta, String anteriorDesde, String anteriorHasta) {
        ProfileServices servicios = contexto.requerirServiciosActivos();
        return new ReportService(servicios.ventas(), servicios.inventario())
                .generar(desde, hasta, anteriorDesde, anteriorHasta);
    }
}
