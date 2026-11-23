package sistemainventario.repository;

import sistemainventario.model.Category;
import sistemainventario.model.Product;
import sistemainventario.model.Profile;
import sistemainventario.model.Sale;
import sistemainventario.model.StockEntry;
import sistemainventario.model.StockExit;
import sistemainventario.model.Supplier;
import sistemainventario.model.User;

import java.util.Objects;

/** Repositorios JDBC sobre PostgreSQL u Oracle; todos comparten una conexión. */
public class DatabaseStorageProvider implements StorageProvider {

    private final Database bd;

    /** @param bd conexión a la base (con las tablas ya creadas) */
    public DatabaseStorageProvider(Database bd) {
        this.bd = Objects.requireNonNull(bd);
    }

    /** @return la conexión, para crear tablas o copiar datos */
    public Database getBaseDeDatos() {
        return bd;
    }

    @Override
    public String nombre() {
        return bd.nombreMotor();
    }

    @Override
    public Repository<Profile> perfiles() {
        return new ProfileJdbcRepository(bd);
    }

    @Override
    public Repository<User> usuarios() {
        return new UserJdbcRepository(bd);
    }

    @Override
    public Repository<sistemainventario.model.AuditEntry> auditoria() {
        return new AuditJdbcRepository(bd);
    }

    @Override
    public Repository<Product> productos(String perfilId) {
        return new ProductJdbcRepository(bd, perfilId);
    }

    @Override
    public Repository<Category> categorias(String perfilId) {
        return new CategoryJdbcRepository(bd, perfilId);
    }

    @Override
    public Repository<Supplier> proveedores(String perfilId) {
        return new SupplierJdbcRepository(bd, perfilId);
    }

    @Override
    public Repository<StockEntry> entradas(String perfilId) {
        return new StockEntryJdbcRepository(bd, perfilId);
    }

    @Override
    public Repository<sistemainventario.model.CreditPayment> abonos(String perfilId) {
        return new CreditPaymentJdbcRepository(bd, perfilId);
    }

    @Override
    public Repository<StockExit> salidas(String perfilId) {
        return new StockExitJdbcRepository(bd, perfilId);
    }

    @Override
    public Repository<Sale> ventas(String perfilId) {
        return new SaleJdbcRepository(bd, perfilId);
    }

    @Override
    public void close() {
        bd.close();
    }
}
