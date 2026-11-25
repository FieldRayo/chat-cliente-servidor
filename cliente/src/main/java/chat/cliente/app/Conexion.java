package chat.cliente.app;

import chat.comun.protocolo.Paquete;
import chat.comun.protocolo.Tipo;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * El teléfono del cliente: la única conexión con el servidor.
 *
 * <pre>
 * conexion.alRecibir(Tipo.LOGIN, p -> {
 *     if (p.esOk()) ... else mostrarError((String) p.getDatos());
 * });
 * conexion.enviar(new Paquete(Tipo.LOGIN, usuario, null, contrasena));
 * </pre>
 *
 * Quien se suscribe a un tipo recibe los paquetes de ese tipo Y los OK/ERROR que lo contestan.
 * Los avisos llegan ya en el hilo de Swing: puedes tocar la ventana directamente.
 */
public final class Conexion {
    private final Socket socket;
    private final ObjectOutputStream salida;
    private final ObjectInputStream entrada;
    private final Map<Tipo, List<Consumer<Paquete>>> oyentes = new ConcurrentHashMap<>();
    private volatile String usuario;

    /** Se conecta al servidor. Lanza IOException si la IP está mal o el servidor está apagado. */
    public Conexion(String ip, int puerto) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, puerto), 3000);
        salida = new ObjectOutputStream(socket.getOutputStream());
        salida.flush(); // la cabecera primero, o los dos lados se quedan esperando
        entrada = new ObjectInputStream(socket.getInputStream());
        Thread lector = new Thread(this::leer, "lector-servidor");
        lector.setDaemon(true);
        lector.start();
    }

    /** Manda un paquete al servidor. Se puede llamar desde cualquier hilo. */
    public synchronized void enviar(Paquete p) {
        try {
            salida.writeObject(p);
            salida.reset(); // si no, un objeto reenviado llegaría con sus datos viejos
            salida.flush();
        } catch (IOException e) {
            perdida();
        }
    }

    /** Llama a {@code accion} cada vez que llegue un paquete de {@code tipo} (o su OK/ERROR). */
    public void alRecibir(Tipo tipo, Consumer<Paquete> accion) {
        oyentes.computeIfAbsent(tipo, t -> new CopyOnWriteArrayList<>()).add(accion);
    }

    /** Deja de avisar a {@code accion}. Úsalo al cerrar tu ventana para no recibir dos veces al reabrirla. */
    public void quitar(Tipo tipo, Consumer<Paquete> accion) {
        List<Consumer<Paquete>> l = oyentes.get(tipo);
        if (l != null) l.remove(accion);
    }

    /** Olvida todas las suscripciones (al cerrar sesión, para no arrastrar ventanas viejas). */
    void quitarTodos() { oyentes.clear(); }

    /** Usuario con sesión iniciada; null antes del login. Lo pone 06 · Login al recibir OK. */
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    private void leer() {
        try {
            while (true) {
                Paquete p = (Paquete) entrada.readObject();
                SwingUtilities.invokeLater(() -> repartir(p));
            }
        } catch (IOException | ClassNotFoundException e) {
            perdida();
        }
    }

    void repartir(Paquete p) {
        avisar(p.getTipo(), p);
        if (p.getRespuestaA() != null) avisar(p.getRespuestaA(), p);
    }

    private void avisar(Tipo tipo, Paquete p) {
        for (Consumer<Paquete> accion : oyentes.getOrDefault(tipo, List.of())) {
            try {
                accion.accept(p);
            } catch (RuntimeException e) {
                e.printStackTrace(); // un error en una ventana no debe tumbar a las demás
            }
        }
    }

    private volatile boolean cerrada;

    // ponytail: sin reconexión automática; si se cae el servidor se avisa y se cierra el cliente.
    private void perdida() {
        if (cerrada) return;
        cerrada = true;
        try { socket.close(); } catch (IOException ignored) { }
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, "Se perdió la conexión con el servidor.",
                    "Chat", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        });
    }
}
