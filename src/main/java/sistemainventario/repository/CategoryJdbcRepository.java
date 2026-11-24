package sistemainventario.repository;

import sistemainventario.model.Category;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Categorías de un negocio en la tabla CATEGORIA. */
public class CategoryJdbcRepository extends JdbcRepository<Category> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public CategoryJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "CATEGORIA";
    }

    @Override
    protected String funcionListar() {
        return "FN_LISTAR_CATEGORIAS";
    }

    @Override
    protected List<String> columnas() {
        return List.of("NOMBRE", "DESCRIPCION", "ACTIVO");
    }

    @Override
    protected Object[] valores(Category c) {
        return new Object[]{c.getNombre(), texto(c.getDescripcion()), c.isActivo()};
    }

    @Override
    protected Category leer(ResultSet fila) throws SQLException {
        Category c = new Category(fila.getString("ID"), fila.getString("NOMBRE"), fila.getString("DESCRIPCION"));
        c.setActivo(fila.getInt("ACTIVO") == 1);
        return c;
    }
}
