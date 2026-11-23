package sistemainventario.repository;

import sistemainventario.model.Supplier;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Proveedores de un negocio en la tabla PROVEEDOR. */
public class SupplierJdbcRepository extends JdbcRepository<Supplier> {

    /**
     * @param bd       conexión a la base
     * @param perfilId negocio
     */
    public SupplierJdbcRepository(Database bd, String perfilId) {
        super(bd, perfilId);
    }

    @Override
    protected String tabla() {
        return "PROVEEDOR";
    }

    @Override
    protected List<String> columnas() {
        return List.of("NOMBRE", "CONTACTO", "TELEFONO", "EMAIL", "DIRECCION", "CATEGORIA", "ACTIVO");
    }

    @Override
    protected Object[] valores(Supplier p) {
        return new Object[]{p.getNombre(), texto(p.getContacto()), texto(p.getTelefono()), texto(p.getEmail()),
                texto(p.getDireccion()), texto(p.getCategoria()), p.isActivo()};
    }

    @Override
    protected Supplier leer(ResultSet fila) throws SQLException {
        Supplier p = new Supplier();
        p.setId(fila.getString("ID"));
        p.setNombre(fila.getString("NOMBRE"));
        p.setContacto(fila.getString("CONTACTO"));
        p.setTelefono(fila.getString("TELEFONO"));
        p.setEmail(fila.getString("EMAIL"));
        p.setDireccion(fila.getString("DIRECCION"));
        p.setCategoria(fila.getString("CATEGORIA"));
        p.setActivo(fila.getInt("ACTIVO") == 1);
        return p;
    }

    /** Lee de PROVEEDOR_CATEGORIA los tipos de producto de todos los proveedores del negocio. */
    @Override
    protected void completar(List<Supplier> proveedores) {
        Map<String, List<String>> porProveedor = new LinkedHashMap<>();
        bd.consultar("SELECT PROVEEDOR_ID, CATEGORIA_ID FROM PROVEEDOR_CATEGORIA WHERE PERFIL_ID = ? "
                        + "ORDER BY PROVEEDOR_ID, CATEGORIA_ID",
                fila -> porProveedor.computeIfAbsent(fila.getString("PROVEEDOR_ID"), k -> new ArrayList<>())
                        .add(fila.getString("CATEGORIA_ID")), perfilId);
        for (Supplier proveedor : proveedores) {
            proveedor.setCategoriaIds(porProveedor.getOrDefault(proveedor.getId(), List.of()));
        }
    }

    /** Reescribe los tipos de producto del proveedor (solo categorías que existen en el negocio). */
    @Override
    protected void guardarHijos(Connection conexion, Supplier proveedor, boolean nuevo) throws SQLException {
        if (!nuevo) {
            Database.actualizar(conexion, "DELETE FROM PROVEEDOR_CATEGORIA WHERE PERFIL_ID = ? AND PROVEEDOR_ID = ?",
                    perfilId, proveedor.getId());
        }
        for (String categoriaId : proveedor.getCategoriaIds()) {
            if (referenciaValida("CATEGORIA", categoriaId) instanceof String) {
                Database.actualizar(conexion,
                        "INSERT INTO PROVEEDOR_CATEGORIA (PERFIL_ID, PROVEEDOR_ID, CATEGORIA_ID) VALUES (?, ?, ?)",
                        perfilId, proveedor.getId(), categoriaId);
            }
        }
    }

    /** Los productos del proveedor quedan sin proveedor (la llave foránea no lo permitiría). */
    @Override
    protected void antesDeEliminar(Connection conexion, String id) throws SQLException {
        Database.actualizar(conexion, "UPDATE PRODUCTO SET PROVEEDOR_ID = NULL WHERE PERFIL_ID = ? AND PROVEEDOR_ID = ?",
                perfilId, id);
    }
}
