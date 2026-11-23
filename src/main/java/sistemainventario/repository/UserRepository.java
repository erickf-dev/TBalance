package sistemainventario.repository;

import sistemainventario.model.User;

import java.nio.file.Path;

/**
 * Personas que usan TBalance, en {@code usuarios.json} (junto a {@code perfiles.json}).
 * Cada persona lleva dentro la lista de negocios a los que entra y su rol.
 */
public class UserRepository extends JsonRepository<User> {

    public static final String ARCHIVO = "usuarios.json";

    /** @param directorio carpeta {@code data} */
    public UserRepository(Path directorio) {
        super(directorio, ARCHIVO, User.class);
    }
}
