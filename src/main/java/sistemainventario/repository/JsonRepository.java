package sistemainventario.repository;

import com.google.gson.JsonParseException;
import sistemainventario.exception.PersistenceException;
import sistemainventario.model.IdentifiedEntity;
import sistemainventario.util.IdGenerator;
import sistemainventario.util.JsonUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementación genérica de {@link Repository} que guarda una lista de
 * entidades en un archivo JSON y la mantiene en memoria (caché).
 *
 * <p><b>OCP:</b> está cerrada a modificación y abierta a extensión. Cada
 * entidad nueva solo necesita una subclase que indique el nombre del
 * archivo y la clase (ver {@link ProductRepository}).</p>
 *
 * <p><b>SRP:</b> se ocupa del CRUD sobre la caché; la E/S de disco la delega
 * en {@link JsonFileStore} y la lógica de negocio vive en los servicios.</p>
 *
 * @param <T> tipo de entidad, siempre subclase de {@link IdentifiedEntity}
 */
public abstract class JsonRepository<T extends IdentifiedEntity> implements Repository<T> {

    private final JsonFileStore almacen;
    private final Class<T> tipo;
    private final List<T> cache;

    /**
     * @param directorio    carpeta del perfil o carpeta global
     * @param nombreArchivo nombre del archivo JSON (por ejemplo {@code productos.json})
     * @param tipo          clase de la entidad para deserializar
     */
    protected JsonRepository(Path directorio, String nombreArchivo, Class<T> tipo) {
        this.almacen = new JsonFileStore(directorio.resolve(nombreArchivo));
        this.tipo = tipo;
        this.cache = cargar();
    }

    @Override
    public List<T> listarTodos() {
        return new ArrayList<>(cache);
    }

    @Override
    public Optional<T> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return cache.stream().filter(entidad -> id.equals(entidad.getId())).findFirst();
    }

    @Override
    public boolean existe(String id) {
        return buscarPorId(id).isPresent();
    }

    @Override
    public List<T> filtrar(Predicate<T> condicion) {
        return cache.stream().filter(condicion).collect(Collectors.toList());
    }

    @Override
    public void agregar(T entidad) {
        Objects.requireNonNull(entidad, "La entidad no puede ser nula");
        cache.add(entidad);
        persistir();
    }

    @Override
    public boolean actualizar(T entidad) {
        int posicion = indiceDe(entidad.getId());
        if (posicion < 0) {
            return false;
        }
        cache.set(posicion, entidad);
        persistir();
        return true;
    }

    @Override
    public boolean eliminar(String id) {
        boolean eliminado = cache.removeIf(entidad -> Objects.equals(id, entidad.getId()));
        if (eliminado) {
            persistir();
        }
        return eliminado;
    }

    @Override
    public String siguienteIdNumerico() {
        return IdGenerator.siguienteNumerico(
                cache.stream().map(IdentifiedEntity::getId).collect(Collectors.toList()));
    }

    /** @return ruta del archivo JSON administrado */
    public Path getArchivo() {
        return almacen.getArchivo();
    }

    private int indiceDe(String id) {
        for (int i = 0; i < cache.size(); i++) {
            if (Objects.equals(id, cache.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private List<T> cargar() {
        try {
            return almacen.leer()
                    .<List<T>>map(json -> new ArrayList<>(JsonUtil.fromJsonList(json, tipo)))
                    .orElseGet(ArrayList::new);
        } catch (JsonParseException e) {
            throw new PersistenceException("El archivo " + almacen.getArchivo().getFileName()
                    + " tiene un formato JSON invalido", e);
        }
    }

    private void persistir() {
        almacen.escribir(JsonUtil.toJson(cache));
    }
}
