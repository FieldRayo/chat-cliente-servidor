package chat.servidor.nucleo;

import chat.comun.protocolo.Paquete;
import chat.comun.protocolo.Tipo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Atiende una conexión de cliente en su propio hilo. Lee paquetes y los entrega al
 * {@link Despachador}. Cuando el cliente se va (cerrar sesión o desconexión abrupta),
 * lo saca de {@link Sesiones} y avisa a los demás.
 */
public final class ManejadorCliente implements Runnable {
    private final Socket socket;
    private final Despachador despachador;
    private final AtomicBoolean cerrado = new AtomicBoolean(false);
    private ObjectOutputStream salida;
    private volatile String usuario;

    public ManejadorCliente(Socket socket, Despachador despachador) {
        this.socket = socket;
        this.despachador = despachador;
    }

    @Override
    public void run() {
        try {
            socket.setKeepAlive(true); // detecta cables o redes caídos sin que el cliente cierre
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();                                   // cabecera primero (como en Conexion)
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
            Eventos.registrar("Conexión desde " + socket.getRemoteSocketAddress());

            while (!cerrado.get()) {
                Paquete paquete = (Paquete) entrada.readObject();
                despachador.despachar(paquete, this);
            }
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            // Desconexión normal o abrupta: se trata igual que cerrar sesión.
        } finally {
            cerrarConexion();
        }
    }

    /** Envía un paquete a este cliente. Si el envío falla, la conexión se cierra. */
    public void enviar(Paquete paquete) {
        if (cerrado.get()) return;
        try {
            synchronized (this) {                // dos hilos no pueden escribir a la vez
                salida.writeObject(paquete);
                salida.reset();                   // si no, un objeto reenviado llega con datos viejos
                salida.flush();
            }
        } catch (IOException e) {
            cerrarConexion();
        }
    }

    /**
     * Registra al usuario como conectado y avisa a todos. Devuelve false si ya tenía sesión
     * o si ese usuario ya está conectado desde otra conexión.
     * Lo llama el servicio de login al validar la contraseña.
     */
    public boolean iniciarSesion(String nuevoUsuario) {
        if (usuario != null) return false;
        if (!Sesiones.iniciar(nuevoUsuario, this)) return false;
        usuario = nuevoUsuario;
        Eventos.registrar(nuevoUsuario + " inició sesión.");
        Sesiones.difundir(new Paquete(Tipo.USUARIO_CONECTADO, null, null, nuevoUsuario));
        return true;
    }

    /**
     * Termina la sesión y avisa a los demás, pero deja la conexión abierta: el cliente
     * vuelve al login con el mismo socket. Lo usan LOGOUT y, después de eliminar la cuenta, 07 · Cuenta.
     */
    public void cerrarSesion() {
        String anterior = usuario;
        if (anterior == null) return;
        usuario = null;
        Sesiones.cerrar(anterior, this);
        Eventos.registrar(anterior + " cerró sesión.");
        Sesiones.difundir(new Paquete(Tipo.USUARIO_DESCONECTADO, null, null, anterior));
    }

    /** Cierra la conexión por completo (salida de la aplicación o caída). */
    private void cerrarConexion() {
        if (!cerrado.compareAndSet(false, true)) return;
        cerrarSesion();
        try {
            socket.close();
        } catch (IOException ignored) {
            // ya estaba cerrado
        }
    }

    public String getUsuario() {
        return usuario;
    }
}
