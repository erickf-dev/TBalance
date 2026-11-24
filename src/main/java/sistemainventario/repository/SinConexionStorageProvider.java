package sistemainventario.repository;

import sistemainventario.exception.ValidationException;
import sistemainventario.model.AuditEntry;
import sistemainventario.model.Category;
import sistemainventario.model.CreditPayment;
import sistemainventario.model.IdentifiedEntity;
import sistemainventario.model.Product;
import sistemainventario.model.Profile;
import sistemainventario.model.Sale;
import sistemainventario.model.StockEntry;
import sistemainventario.model.StockExit;
import sistemainventario.model.Supplier;
import sistemainventario.model.User;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Lo que usa TBalance mientras no hay conexión con Oracle: todo está vacío y
 * no se puede guardar nada. La pantalla de inicio pide entonces los datos de
 * conexión.
 */
public class SinConexionStorageProvider implements StorageProvider {

    /** Mensaje cuando se intenta guardar sin conexión. */
    public static final String MENSAJE = "No hay conexión con Oracle. Conecte la base de datos para continuar.";

    @Override
    public String nombre() {
        return "Sin conexión";
    }

    @Override
    public Repository<Profile> perfiles() {
        return new Vacio<>();
    }

    @Override
    public Repository<User> usuarios() {
        return new Vacio<>();
    }

    @Override
    public Repository<AuditEntry> auditoria() {
        return new Vacio<>();
    }

    @Override
    public Repository<Product> productos(String perfilId) {
        return new Vacio<>();
    }

    @Override
    public Repository<Category> categorias(String perfilId) {
        return new Vacio<>();
    }

    @Override
    public Repository<Supplier> proveedores(String perfilId) {
        return new Vacio<>();
    }

    @Override
    public Repository<StockEntry> entradas(String perfilId) {
        return new Vacio<>();
    }

    @Override
    public Repository<StockExit> salidas(String perfilId) {
        return new Vacio<>();
    }

    @Override
    public Repository<Sale> ventas(String perfilId) {
        return new Vacio<>();
    }

    @Override
    public Repository<CreditPayment> abonos(String perfilId) {
        return new Vacio<>();
    }

    /** Repositorio vacío que no deja guardar. */
    private static final class Vacio<T extends IdentifiedEntity> implements Repository<T> {
        @Override
        public List<T> listarTodos() {
            return List.of();
        }

        @Override
        public Optional<T> buscarPorId(String id) {
            return Optional.empty();
        }

        @Override
        public boolean existe(String id) {
            return false;
        }

        @Override
        public List<T> filtrar(Predicate<T> condicion) {
            return List.of();
        }

        @Override
        public void agregar(T entidad) {
            throw new ValidationException(MENSAJE);
        }

        @Override
        public boolean actualizar(T entidad) {
            throw new ValidationException(MENSAJE);
        }

        @Override
        public boolean eliminar(String id) {
            throw new ValidationException(MENSAJE);
        }

        @Override
        public String siguienteIdNumerico() {
            return "1";
        }
    }
}
