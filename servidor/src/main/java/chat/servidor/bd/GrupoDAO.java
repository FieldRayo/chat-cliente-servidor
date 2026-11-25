package chat.servidor.bd;

import chat.comun.modelo.Grupo;
import chat.comun.modelo.Mensaje;
import chat.comun.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class GrupoDAO {
    public static final int MINIMO_MIEMBROS = 3;

    private static final String SELECT_GRUPO =
            "SELECT g.id_grupo, g.nombre, g.id_dueno, u.nombre_usuario "
                    + "FROM grupos g JOIN usuarios u ON u.id_usuario = g.id_dueno ";

    /**
     * Crea el grupo: el dueño entra como aceptado y los demás como invitados.
     * @return id del grupo nuevo
     * @throws IllegalArgumentException si el nombre está vacío o con el dueño no suman 3 personas
     */
    public int crear(String nombre, int idDueno, List<Integer> invitados) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El grupo necesita un nombre");
        }
        Set<Integer> otros = new LinkedHashSet<>(invitados);
        otros.remove(idDueno);
        if (otros.size() + 1 < MINIMO_MIEMBROS) {
            throw new IllegalArgumentException("Un grupo necesita al menos " + MINIMO_MIEMBROS + " personas");
        }
        try (Connection c = BaseDatos.obtenerConexion()) {
            c.setAutoCommit(false);
            try {
                int idGrupo;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO grupos (nombre, id_dueno) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre.trim());
                    ps.setInt(2, idDueno);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idGrupo = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO miembros_grupo (id_grupo, id_usuario, estado) VALUES (?, ?, ?)")) {
                    ps.setInt(1, idGrupo);
                    ps.setInt(2, idDueno);
                    ps.setString(3, "aceptado");
                    ps.addBatch();
                    for (int idInvitado : otros) {
                        ps.setInt(1, idGrupo);
                        ps.setInt(2, idInvitado);
                        ps.setString(3, "invitado");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return idGrupo;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    public Grupo buscar(int idGrupo) {
        List<Grupo> r = consultarGrupos(SELECT_GRUPO + "WHERE g.id_grupo = ?", idGrupo);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Grupos en los que el usuario ya es miembro aceptado. */
    public List<Grupo> gruposDe(int idUsuario) {
        return consultarGrupos(SELECT_GRUPO
                + "JOIN miembros_grupo m ON m.id_grupo = g.id_grupo "
                + "WHERE m.id_usuario = ? AND m.estado = 'aceptado' ORDER BY g.nombre", idUsuario);
    }

    /** Invitaciones a grupos que el usuario aún no responde. */
    public List<Grupo> invitacionesDe(int idUsuario) {
        return consultarGrupos(SELECT_GRUPO
                + "JOIN miembros_grupo m ON m.id_grupo = g.id_grupo "
                + "WHERE m.id_usuario = ? AND m.estado = 'invitado' ORDER BY g.nombre", idUsuario);
    }

    /** Miembros aceptados del grupo. */
    public List<Usuario> miembros(int idGrupo) {
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.email, u.bloqueado "
                + "FROM miembros_grupo m JOIN usuarios u ON u.id_usuario = m.id_usuario "
                + "WHERE m.id_grupo = ? AND m.estado = 'aceptado' ORDER BY u.nombre_usuario";
        List<Usuario> lista = new ArrayList<>();
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGrupo);
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

    /** Invitados + aceptados: lo que cuenta para la regla de mínimo 3. */
    public int contarMiembros(int idGrupo) {
        String sql = "SELECT COUNT(*) FROM miembros_grupo WHERE id_grupo = ?";
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGrupo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    public boolean esDueno(int idGrupo, int idUsuario) {
        String sql = "SELECT 1 FROM grupos WHERE id_grupo = ? AND id_dueno = ?";
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGrupo);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    public boolean agregarInvitado(int idGrupo, int idUsuario) {
        String sql = "INSERT IGNORE INTO miembros_grupo (id_grupo, id_usuario, estado) VALUES (?, ?, 'invitado')";
        return modificar(sql, idGrupo, idUsuario);
    }

    public boolean aceptarInvitacion(int idGrupo, int idUsuario) {
        return modificar("UPDATE miembros_grupo SET estado = 'aceptado' "
                + "WHERE id_grupo = ? AND id_usuario = ? AND estado = 'invitado'", idGrupo, idUsuario);
    }

    public boolean quitarMiembro(int idGrupo, int idUsuario) {
        return modificar("DELETE FROM miembros_grupo WHERE id_grupo = ? AND id_usuario = ?", idGrupo, idUsuario);
    }

    public boolean renombrar(int idGrupo, String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            return false;
        }
        try (Connection c = BaseDatos.obtenerConexion();
             PreparedStatement ps = c.prepareStatement("UPDATE grupos SET nombre = ? WHERE id_grupo = ?")) {
            ps.setString(1, nuevoNombre.trim());
            ps.setInt(2, idGrupo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    /** Borra el grupo; la cascada elimina miembros y mensajes. */
    public boolean eliminar(int idGrupo) {
        return modificar("DELETE FROM grupos WHERE id_grupo = ?", idGrupo);
    }

   
    public List<Integer> eliminarGruposIncompletos() {
        String sql = "SELECT g.id_grupo FROM grupos g "
                + "LEFT JOIN miembros_grupo m ON m.id_grupo = g.id_grupo "
                + "GROUP BY g.id_grupo HAVING COUNT(m.id_usuario) < ?";
        List<Integer> borrados = new ArrayList<>();
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, MINIMO_MIEMBROS);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    borrados.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
        for (int id : borrados) {
            eliminar(id);
        }
        return borrados;
    }


    public boolean guardarMensaje(int idGrupo, int idEmisor, String contenido) {
        String sql = "INSERT INTO mensajes_grupo (id_grupo, id_emisor, contenido) "
                + "SELECT ?, ?, ? FROM miembros_grupo "
                + "WHERE id_grupo = ? AND id_usuario = ? AND estado = 'aceptado'";
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGrupo);
            ps.setInt(2, idEmisor);
            ps.setString(3, contenido);
            ps.setInt(4, idGrupo);
            ps.setInt(5, idEmisor);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }


    public List<Mensaje> historial(int idGrupo) {
        String sql = "SELECT m.id_mensaje, m.id_emisor, u.nombre_usuario, m.contenido, m.fecha "
                + "FROM mensajes_grupo m JOIN usuarios u ON u.id_usuario = m.id_emisor "
                + "WHERE m.id_grupo = ? ORDER BY m.fecha, m.id_mensaje";
        List<Mensaje> lista = new ArrayList<>();
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idGrupo);
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

    private List<Grupo> consultarGrupos(String sql, int parametro) {
        List<Grupo> lista = new ArrayList<>();
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Grupo(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4)));
                }
            }
            return lista;
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
