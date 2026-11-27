package chat.servidor.nucleo;

import chat.comun.protocolo.Paquete;
import chat.comun.protocolo.Tipo;
import chat.servidor.bd.BaseDatos;
import chat.servidor.cuenta.CuentaServicio;
import chat.servidor.gui.VentanaServidor;
import chat.servidor.login.LoginServicio;
import chat.servidor.registro.RegistroServicio;

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
        // 1. Base de datos (02): crea chat.db y sus tablas si no existen.
        BaseDatos.inicializar();

        // 2. Ventana del servidor (04): muestra la bitácora y conecta Eventos con Bitacora.
        VentanaServidor.abrir(Paquete.PUERTO);

        // 3. Servicios de cada área. Una línea por servicio; los que faltan se agregan al terminarlos.
        Despachador despachador = new Despachador();
        despachador.registrar(new RegistroServicio(), Tipo.REGISTRO);   // 05 · Registro
        despachador.registrar(new LoginServicio(), Tipo.LOGIN);         // 06 · Inicio de sesión
        CuentaServicio.registrarEn(despachador);                        // 07 · Recuperar acceso y eliminar cuenta

        // 4. Red: abre el puerto y atiende clientes.
        arrancar(despachador, Paquete.PUERTO);
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
            while (!servidor.isClosed()) {
                Socket socket;
                try {
                    socket = servidor.accept();
                } catch (IOException e) {
                    // Un fallo al aceptar a un cliente no debe tumbar el servidor completo.
                    Eventos.registrar("No se pudo aceptar una conexión: " + e.getMessage());
                    continue;
                }
                try {
                    pool.execute(new ManejadorCliente(socket, despachador));
                } catch (RejectedExecutionException e) {
                    Eventos.registrar("Servidor lleno; se rechazó a " + socket.getRemoteSocketAddress() + ".");
                    socket.close();
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
