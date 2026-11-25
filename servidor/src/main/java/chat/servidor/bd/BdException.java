package chat.servidor.bd;

import java.sql.SQLException;

/** Envuelve SQLException para que el resto del servidor no tenga que atraparla en cada llamada. */
public class BdException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BdException(SQLException causa) {
        super("Error de base de datos: " + causa.getMessage(), causa);
    }
}
