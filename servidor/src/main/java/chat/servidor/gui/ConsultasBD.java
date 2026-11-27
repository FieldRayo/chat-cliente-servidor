package chat.servidor.gui;

import chat.comun.modelo.Grupo;
import chat.comun.modelo.Mensaje;
import chat.comun.modelo.Solicitud;
import chat.comun.modelo.Usuario;
import chat.servidor.bd.AmistadDAO;
import chat.servidor.bd.GrupoDAO;
import chat.servidor.bd.MensajeAmigoDAO;
import chat.servidor.bd.UsuarioDAO;
import chat.servidor.nucleo.Sesiones;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Arma las filas de las pestañas Usuarios, Amistades, Grupos y Mensajes.
 * Solo usa los DAOs de 02 · Base de datos (aquí no hay SQL).
 * Cada método regresa una lista de filas lista para meter en una tabla.
 */
final class ConsultasBD {

    private static final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private static final AmistadDAO amistadDAO = new AmistadDAO();
    private static final GrupoDAO grupoDAO = new GrupoDAO();
    private static final MensajeAmigoDAO mensajeAmigoDAO = new MensajeAmigoDAO();

    static final String[] COLUMNAS_USUARIOS = {"ID", "Usuario", "Correo", "Estado"};
    static final String[] COLUMNAS_AMISTADES = {"Usuario", "Usuario", "Estado"};
    static final String[] COLUMNAS_GRUPOS = {"ID", "Grupo", "Dueño", "Integrantes", "Miembros"};
    static final String[] COLUMNAS_MENSAJES = {"Fecha", "Chat", "De", "Mensaje"};

    private ConsultasBD() { }

    /** Todos los usuarios con su estado: Bloqueado, Conectado o Desconectado. */
    static List<Object[]> usuarios() {
        List<Object[]> filas = new ArrayList<>();
        for (Usuario u : usuarioDAO.listarTodos()) {
            String estado;
            if (u.isBloqueado()) {
                estado = "Bloqueado";
            } else if (Sesiones.estaConectado(u.getNombreUsuario())) {
                estado = "Conectado";
            } else {
                estado = "Desconectado";
            }
            filas.add(new Object[]{u.getId(), u.getNombreUsuario(), u.getEmail(), estado});
        }
        return filas;
    }

    /** Todas las amistades aceptadas y las solicitudes pendientes, sin repetir parejas. */
    static List<Object[]> amistades() {
        List<Usuario> usuarios = usuarioDAO.listarTodos();
        Map<Integer, String> nombres = nombresPorId(usuarios);
        List<Object[]> filas = new ArrayList<>();
        Set<String> vistas = new HashSet<>();

        for (Usuario u : usuarios) {
            for (Usuario amigo : amistadDAO.amigosDe(u.getId())) {
                if (vistas.add(clavePareja(u.getId(), amigo.getId()))) {
                    filas.add(new Object[]{u.getNombreUsuario(), amigo.getNombreUsuario(), "Amigos"});
                }
            }
            for (Solicitud s : amistadDAO.pendientesDe(u.getId())) {
                if (vistas.add(clavePareja(s.getIdSolicitante(), s.getIdReceptor()))) {
                    String receptor = nombres.getOrDefault(s.getIdReceptor(), "#" + s.getIdReceptor());
                    filas.add(new Object[]{s.getNombreSolicitante(), receptor, "Solicitud pendiente"});
                }
            }
        }
        return filas;
    }

    /** Todos los grupos con su dueño y sus miembros aceptados. */
    static List<Object[]> grupos() {
        List<Object[]> filas = new ArrayList<>();
        for (Grupo g : todosLosGrupos().values()) {
            List<Usuario> miembros = grupoDAO.miembros(g.getId());
            String nombresMiembros = miembros.stream()
                    .map(Usuario::getNombreUsuario)
                    .collect(Collectors.joining(", "));
            filas.add(new Object[]{g.getId(), g.getNombre(), g.getNombreDueno(), miembros.size(), nombresMiembros});
        }
        return filas;
    }

    /**
     * Todos los mensajes guardados: los de los chats de amigos y los de grupos, del más nuevo al más viejo.
     * Los del chat temporal (ventana 1) no se guardan en la BD; esos se ven en la Bitácora.
     */
    static List<Object[]> mensajes() {
        List<FilaMensaje> lista = new ArrayList<>();

        List<Usuario> usuarios = usuarioDAO.listarTodos();
        Set<String> parejas = new HashSet<>();
        for (Usuario u : usuarios) {
            for (Usuario amigo : amistadDAO.amigosDe(u.getId())) {
                if (parejas.add(clavePareja(u.getId(), amigo.getId()))) {
                    String chat = "Amigos: " + u.getNombreUsuario() + " ↔ " + amigo.getNombreUsuario();
                    for (Mensaje m : mensajeAmigoDAO.historial(u.getId(), amigo.getId())) {
                        lista.add(new FilaMensaje(m, chat));
                    }
                }
            }
        }

        for (Grupo g : todosLosGrupos().values()) {
            String chat = "Grupo: " + g.getNombre();
            for (Mensaje m : grupoDAO.historial(g.getId())) {
                lista.add(new FilaMensaje(m, chat));
            }
        }

        // Del más nuevo al más viejo; si dos tienen el mismo segundo, el de id más alto va primero.
        lista.sort(Comparator.comparing((FilaMensaje f) -> f.mensaje.getFecha(),
                        Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()))
                .thenComparingLong(f -> f.mensaje.getId())
                .reversed());

        List<Object[]> filas = new ArrayList<>();
        for (FilaMensaje f : lista) {
            String fecha = f.mensaje.getFecha() == null ? "" : f.mensaje.getFecha().format(Bitacora.FORMATO_FECHA);
            filas.add(new Object[]{fecha, f.chat, f.mensaje.getNombreEmisor(), f.mensaje.getContenido()});
        }
        return filas;
    }

    // ------------------------------------------------------------------ ayudantes

    /** No hay un "listar todos los grupos", así que se juntan los grupos de cada usuario sin repetir. */
    private static Map<Integer, Grupo> todosLosGrupos() {
        Map<Integer, Grupo> grupos = new LinkedHashMap<>();
        for (Usuario u : usuarioDAO.listarTodos()) {
            for (Grupo g : grupoDAO.gruposDe(u.getId())) {
                grupos.putIfAbsent(g.getId(), g);
            }
        }
        return grupos;
    }

    private static Map<Integer, String> nombresPorId(List<Usuario> usuarios) {
        Map<Integer, String> nombres = new HashMap<>();
        for (Usuario u : usuarios) {
            nombres.put(u.getId(), u.getNombreUsuario());
        }
        return nombres;
    }

    /** La misma clave para (a, b) y (b, a), para no contar dos veces una pareja. */
    private static String clavePareja(int a, int b) {
        return Math.min(a, b) + "-" + Math.max(a, b);
    }

    private static final class FilaMensaje {
        final Mensaje mensaje;
        final String chat;

        FilaMensaje(Mensaje mensaje, String chat) {
            this.mensaje = mensaje;
            this.chat = chat;
        }
    }
}
