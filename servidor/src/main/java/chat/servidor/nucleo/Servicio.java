package chat.servidor.nucleo;

import chat.comun.protocolo.Paquete;

/**
 * Lógica de una función (registro, login, amistades...). El {@link Despachador} le entrega
 * cada petición del tipo para el que se registró. Para contestar usa
 * {@link ManejadorCliente#enviar} o {@link Sesiones#enviarA} para otro usuario.
 */
@FunctionalInterface
public interface Servicio {
    void atender(Paquete peticion, ManejadorCliente remitente);
}
