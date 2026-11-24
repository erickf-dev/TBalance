package sistemainventario.repository;

import sistemainventario.model.Category;
import sistemainventario.model.Product;
import sistemainventario.model.Profile;
import sistemainventario.model.Sale;
import sistemainventario.model.StockEntry;
import sistemainventario.model.StockExit;
import sistemainventario.model.Supplier;
import sistemainventario.model.User;

import sistemainventario.exception.PersistenceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Repositorios en archivos JSON. TBalance ya no guarda los negocios en
 * archivos: solo se usan como formato dentro del .zip de una copia de
 * seguridad (los datos de Oracle se escriben aquí y se comprimen, y al
 * restaurar se leen de aquí y se vuelven a guardar en Oracle).
 */
public class JsonStorageProvider implements StorageProvider {

    private final Path directorioDatos;
    private final ProfileDirectory directorios;

    /** @param directorioDatos carpeta {@code data} */
    public JsonStorageProvider(Path directorioDatos) {
        this.directorioDatos = Objects.requireNonNull(directorioDatos);
        this.directorios = new ProfileDirectory(directorioDatos);
    }

    /**
     * Borra {@code perfiles.json} y las carpetas de los negocios (no toca la
     * configuración global). Se usa antes de volcar ahí los datos de la base.
     */
    public void vaciarArchivos() {
        try {
            Files.deleteIfExists(directorioDatos.resolve(ProfileRepository.ARCHIVO));
            Files.deleteIfExists(directorioDatos.resolve(UserRepository.ARCHIVO));
        } catch (IOException e) {
            throw new PersistenceException("No se pudo borrar " + ProfileRepository.ARCHIVO, e);
        }
        directorios.eliminarTodas();
    }

    @Override
    public String nombre() {
        return "Archivos JSON";
    }

    @Override
    public Repository<Profile> perfiles() {
        return new ProfileRepository(directorioDatos);
    }

    @Override
    public Repository<User> usuarios() {
        return new UserRepository(directorioDatos);
    }

    @Override
    public Repository<sistemainventario.model.AuditEntry> auditoria() {
        return new AuditRepository(directorioDatos);
    }

    @Override
    public Repository<Product> productos(String perfilId) {
        return new ProductRepository(directorios.crear(perfilId));
    }

    @Override
    public Repository<Category> categorias(String perfilId) {
        return new CategoryRepository(directorios.crear(perfilId));
    }

    @Override
    public Repository<Supplier> proveedores(String perfilId) {
        return new SupplierRepository(directorios.crear(perfilId));
    }

    @Override
    public Repository<StockEntry> entradas(String perfilId) {
        return new StockEntryRepository(directorios.crear(perfilId));
    }

    @Override
    public Repository<sistemainventario.model.CreditPayment> abonos(String perfilId) {
        return new CreditPaymentRepository(directorios.crear(perfilId));
    }

    @Override
    public Repository<StockExit> salidas(String perfilId) {
        return new StockExitRepository(directorios.crear(perfilId));
    }

    @Override
    public Repository<Sale> ventas(String perfilId) {
        return new SaleRepository(directorios.crear(perfilId));
    }
}
