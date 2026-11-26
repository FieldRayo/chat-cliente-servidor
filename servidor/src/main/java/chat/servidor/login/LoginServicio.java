package chat.servidor.login;

import chat.comun.modelo.Usuario;
import chat.comun.protocolo.Paquete;
import chat.servidor.bd.UsuarioDAO;
import chat.servidor.nucleo.ManejadorCliente;
import chat.servidor.nucleo.Servicio;
import chat.servidor.nucleo.Sesiones;
import chat.servidor.registro.Hash;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lógica del inicio de sesión (servidor).
 * Dueño: Integrante 06 · Inicio de sesión.
 *
 * Petición: Paquete(Tipo.LOGIN, usuario, null, contraseña).
 * Respuestas:
 *  - OK con datos "OK": entró.
 *  - OK con datos IR_REGISTRO: 3 fallos con un usuario que no existe; el cliente abre el registro.
 *  - ERROR con el motivo para mostrar al usuario.
 */
public class LoginServicio implements Servicio {

    public static final int MAX_INTENTOS = 3;
    public static final String IR_REGISTRO = "IR_REGISTRO";

    private final UsuarioDAO usuarios = new UsuarioDAO();

    /** Fallos con usuarios que no existen, por conexión (la BD no tiene dónde guardarlos). */
    private final Map<ManejadorCliente, Integer> fallosInexistente =
            Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void atender(Paquete peticion, ManejadorCliente remitente) {
        String nombre = peticion.getDe();
        String contrasena = peticion.getDatos() instanceof String s ? s : null;

        if (nombre == null || nombre.isBlank() || contrasena == null || contrasena.isEmpty()) {
            remitente.enviar(Paquete.error(peticion, "Llena usuario y contraseña."));
            return;
        }

        Usuario usuario = usuarios.buscarPorNombre(nombre);

        // 1. El usuario no existe: a los 3 intentos se manda al registro.
        if (usuario == null) {
            int fallos;
            synchronized (fallosInexistente) {
                fallos = fallosInexistente.merge(remitente, 1, Integer::sum);
                if (fallos >= MAX_INTENTOS) fallosInexistente.remove(remitente);
            }
            if (fallos >= MAX_INTENTOS) {
                remitente.enviar(Paquete.ok(peticion, IR_REGISTRO));
            } else {
                remitente.enviar(Paquete.error(peticion,
                        "Usuario o contraseña incorrectos. Intento " + fallos + " de " + MAX_INTENTOS + "."));
            }
            return;
        }

        // 2. Cuenta bloqueada: no entra ni con la contraseña correcta.
        if (usuario.isBloqueado()) {
            remitente.enviar(Paquete.error(peticion,
                    "Tu cuenta está bloqueada. Recupera tu acceso con \"¿Olvidaste tu acceso?\"."));
            return;
        }

        // 3. Contraseña incorrecta: suma intento y bloquea al tercero.
        if (!Hash.verificar(contrasena, usuarios.obtenerHash(nombre))) {
            int intentos = usuarios.sumarIntentoFallido(usuario.getId());
            if (intentos >= MAX_INTENTOS) {
                usuarios.bloquear(usuario.getId());
                remitente.enviar(Paquete.error(peticion,
                        "Fallaste " + MAX_INTENTOS + " veces. Tu cuenta se bloqueó; recupera tu acceso."));
            } else {
                remitente.enviar(Paquete.error(peticion,
                        "Usuario o contraseña incorrectos. Intento " + intentos + " de " + MAX_INTENTOS + "."));
            }
            return;
        }

        // 4. Contraseña correcta: no se permiten dos sesiones con el mismo usuario.
        if (Sesiones.estaConectado(nombre) || !remitente.iniciarSesion(nombre)) {
            remitente.enviar(Paquete.error(peticion, "Este usuario ya tiene una sesión abierta."));
            return;
        }

        usuarios.reiniciarIntentos(usuario.getId());
        fallosInexistente.remove(remitente);
        remitente.enviar(Paquete.ok(peticion, "OK"));
    }
}
