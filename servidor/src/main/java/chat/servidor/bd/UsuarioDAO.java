package chat.servidor.bd;

import chat.comun.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UsuarioDAO {
    public static final int RESPUESTAS_REQUERIDAS = 3;

    public boolean existeNombre(String nombreUsuario) {
        return existe("SELECT 1 FROM usuarios WHERE nombre_usuario = ?", nombreUsuario);
    }

    public boolean existeEmail(String email) {
        return existe("SELECT 1 FROM usuarios WHERE email = ?", email);
    }

    /**
     * Registra al usuario y sus 3 respuestas de seguridad en una sola transacción.
     * @param hashPsw   contraseña ya hasheada
     * @param respuestasHash id_pregunta -> hash de la respuesta normalizada (exactamente 3)
     * @return id del usuario nuevo
     */
    public int registrar(String nombreUsuario, String email, String hashPsw, Map<Integer, String> respuestasHash) {
        if (respuestasHash == null || respuestasHash.size() != RESPUESTAS_REQUERIDAS) {
            throw new IllegalArgumentException("Se requieren exactamente " + RESPUESTAS_REQUERIDAS + " respuestas");
        }
        try (Connection c = BaseDatos.obtenerConexion()) {
            c.setAutoCommit(false);
            try {
                int idUsuario;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO usuarios (nombre_usuario, email, psw) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombreUsuario);
                    ps.setString(2, email);
                    ps.setString(3, hashPsw);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idUsuario = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO respuestas_seguridad (id_usuario, id_pregunta, respuesta_hash) VALUES (?, ?, ?)")) {
                    for (Map.Entry<Integer, String> r : respuestasHash.entrySet()) {
                        ps.setInt(1, idUsuario);
                        ps.setInt(2, r.getKey());
                        ps.setString(3, r.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return idUsuario;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    public Usuario buscarPorNombre(String nombreUsuario) {
        return buscarUno("WHERE nombre_usuario = ?", nombreUsuario);
    }

    public Usuario buscarPorId(int idUsuario) {
        return buscarUno("WHERE id_usuario = ?", idUsuario);
    }

    public String obtenerHash(String nombreUsuario) {
        String sql = "SELECT psw FROM usuarios WHERE nombre_usuario = ?";
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    /** Suma un intento fallido y devuelve el total acumulado. */
    public int sumarIntentoFallido(int idUsuario) {
        try (Connection c = BaseDatos.obtenerConexion()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE usuarios SET intentos_fallidos = intentos_fallidos + 1 WHERE id_usuario = ?")) {
                ps.setInt(1, idUsuario);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT intentos_fallidos FROM usuarios WHERE id_usuario = ?")) {
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    public void reiniciarIntentos(int idUsuario) {
        actualizar("UPDATE usuarios SET intentos_fallidos = 0 WHERE id_usuario = ?", idUsuario);
    }

    public void bloquear(int idUsuario) {
        actualizar("UPDATE usuarios SET bloqueado = 1 WHERE id_usuario = ?", idUsuario);
    }

    /** Cambia la contraseña, reinicia los intentos y desbloquea (flujo de recuperación). */
    public void restablecerAcceso(int idUsuario, String nuevoHash) {
        String sql = "UPDATE usuarios SET psw = ?, intentos_fallidos = 0, bloqueado = 0 WHERE id_usuario = ?";
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nuevoHash);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    /** Todos los usuarios registrados. El estado conectado lo pone el servidor. */
    public List<Usuario> listarTodos() {
        String sql = "SELECT id_usuario, nombre_usuario, email, bloqueado FROM usuarios ORDER BY nombre_usuario";
        List<Usuario> lista = new ArrayList<>();
        try (Connection c = BaseDatos.obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(aUsuario(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    /**
     * Elimina la cuenta. La cascada borra respuestas, amistades, mensajes, membresías
     * y los grupos de los que era dueño.
     * @return true si existía y se borró
     */
    public boolean eliminar(int idUsuario) {
        try (Connection c = BaseDatos.obtenerConexion();
             PreparedStatement ps = c.prepareStatement("DELETE FROM usuarios WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }


    static Usuario aUsuario(ResultSet rs) throws SQLException {
        return new Usuario(rs.getInt("id_usuario"), rs.getString("nombre_usuario"),
                rs.getString("email"), rs.getBoolean("bloqueado"));
    }

    private boolean existe(String sql, String valor) {
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    private Usuario buscarUno(String where, Object parametro) {
        String sql = "SELECT id_usuario, nombre_usuario, email, bloqueado FROM usuarios " + where;
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? aUsuario(rs) : null;
            }
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }

    private void actualizar(String sql, int idUsuario) {
        try (Connection c = BaseDatos.obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BdException(e);
        }
    }
}
