package chat.servidor.gui;

import chat.comun.protocolo.Tipo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/**
 * La "libreta" del servidor: anota todo lo que pasa y avisa a la ventana para que lo muestre.
 *
 * <p>Se usa con una sola línea desde cualquier parte del servidor:
 * <pre>Bitacora.registrar("ana inició sesión.");</pre>
 *
 * <p>Es seguro llamarla desde varios hilos a la vez (cada cliente tiene su hilo).
 * Los avisos a la ventana siempre llegan en el hilo de Swing.
 */
public final class Bitacora {

    /** Formato con el que se guarda y se muestra la fecha. */
    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Categorías para el filtro de la ventana. "Todos" no es categoría, es la opción de no filtrar. */
    public static final String ACCESO = "Acceso";
    public static final String USUARIOS = "Usuarios";
    public static final String AMIGOS = "Amigos";
    public static final String GRUPOS = "Grupos";
    public static final String CONEXIONES = "Conexiones";
    public static final String ERRORES = "Errores";
    public static final String OTROS = "Otros";
    public static final String[] CATEGORIAS = {ACCESO, USUARIOS, AMIGOS, GRUPOS, CONEXIONES, ERRORES, OTROS};

    /** Todo lo que se ha anotado desde que arrancó el servidor. */
    private static final List<Evento> historial = new ArrayList<>();

    /** Quién quiere enterarse de cada evento nuevo (la ventana). */
    private static final List<Consumer<Evento>> oyentes = new CopyOnWriteArrayList<>();

    private Bitacora() { }

    /**
     * Anota un evento con la fecha y hora actual y lo manda a la ventana.
     * TODO (cuando 02 · Base de datos suba BitacoraDAO): guardarlo también en la BD.
     */
    public static void registrar(String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        Evento evento = new Evento(LocalDateTime.now().format(FORMATO_FECHA), texto.trim());
        agregar(evento);
    }

    /**
     * Mete eventos viejos al historial sin cambiarles la fecha.
     * Sirve para cargar lo que ya estaba guardado en la BD al arrancar el servidor.
     */
    public static void cargarHistorial(List<String[]> filas) {
        for (String[] fila : filas) {
            if (fila != null && fila.length >= 2) {
                agregar(new Evento(fila[0], fila[1]));
            }
        }
    }

    /** Copia del historial, para que la ventana pinte lo que pasó antes de abrirse. */
    public static List<Evento> historial() {
        synchronized (historial) {
            return new ArrayList<>(historial);
        }
    }

    /** La ventana se suscribe aquí para recibir cada evento nuevo (en el hilo de Swing). */
    public static void alAgregar(Consumer<Evento> oyente) {
        oyentes.add(oyente);
    }

    private static void agregar(Evento evento) {
        synchronized (historial) {
            historial.add(evento);
        }
        System.out.println(evento.fecha() + "  " + evento.texto());
        for (Consumer<Evento> oyente : oyentes) {
            SwingUtilities.invokeLater(() -> oyente.accept(evento));
        }
    }

    /**
     * Decide la categoría de un evento leyendo su texto.
     * El núcleo anota los paquetes como "[LOGIN] de=ana para=null", así que primero
     * busca el tipo entre corchetes y si no hay, se guía por palabras clave.
     */
    public static String categoriaDe(String texto) {
        String t = texto.toLowerCase(Locale.ROOT);

        if (t.startsWith("error") || t.contains("excepción") || t.contains("exception")) {
            return ERRORES;
        }

        if (texto.startsWith("[") && texto.indexOf(']') > 1) {
            String nombre = texto.substring(1, texto.indexOf(']')).trim();
            try {
                return categoriaDe(Tipo.valueOf(nombre));
            } catch (IllegalArgumentException noEsUnTipo) {
                // No era un Tipo; seguimos con las palabras clave.
            }
        }

        if (t.contains("sesión") || t.contains("login") || t.contains("registr") || t.contains("bloque")) {
            return ACCESO;
        }
        if (t.contains("grupo")) {
            return GRUPOS;
        }
        if (t.contains("amig") || t.contains("solicitud")) {
            return AMIGOS;
        }
        if (t.contains("conexión") || t.contains("conect") || t.contains("servidor")) {
            return CONEXIONES;
        }
        return OTROS;
    }

    private static String categoriaDe(Tipo tipo) {
        switch (tipo) {
            case REGISTRO: case LOGIN: case LOGOUT:
            case RECUPERAR_PREGUNTA: case RECUPERAR: case ELIMINAR_CUENTA:
                return ACCESO;
            case LISTA_USUARIOS: case USUARIO_CONECTADO: case USUARIO_DESCONECTADO: case MSG_TEMPORAL:
                return USUARIOS;
            case SOLICITUD_AMISTAD: case RESPUESTA_AMISTAD: case ELIMINAR_AMIGO:
            case LISTA_AMIGOS: case MSG_AMIGO: case HISTORIAL_AMIGO:
                return AMIGOS;
            case GRUPO_CREAR: case GRUPO_INVITAR: case GRUPO_RESPUESTA: case GRUPO_MSG: case GRUPO_HISTORIAL:
            case GRUPO_SALIR: case GRUPO_QUITAR: case GRUPO_RENOMBRAR: case GRUPO_ELIMINAR:
                return GRUPOS;
            case ERROR:
                return ERRORES;
            default:
                return OTROS;
        }
    }

    /** Un renglón de la bitácora. */
    public static final class Evento {
        private final String fecha;
        private final String texto;
        private final String categoria;

        public Evento(String fecha, String texto) {
            this.fecha = fecha;
            this.texto = texto;
            this.categoria = categoriaDe(texto);
        }

        public String fecha() { return fecha; }
        public String texto() { return texto; }
        public String categoria() { return categoria; }
    }
}
