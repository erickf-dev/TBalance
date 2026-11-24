package sistemainventario.repository;

import sistemainventario.model.AuditEntry;
import sistemainventario.model.Category;
import sistemainventario.model.CreditPayment;
import sistemainventario.model.Product;
import sistemainventario.model.Profile;
import sistemainventario.model.Sale;
import sistemainventario.model.StockEntry;
import sistemainventario.model.StockExit;
import sistemainventario.model.Supplier;
import sistemainventario.model.User;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Contenido de una copia de seguridad: los datos de Oracle en archivos JSON
 * dentro de una carpeta temporal, que después se comprime en un .zip y se
 * guarda en la tabla RESPALDO. TBalance no guarda los negocios en archivos.
 *
 * <p>Dentro de la copia van {@code perfiles.json}, {@code usuarios.json},
 * {@code auditoria.json} y una carpeta {@code perfiles/<ID>} por negocio con
 * sus productos, categorías, proveedores, entradas, salidas, ventas y abonos
 * (el mismo formato de siempre, así se pueden restaurar copias viejas).</p>
 */
public class JsonStorageProvider implements StorageProvider {

    /** Archivo con los negocios; si una copia no lo tiene, no es una copia de TBalance. */
    public static final String ARCHIVO_PERFILES = "perfiles.json";
    /** Archivo con las personas que usan TBalance. */
    public static final String ARCHIVO_USUARIOS = "usuarios.json";

    private final Path carpeta;

    /** @param carpeta carpeta temporal de la copia */
    public JsonStorageProvider(Path carpeta) {
        this.carpeta = Objects.requireNonNull(carpeta);
    }

    @Override
    public String nombre() {
        return "Copia de seguridad";
    }

    @Override
    public Repository<Profile> perfiles() {
        return new JsonRepository<>(carpeta, ARCHIVO_PERFILES, Profile.class);
    }

    @Override
    public Repository<User> usuarios() {
        return new JsonRepository<>(carpeta, ARCHIVO_USUARIOS, User.class);
    }

    @Override
    public Repository<AuditEntry> auditoria() {
        return new JsonRepository<>(carpeta, "auditoria.json", AuditEntry.class);
    }

    @Override
    public Repository<Product> productos(String perfilId) {
        return new JsonRepository<>(deNegocio(perfilId), "productos.json", Product.class);
    }

    @Override
    public Repository<Category> categorias(String perfilId) {
        return new JsonRepository<>(deNegocio(perfilId), "categorias.json", Category.class);
    }

    @Override
    public Repository<Supplier> proveedores(String perfilId) {
        return new JsonRepository<>(deNegocio(perfilId), "proveedores.json", Supplier.class);
    }

    @Override
    public Repository<StockEntry> entradas(String perfilId) {
        return new JsonRepository<>(deNegocio(perfilId), "entradas.json", StockEntry.class);
    }

    @Override
    public Repository<CreditPayment> abonos(String perfilId) {
        return new JsonRepository<>(deNegocio(perfilId), "abonos.json", CreditPayment.class);
    }

    @Override
    public Repository<StockExit> salidas(String perfilId) {
        return new JsonRepository<>(deNegocio(perfilId), "salidas.json", StockExit.class);
    }

    @Override
    public Repository<Sale> ventas(String perfilId) {
        return new JsonRepository<>(deNegocio(perfilId), "ventas.json", Sale.class);
    }

    /** @return carpeta {@code perfiles/<ID>} del negocio dentro de la copia (se crea si no existe) */
    private Path deNegocio(String perfilId) {
        Path dir = carpeta.resolve("perfiles").resolve(perfilId);
        JsonFileStore.crearDirectorio(dir);
        return dir;
    }
}
