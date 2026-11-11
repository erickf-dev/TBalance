package sistemainventario.repository;

import com.google.gson.JsonParseException;
import sistemainventario.exception.PersistenceException;
import sistemainventario.model.GlobalConfig;
import sistemainventario.util.JsonUtil;

import java.nio.file.Path;

/**
 * Persiste la {@link GlobalConfig} (perfil activo y lista de perfiles)
 * en {@code configuracion-global.json}.
 *
 * <p>Es un repositorio de objeto único, por eso no hereda de
 * {@link JsonRepository}; pero reutiliza {@link JsonFileStore} para la E/S
 * (SRP y eliminación de código duplicado respecto a P2).</p>
 */
public class GlobalConfigRepository {

    public static final String ARCHIVO = "configuracion-global.json";

    private final JsonFileStore almacen;

    /**
     * @param directorioBase carpeta raíz de datos ({@code ~/.inventariopro/data})
     */
    public GlobalConfigRepository(Path directorioBase) {
        this.almacen = new JsonFileStore(directorioBase.resolve(ARCHIVO));
    }

    /** @return {@code true} si el archivo de configuración ya fue creado */
    public boolean existe() {
        return almacen.existe();
    }

    /**
     * Carga la configuración guardada. Nunca devuelve {@code null}: si el
     * archivo no existe se entrega una configuración nueva y vacía.
     *
     * @return configuración actual
     * @throws PersistenceException si el archivo existe pero está corrupto
     */
    public GlobalConfig cargar() {
        try {
            return almacen.leer()
                    .map(json -> JsonUtil.fromJson(json, GlobalConfig.class))
                    .orElseGet(GlobalConfig::new);
        } catch (JsonParseException e) {
            throw new PersistenceException("El archivo " + ARCHIVO + " tiene un formato JSON invalido", e);
        }
    }

    /**
     * @param configuracion configuración a guardar
     */
    public void guardar(GlobalConfig configuracion) {
        almacen.escribir(JsonUtil.toJson(configuracion));
    }
}
