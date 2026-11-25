package chat.servidor.nucleo;

import chat.comun.protocolo.Paquete;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Arranque del servidor. Acepta clientes y atiende cada uno en un hilo del pool.
 * El pool tiene un límite: cuando está lleno, la conexión nueva se rechaza en lugar de
 * encolarse sin fin, para que el servidor no se sature.
 */
public final class ServidorMain {
    /** Clientes atendidos al mismo tiempo. */
    public static final int MAX_CLIENTES = 50;

    private ServidorMain() { }

    public static void main(String[] args) throws IOException {
        arrancar(new Despachador(), Paquete.PUERTO);
    }

    /**
     * Abre el puerto y atiende clientes hasta que el proceso termine. Bloquea el hilo que la llama.
     * Los servicios de cada área se registran en el {@link Despachador} antes de llamar a esto.
     */
    public static void arrancar(Despachador despachador, int puerto) throws IOException {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                MAX_CLIENTES, MAX_CLIENTES, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), new ThreadPoolExecutor.AbortPolicy());

        try (ServerSocket servidor = new ServerSocket(puerto)) {
            Eventos.registrar("Servidor escuchando en el puerto " + puerto + ".");
            while (true) {
                Socket socket = servidor.accept();
                try {
                    pool.execute(new ManejadorCliente(socket, despachador));
                } catch (RejectedExecutionException e) {
                    Eventos.registrar("Servidor lleno; se rechazó a " + socket.getRemoteSocketAddress() + ".");
                    socket.close();
                }
            }
        }
    }
}
