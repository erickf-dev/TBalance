package sistemainventario.repository;

import sistemainventario.model.Category;
import sistemainventario.model.Product;
import sistemainventario.model.Profile;
import sistemainventario.model.Sale;
import sistemainventario.model.StockEntry;
import sistemainventario.model.StockExit;
import sistemainventario.model.Supplier;
import sistemainventario.model.User;

/**
 * Fábrica de los repositorios de TBalance (patrón fábrica abstracta).
 *
 * <p>Hay dos familias: {@link JsonStorageProvider} (archivos JSON) y
 * {@link DatabaseStorageProvider} (PostgreSQL u Oracle con JDBC). Los servicios
 * reciben {@link Repository} y no saben cuál se está usando; cambiar de
 * almacenamiento no obliga a tocar la lógica de negocio.</p>
 */
public interface StorageProvider extends AutoCloseable {

    /** @return nombre del almacenamiento para mostrar ("PostgreSQL", "Oracle"...) */
    String nombre();

    /** @return repositorio de perfiles */
    Repository<Profile> perfiles();

    /** @return personas que usan TBalance, con sus accesos a los negocios */
    Repository<User> usuarios();

    /** @return historial de acciones (quién hizo qué) */
    Repository<sistemainventario.model.AuditEntry> auditoria();

    /**
     * @param perfilId negocio
     * @return repositorio de productos del negocio
     */
    Repository<Product> productos(String perfilId);

    /**
     * @param perfilId negocio
     * @return repositorio de categorías del negocio
     */
    Repository<Category> categorias(String perfilId);

    /**
     * @param perfilId negocio
     * @return repositorio de proveedores del negocio
     */
    Repository<Supplier> proveedores(String perfilId);

    /**
     * @param perfilId negocio
     * @return repositorio de entradas del negocio
     */
    Repository<StockEntry> entradas(String perfilId);

    /**
     * @param perfilId negocio
     * @return repositorio de salidas del negocio
     */
    Repository<StockExit> salidas(String perfilId);

    /**
     * @param perfilId negocio
     * @return repositorio de ventas del negocio
     */
    Repository<Sale> ventas(String perfilId);

    /**
     * @param perfilId negocio
     * @return abonos de los clientes que deben (fiados)
     */
    Repository<sistemainventario.model.CreditPayment> abonos(String perfilId);

    /** Libera la conexión, si la hay. */
    @Override
    default void close() {
    }
}
