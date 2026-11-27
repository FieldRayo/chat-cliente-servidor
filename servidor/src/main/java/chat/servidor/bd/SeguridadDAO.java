package chat.servidor.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Preguntas y respuestas de seguridad para recuperar el acceso. */
public class SeguridadDAO {

    public Map<Integer, String> listarPreguntas() {
        return consultarPreguntas("SELECT id_pregunta, pregunta FROM preguntas_seguridad ORDER BY id_pregunta", null);
    }

    public Map<Integer, String> preguntasDeUsuario(int idUsuario) {
        return consultarPreguntas(
                "SELECT p.id_pregunta, p.pregunta FROM respuestas_seguridad r "
                        + "JOIN preguntas_seguridad p ON p.id_pregunta = r.id_pregunta "
                        + "WHERE r.id_usuario = ? ORDER BY p.id_pregunta", idUsuario);
    }

    public Map<Integer, String> respuestasHashDe(int idUsuario) {
        String sql = "SELECT id_pregunta, respuesta_hash FROM respuestas_seguridad WHERE id_usuario = ?";
        Map<Integer, String> mapa = new LinkedHashMap<>();
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getInt(1), rs.getString(2));
                }
            }
            return mapa;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    public void agregarPregunta(String pregunta) {
        try (Connection c = BaseDatos.obtenerConexion();
             PreparedStatement ps = c.prepareStatement("INSERT OR IGNORE INTO preguntas_seguridad (pregunta) VALUES (?)")) {
            ps.setString(1, pregunta);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    private Map<Integer, String> consultarPreguntas(String sql, Integer parametro) {
        Map<Integer, String> mapa = new LinkedHashMap<>();
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getInt(1), rs.getString(2));
                }
            }
            return mapa;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }
}
