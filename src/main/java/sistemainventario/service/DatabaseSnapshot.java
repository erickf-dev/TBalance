package sistemainventario.service;

import sistemainventario.repository.JsonStorageProvider;
import sistemainventario.repository.StorageProvider;
import sistemainventario.util.FileUtil;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Copias de seguridad cuando los datos están en PostgreSQL u Oracle.
 *
 * <p>Para comprimir, vuelca las tablas a una carpeta temporal en el mismo
 * formato JSON de siempre (una copia hecha con base de datos se puede
 * restaurar con archivos y al revés) y luego la borra: no quedan archivos en el
 * computador. Al restaurar, vacía la base y carga lo que trae la copia.</p>
 */
public class DatabaseSnapshot implements DataSnapshot {

    private final StorageProvider base;
    private final DataTransferService copiador;

    /**
     * @param base     almacenamiento en la base de datos
     * @param copiador servicio que copia entre almacenamientos
     */
    public DatabaseSnapshot(StorageProvider base, DataTransferService copiador) {
        this.base = Objects.requireNonNull(base);
        this.copiador = Objects.requireNonNull(copiador);
    }

    @Override
    public Path prepararCarpeta() {
        Path temporal = FileUtil.carpetaTemporal("tbalance-copia-");
        copiador.copiar(base, new JsonStorageProvider(temporal));
        return temporal;
    }

    @Override
    public void liberarCarpeta(Path carpeta) {
        FileUtil.borrarCarpeta(carpeta);
    }

    @Override
    public void restaurarDesde(Path carpeta) {
        // Una copia de antes del inicio de sesión no trae personas: se conservan las
        // de ahora, para no quedar sin cuentas (y sin poder entrar).
        java.util.List<sistemainventario.model.User> actuales = base.usuarios().listarTodos();
        JsonStorageProvider copia = new JsonStorageProvider(carpeta);
        boolean copiaSinUsuarios = copia.usuarios().listarTodos().isEmpty();
        copiador.vaciar(base);
        copiador.copiar(copia, base);
        if (copiaSinUsuarios && !actuales.isEmpty()) {
            copiador.copiarUsuarios(actuales, base);
        }
    }
}
