package chat.servidor.bd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public final class BaseDatos {
    private static final String HOST = "localhost";
    private static final int PUERTO = 3306;
    private static final String NOMBRE_BD = "cocochatpro8c";
    private static final String USUARIO = "root";
    private static final String CLAVE = "";

    private static final String URL_SERVIDOR = "jdbc:mariadb://" + HOST + ":" + PUERTO + "/";
    private static final String URL_BD = URL_SERVIDOR + NOMBRE_BD;

    private BaseDatos() { }

    public static void inicializar() {
        try (Connection c = DriverManager.getConnection(URL_SERVIDOR, USUARIO, CLAVE);
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + NOMBRE_BD
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
        } catch (SQLException e) {
            throw new BdException(e);
        }
        try (Connection c = obtenerConexion(); Statement st = c.createStatement()) {
            for (String sentencia : leerSchema()) {
                st.executeUpdate(sentencia);
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(URL_BD, USUARIO, CLAVE);
    }

    private static String[] leerSchema() {
        try (InputStream in = BaseDatos.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró schema.sql en resources");
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return java.util.Arrays.stream(sql.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer schema.sql", e);
        }
    }
}
