package chat.servidor.bd;

import chat.comun.modelo.Solicitud;
import chat.comun.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AmistadDAO {

    /** true si existe cualquier relación (pendiente o aceptada) entre los dos, en cualquier sentido. */
    public boolean existeRelacion(int a, int b) {
        return hayFila("SELECT 1 FROM amistades WHERE "
                + "(id_solicitante = ? AND id_receptor = ?) OR (id_solicitante = ? AND id_receptor = ?)", a, b, b, a);
    }

    public boolean sonAmigos(int a, int b) {
        return hayFila("SELECT 1 FROM amistades WHERE estado = 'aceptada' AND "
                + "((id_solicitante = ? AND id_receptor = ?) OR (id_solicitante = ? AND id_receptor = ?))", a, b, b, a);
    }

    /**
     * Crea una solicitud pendiente.
     * @return id de la solicitud, o -1 si es a sí mismo o ya existe una relación entre ambos
     */
    public int enviarSolicitud(int idSolicitante, int idReceptor) {
        if (idSolicitante == idReceptor || existeRelacion(idSolicitante, idReceptor)) {
            return -1;
        }
        try (Connection c = BaseDatos.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO amistades (id_solicitante, id_receptor) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idSolicitante);
            ps.setInt(2, idReceptor);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    /** Solicitudes pendientes que ha recibido el usuario. */
    public List<Solicitud> pendientesDe(int idReceptor) {
        String sql = "SELECT a.id_amistad, a.id_solicitante, u.nombre_usuario "
                + "FROM amistades a JOIN usuarios u ON u.id_usuario = a.id_solicitante "
                + "WHERE a.id_receptor = ? AND a.estado = 'pendiente' ORDER BY a.id_amistad";
        List<Solicitud> lista = new ArrayList<>();
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idReceptor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Solicitud(rs.getInt(1), rs.getInt(2), rs.getString(3), idReceptor));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    /** Solo el receptor puede aceptar. @return true si se aceptó */
    public boolean aceptar(int idAmistad, int idReceptor) {
        return modificar("UPDATE amistades SET estado = 'aceptada' "
                + "WHERE id_amistad = ? AND id_receptor = ? AND estado = 'pendiente'", idAmistad, idReceptor);
    }

    /** Solo el receptor puede rechazar. Borra la fila. @return true si se rechazó */
    public boolean rechazar(int idAmistad, int idReceptor) {
        return modificar("DELETE FROM amistades "
                + "WHERE id_amistad = ? AND id_receptor = ? AND estado = 'pendiente'", idAmistad, idReceptor);
    }

    /** Elimina la amistad aceptada entre los dos; la cascada borra su historial. @return true si existía */
    public boolean eliminarAmistad(int a, int b) {
        return modificar("DELETE FROM amistades WHERE estado = 'aceptada' AND "
                + "((id_solicitante = ? AND id_receptor = ?) OR (id_solicitante = ? AND id_receptor = ?))",
                a, b, b, a);
    }

    /** Amigos aceptados del usuario. El estado conectado lo pone el servidor. */
    public List<Usuario> amigosDe(int idUsuario) {
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.email, u.bloqueado "
                + "FROM amistades a JOIN usuarios u "
                + "ON u.id_usuario = CASE WHEN a.id_solicitante = ? THEN a.id_receptor ELSE a.id_solicitante END "
                + "WHERE a.estado = 'aceptada' AND (a.id_solicitante = ? OR a.id_receptor = ?) "
                + "ORDER BY u.nombre_usuario";
        List<Usuario> lista = new ArrayList<>();
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idUsuario);
            ps.setInt(3, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(UsuarioDAO.aUsuario(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    /** id de la amistad aceptada entre los dos, o -1 si no son amigos. */
    public int idAmistad(int a, int b) {
        String sql = "SELECT id_amistad FROM amistades WHERE estado = 'aceptada' AND "
                + "((id_solicitante = ? AND id_receptor = ?) OR (id_solicitante = ? AND id_receptor = ?))";
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, a);
            ps.setInt(2, b);
            ps.setInt(3, b);
            ps.setInt(4, a);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    private boolean hayFila(String sql, int... parametros) {
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setInt(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    private boolean modificar(String sql, int... parametros) {
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setInt(i + 1, parametros[i]);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }
}
