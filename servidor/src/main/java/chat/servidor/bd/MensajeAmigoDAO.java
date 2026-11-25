package chat.servidor.bd;

import chat.comun.modelo.Mensaje;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Historial de la vista de amigos. Lo de "Todos los usuarios" no pasa por aquí: no se guarda. */
public class MensajeAmigoDAO {
    private final AmistadDAO amistades = new AmistadDAO();

    public boolean guardar(int idEmisor, int idReceptor, String contenido) {
        int idAmistad = amistades.idAmistad(idEmisor, idReceptor);
        if (idAmistad < 0) {
            return false;
        }
        try (Connection c = BaseDatos.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO mensajes_amigos (id_amistad, id_emisor, contenido) VALUES (?, ?, ?)")) {
            ps.setInt(1, idAmistad);
            ps.setInt(2, idEmisor);
            ps.setString(3, contenido);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    public List<Mensaje> historial(int a, int b) {
        String sql = "SELECT m.id_mensaje, m.id_emisor, u.nombre_usuario, m.contenido, m.fecha "
                + "FROM mensajes_amigos m "
                + "JOIN amistades am ON am.id_amistad = m.id_amistad "
                + "JOIN usuarios u ON u.id_usuario = m.id_emisor "
                + "WHERE am.estado = 'aceptada' AND "
                + "((am.id_solicitante = ? AND am.id_receptor = ?) OR (am.id_solicitante = ? AND am.id_receptor = ?)) "
                + "ORDER BY m.fecha, m.id_mensaje";
        List<Mensaje> lista = new ArrayList<>();
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, a);
            ps.setInt(2, b);
            ps.setInt(3, b);
            ps.setInt(4, a);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Mensaje(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                            rs.getTimestamp(5).toLocalDateTime()));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }
}
