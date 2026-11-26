package chat.servidor.nucleo;

import chat.comun.protocolo.Paquete;
import chat.comun.protocolo.Tipo;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * El cartero del servidor. Revisa cada paquete, lo registra en la bitácora y lo entrega
 * al servicio de su {@link Tipo}. Las reglas comunes se validan aquí, no en cada servicio:
 * sesión obligatoria, remitente correcto y cierre de sesión con LOGOUT.
 */
public final class Despachador {
    /** Tipos que se pueden usar sin haber iniciado sesión. */
    private static final Set<Tipo> SIN_SESION = EnumSet.of(
            Tipo.REGISTRO, Tipo.LOGIN, Tipo.RECUPERAR_PREGUNTA, Tipo.RECUPERAR);

    private final Map<Tipo, Servicio> servicios = new EnumMap<>(Tipo.class);

    /** Asocia un servicio a uno o más tipos. Se llama al arrancar, antes de aceptar clientes. */
    public void registrar(Servicio servicio, Tipo... tipos) {
        for (Tipo tipo : tipos) {
            if (servicios.putIfAbsent(tipo, servicio) != null) {
                throw new IllegalStateException("Ya hay un servicio para " + tipo);
            }
        }
    }

    public void despachar(Paquete paquete, ManejadorCliente remitente) {
        Eventos.registrar("[" + paquete.getTipo() + "] de=" + paquete.getDe() + " para=" + paquete.getPara());

        if (paquete.getTipo() == Tipo.LOGOUT) {
            remitente.cerrarSesion(); // ya anota "X cerró sesión." en la bitácora
            return;
        }

        String usuario = remitente.getUsuario();
        if (usuario == null && !SIN_SESION.contains(paquete.getTipo())) {
            remitente.enviar(Paquete.error(paquete, "Inicia sesión primero."));
            return;
        }
        if (usuario != null && !usuario.equals(paquete.getDe())) {
            remitente.enviar(Paquete.error(paquete, "Remitente inválido."));
            return;
        }

        Servicio servicio = servicios.get(paquete.getTipo());
        if (servicio == null) {
            remitente.enviar(Paquete.error(paquete, "Esta función todavía no está disponible."));
            return;
        }

        try {
            servicio.atender(paquete, remitente);
        } catch (RuntimeException e) {
            Eventos.registrar("Error atendiendo " + paquete.getTipo() + ": " + e);
            remitente.enviar(Paquete.error(paquete, "Error interno del servidor."));
        }
    }
}
