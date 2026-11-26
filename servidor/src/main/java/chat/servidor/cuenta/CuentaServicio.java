package chat.servidor.cuenta;

import chat.comun.modelo.Grupo;
import chat.comun.modelo.Usuario;
import chat.comun.protocolo.Paquete;
import chat.comun.protocolo.Tipo;
import chat.servidor.bd.AmistadDAO;
import chat.servidor.bd.BdException;
import chat.servidor.bd.GrupoDAO;
import chat.servidor.bd.SeguridadDAO;
import chat.servidor.bd.UsuarioDAO;
import chat.servidor.nucleo.Despachador;
import chat.servidor.nucleo.Eventos;
import chat.servidor.nucleo.ManejadorCliente;
import chat.servidor.nucleo.Servicio;
import chat.servidor.nucleo.Sesiones;
import chat.servidor.registro.Hash;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Recuperación de acceso y eliminación de la cuenta que inició sesión. */
public final class CuentaServicio implements Servicio {
    private static final int MAX_INTENTOS = 5;
    private static final long PERIODO_INTENTOS = Duration.ofMinutes(5).toNanos();
    private static final String CUENTA_SIN_PREGUNTA = "No hay una pregunta de seguridad para esa cuenta.";

    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final SeguridadDAO seguridad = new SeguridadDAO();
    private final AmistadDAO amistades = new AmistadDAO();
    private final GrupoDAO grupos = new GrupoDAO();
    private final Map<Integer, Intentos> intentos = new HashMap<>();

    /** El núcleo registra una sola instancia antes de aceptar clientes. */
    public static void registrarEn(Despachador despachador) {
        despachador.registrar(new CuentaServicio(), Tipo.RECUPERAR_PREGUNTA,
                Tipo.RECUPERAR, Tipo.ELIMINAR_CUENTA);
    }

    @Override
    public void atender(Paquete peticion, ManejadorCliente remitente) {
        switch (peticion.getTipo()) {
            case RECUPERAR_PREGUNTA -> consultarPregunta(peticion, remitente);
            case RECUPERAR -> restablecer(peticion, remitente);
            case ELIMINAR_CUENTA -> eliminar(peticion, remitente);
            default -> remitente.enviar(Paquete.error(peticion, "Operación de cuenta desconocida."));
        }
    }

    private void consultarPregunta(Paquete peticion, ManejadorCliente remitente) {
        Usuario usuario = buscarCuenta(peticion.getDe());
        String pregunta = usuario == null ? null : seguridad.preguntasDeUsuario(usuario.getId())
                .values().stream().findFirst().orElse(null);
        remitente.enviar(pregunta == null ? Paquete.error(peticion, CUENTA_SIN_PREGUNTA)
                : Paquete.ok(peticion, pregunta));
    }

    private synchronized void restablecer(Paquete peticion, ManejadorCliente remitente) {
        if (!(peticion.getDatos() instanceof List<?> datos) || datos.size() != 2
                || !(datos.get(0) instanceof String respuesta) || respuesta.isBlank()
                || !(datos.get(1) instanceof String nuevaContrasena) || nuevaContrasena.length() < 6) {
            remitente.enviar(Paquete.error(peticion, "Escribe la respuesta y una contraseña de al menos 6 caracteres."));
            return;
        }

        Usuario usuario = buscarCuenta(peticion.getDe());
        if (usuario == null) {
            remitente.enviar(Paquete.error(peticion, CUENTA_SIN_PREGUNTA));
            return;
        }

        long ahora = System.nanoTime();
        intentos.values().removeIf(registro -> ahora - registro.inicio() >= PERIODO_INTENTOS);
        int id = usuario.getId();
        Intentos registro = intentos.getOrDefault(id, new Intentos(0, ahora));
        if (registro.cantidad() >= MAX_INTENTOS) {
            remitente.enviar(Paquete.error(peticion, "Demasiados intentos. Vuelve a intentarlo en unos minutos."));
            return;
        }

        Integer idPregunta = seguridad.preguntasDeUsuario(id).keySet().stream().findFirst().orElse(null);
        if (idPregunta == null) {
            remitente.enviar(Paquete.error(peticion, CUENTA_SIN_PREGUNTA));
            return;
        }
        String hashRespuesta = seguridad.respuestasHashDe(id).get(idPregunta);
        if (!Hash.verificarRespuesta(respuesta, hashRespuesta)) {
            intentos.put(id, new Intentos(registro.cantidad() + 1, registro.inicio()));
            remitente.enviar(Paquete.error(peticion, "La respuesta de seguridad es incorrecta."));
            return;
        }

        usuarios.restablecerAcceso(id, Hash.generar(nuevaContrasena));
        intentos.remove(id);
        remitente.enviar(Paquete.ok(peticion, "Contraseña restablecida. Ya puedes iniciar sesión."));
    }

    private synchronized void eliminar(Paquete peticion, ManejadorCliente remitente) {
        String nombre = remitente.getUsuario();
        Usuario usuario = buscarCuenta(nombre);
        if (usuario == null || !(peticion.getDatos() instanceof String contrasena)
                || !Hash.verificar(contrasena, usuarios.obtenerHash(nombre))) {
            remitente.enviar(Paquete.error(peticion, "No se pudo confirmar tu cuenta con esa contraseña."));
            return;
        }

        int id = usuario.getId();
        List<Usuario> amigos = amistades.amigosDe(id);
        List<Integer> gruposAjenos = Stream.concat(grupos.gruposDe(id).stream(), grupos.invitacionesDe(id).stream())
                .filter(grupo -> grupo.getIdDueno() != id)
                .map(Grupo::getId).toList();
        if (!usuarios.eliminar(id)) {
            remitente.enviar(Paquete.error(peticion, "La cuenta ya no está disponible."));
            return;
        }
        intentos.remove(id);

        // El borrado ya se confirmó en SQLite; la sesión debe cerrarse aun si falla la revisión de grupos.
        String mensaje = "Tu cuenta fue eliminada.";
        try {
            for (int idGrupo : gruposAjenos) {
                if (grupos.contarMiembros(idGrupo) < GrupoDAO.MINIMO_MIEMBROS) {
                    grupos.eliminar(idGrupo);
                }
            }
        } catch (BdException e) {
            Eventos.registrar("No se pudo completar la revisión de grupos después de eliminar " + nombre);
            mensaje = "Tu cuenta fue eliminada. El servidor dejó pendiente la revisión de grupos.";
        }

        remitente.cerrarSesion();
        for (Usuario amigo : amigos) {
            Sesiones.enviarA(amigo.getNombreUsuario(), new Paquete(Tipo.NOTIFICACION, null,
                    amigo.getNombreUsuario(), nombre + " eliminó su cuenta."));
        }
        remitente.enviar(Paquete.ok(peticion, mensaje));
    }

    private Usuario buscarCuenta(String nombre) {
        return nombre == null || nombre.isBlank() ? null : usuarios.buscarPorNombre(nombre);
    }

    private record Intentos(int cantidad, long inicio) { }
}
