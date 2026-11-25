package chat.servidor.nucleo;

import chat.comun.protocolo.Paquete;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista de quién está conectado: usuario -> su conexión activa.
 * Los servicios la usan para entregar algo a otro usuario, por ejemplo {@code Sesiones.enviarA("luis", p)}.
 */
public final class Sesiones {
    private static final Map<String, ManejadorCliente> activos = new ConcurrentHashMap<>();

    private Sesiones() { }

    /** Solo lo llama {@link ManejadorCliente#iniciarSesion}. Falla si el usuario ya tiene sesión. */
    static boolean iniciar(String usuario, ManejadorCliente manejador) {
        return activos.putIfAbsent(usuario, manejador) == null;
    }

    /** Solo lo llama {@link ManejadorCliente#cerrarSesion}. */
    static void cerrar(String usuario, ManejadorCliente manejador) {
        activos.remove(usuario, manejador);
    }

    public static boolean estaConectado(String usuario) {
        return activos.containsKey(usuario);
    }

    public static Set<String> conectados() {
        return Set.copyOf(activos.keySet());
    }

    /** Envía el paquete al usuario si está conectado. Devuelve false si no lo está. */
    public static boolean enviarA(String usuario, Paquete paquete) {
        ManejadorCliente manejador = activos.get(usuario);
        if (manejador == null) return false;
        manejador.enviar(paquete);
        return true;
    }

    /** Envía el paquete a todos los conectados. */
    public static void difundir(Paquete paquete) {
        for (ManejadorCliente manejador : activos.values()) {
            manejador.enviar(paquete);
        }
    }
}
