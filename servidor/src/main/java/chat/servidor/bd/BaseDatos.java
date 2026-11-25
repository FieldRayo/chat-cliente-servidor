package chat.servidor.bd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


/** Abre conexiones a la base SQLite {@code chat.db} y crea las tablas desde schema.sql. */
public final class BaseDatos {
    /** El archivo se crea solo, en la carpeta desde donde se ejecuta el servidor. */
    private static final String URL = "jdbc:sqlite:chat.db";

    private BaseDatos() { }

    public static void inicializar() {
        try (Connection c = obtenerConexion(); Statement st = c.createStatement()) {
            for (String sentencia : leerSchema()) {
                st.executeUpdate(sentencia);
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    /** SQLite no aplica las FK (ni la cascada) si no se activan en cada conexión. */
    public static Connection obtenerConexion() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    private static String[] leerSchema() {
        try (InputStream in = BaseDatos.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró schema.sql en resources");
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^\\s*--.*$", ""); // quitar comentarios
            return java.util.Arrays.stream(sql.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer schema.sql", e);
        }
    }
}
