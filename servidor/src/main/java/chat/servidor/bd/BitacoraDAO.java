package chat.servidor.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Bitácora de eventos del servidor. Nunca guardes aquí el contenido de mensajes privados. */
public class BitacoraDAO {

    /** Registra un evento con la hora local. Ejemplo: guardar("ana inició sesión"). */
    public void guardar(String evento) {
        try (Connection c = BaseDatos.obtenerConexion();
             PreparedStatement ps = c.prepareStatement("INSERT INTO bitacora (evento) VALUES (?)")) {
            ps.setString(1, evento);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    /** Todos los eventos, del más antiguo al más reciente, como "yyyy-MM-dd HH:mm:ss  evento". */
    public List<String> listar() {
        String sql = "SELECT fecha, evento FROM bitacora ORDER BY id_bitacora";
        List<String> lineas = new ArrayList<>();
        try (Connection c = BaseDatos.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lineas.add(rs.getString(1) + "  " + rs.getString(2));
            }
            return lineas;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }
}
