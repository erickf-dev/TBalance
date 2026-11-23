package sistemainventario.service;

import sistemainventario.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import sistemainventario.repository.JsonFileStore;
import sistemainventario.util.FileUtil;

import java.nio.file.Path;
import java.util.Objects;

/** Copias de seguridad en modo de archivos: se comprime y se reemplaza la carpeta {@code data}. */
public class FileSnapshot implements DataSnapshot {

    private final Path directorioDatos;

    /** @param directorioDatos carpeta {@code data} */
    public FileSnapshot(Path directorioDatos) {
        this.directorioDatos = Objects.requireNonNull(directorioDatos);
    }

    @Override
    public Path prepararCarpeta() {
        return directorioDatos;
    }

    @Override
    public void liberarCarpeta(Path carpeta) {
        // es la carpeta de datos: no se borra
    }

    @Override
    public void restaurarDesde(Path carpeta) {
        // Una copia de antes del inicio de sesión no trae usuarios.json: se conserva el
        // de ahora, para no quedar sin cuentas (y sin poder entrar).
        Path usuarios = directorioDatos.resolve(UserRepository.ARCHIVO);
        byte[] actuales = null;
        try {
            if (Files.exists(usuarios) && !Files.exists(carpeta.resolve(UserRepository.ARCHIVO))) {
                actuales = Files.readAllBytes(usuarios);
            }
        } catch (IOException e) {
            actuales = null;
        }
        // El historial de acciones nunca vuelve atrás: restaurar una copia vieja no
        // puede borrar lo que alguien hizo después.
        Path historial = directorioDatos.resolve(sistemainventario.repository.AuditRepository.ARCHIVO);
        byte[] historialActual = null;
        try {
            if (Files.exists(historial)) {
                historialActual = Files.readAllBytes(historial);
            }
        } catch (IOException e) {
            historialActual = null;
        }
        FileUtil.borrarCarpeta(directorioDatos);
        JsonFileStore.crearDirectorio(directorioDatos);
        FileUtil.copiarCarpeta(carpeta, directorioDatos);
        if (historialActual != null) {
            try {
                Files.write(historial, historialActual);
            } catch (IOException e) {
                throw new sistemainventario.exception.PersistenceException("No se pudo conservar el historial", e);
            }
        }
        if (actuales != null) {
            try {
                Files.write(usuarios, actuales);
            } catch (IOException e) {
                throw new sistemainventario.exception.PersistenceException("No se pudieron conservar los usuarios", e);
            }
        }
    }
}
