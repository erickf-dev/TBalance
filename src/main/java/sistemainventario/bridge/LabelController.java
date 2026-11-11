package sistemainventario.bridge;

import sistemainventario.AppContext;
import sistemainventario.model.Product;
import sistemainventario.service.LabelService;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>GRASP Controller</b> de los casos de uso de etiquetas con código QR.
 */
public class LabelController {

    private final AppContext contexto;
    private final FileOpener abridor;

    public LabelController(AppContext contexto, FileOpener abridor) {
        this.contexto = contexto;
        this.abridor = abridor;
    }

    /**
     * @param productoId ID del producto
     * @return JSON con la imagen de la etiqueta y el texto que contiene el QR
     */
    public String obtenerEtiquetaQR(String productoId) {
        return BridgeResponse.ejecutar(() -> {
            Product producto = producto(productoId);
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("imagen", etiquetas().vistaPrevia(producto));
            respuesta.put("contenido", producto.contenidoEtiquetaQR());
            return BridgeResponse.json(respuesta);
        });
    }

    /**
     * Guarda la etiqueta como PNG y la abre con el visor de imágenes.
     *
     * @param productoId ID del producto
     * @return JSON con la ruta del archivo creado
     */
    public String guardarEtiquetaQR(String productoId) {
        return BridgeResponse.ejecutar(() -> {
            Path archivo = etiquetas().guardarEtiqueta(producto(productoId));
            abridor.abrir(archivo);
            return BridgeResponse.json(Map.of("ruta", archivo.toAbsolutePath().toString()));
        });
    }

    /**
     * Genera las hojas de etiquetas de todos los productos del perfil activo
     * y abre la carpeta donde quedaron.
     *
     * @return JSON con la cantidad de hojas y la carpeta
     */
    public String generarHojaEtiquetas() {
        return BridgeResponse.ejecutar(() -> {
            List<Product> productos = contexto.requerirServiciosActivos().inventario().listarTodos();
            List<Path> hojas = etiquetas().guardarHojas(productos);
            abridor.abrir(etiquetas().getCarpeta());
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("hojas", hojas.size());
            respuesta.put("etiquetas", productos.size());
            respuesta.put("carpeta", etiquetas().getCarpeta().toAbsolutePath().toString());
            return BridgeResponse.json(respuesta);
        });
    }

    private Product producto(String productoId) {
        return contexto.requerirServiciosActivos().inventario().obtener(productoId);
    }

    private LabelService etiquetas() {
        return contexto.getGestorEtiquetas();
    }
}
