package chat.servidor.nucleo;

import java.util.function.Consumer;

/**
 * Punto único para anotar lo que pasa en el servidor. El núcleo llama a {@link #registrar};
 * 04 · Ventana del servidor decide a dónde va cada evento (bitácora en pantalla y en la BD)
 * con {@link #alRegistrar}. Por defecto solo se imprime en consola.
 */
public final class Eventos {
    private static volatile Consumer<String> destino = System.out::println;

    private Eventos() { }

    public static void alRegistrar(Consumer<String> nuevoDestino) {
        destino = nuevoDestino;
    }

    /**
     * Anota un evento. Si el destino falla (por ejemplo, la BD de la bitácora), el error se
     * imprime y no se propaga: anotar nunca debe desconectar a un cliente.
     */
    public static void registrar(String evento) {
        try {
            destino.accept(evento);
        } catch (RuntimeException e) {
            System.err.println("No se pudo registrar el evento \"" + evento + "\": " + e);
        }
    }
}
