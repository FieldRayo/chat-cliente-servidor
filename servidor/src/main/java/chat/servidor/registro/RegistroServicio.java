package chat.servidor.registro;

import chat.comun.protocolo.Paquete;
import chat.servidor.bd.SeguridadDAO;
import chat.servidor.bd.UsuarioDAO;
import chat.servidor.nucleo.Eventos;
import chat.servidor.nucleo.ManejadorCliente;
import chat.servidor.nucleo.Servicio;
import java.util.Map;

/**
 * Atiende los paquetes REGISTRO: crea la cuenta de un usuario nuevo.
 * <p>
 * Dueño: Integrante 05 · Registro — Mery Natalia Dávila González (servidor).
 * <p>
 * Los datos llegan en un HashMap que arma VentanaRegistro (cliente) con las claves
 * de abajo. Se vuelve a validar todo (nunca se confía en el cliente), se revisa que
 * el usuario y el correo no existan, y se guardan la contraseña y la respuesta de
 * seguridad con {@link Hash}. Contesta OK o ERROR con el motivo.
 * <p>
 * Se conecta en el servidor con:
 * <pre>despachador.registrar(new RegistroServicio(), Tipo.REGISTRO);</pre>
 */
public class RegistroServicio implements Servicio {

    // Mismas claves y límites que VentanaRegistro (cliente)
    static final String CAMPO_USUARIO = "usuario";
    static final String CAMPO_EMAIL = "email";
    static final String CAMPO_CONTRASENA = "contrasena";
    static final String CAMPO_ID_PREGUNTA = "idPregunta";
    static final String CAMPO_RESPUESTA = "respuesta";

    static final int MIN_CONTRASENA = 6;
    static final int MAX_TEXTO = 50;

    /**
     * Evita que dos registros al mismo tiempo pasen la revisión de "no existe"
     * con el mismo nombre o correo.
     */
    private static final Object CANDADO = new Object();

    private final UsuarioDAO usuarios;
    private final SeguridadDAO seguridad;

    public RegistroServicio() {
        this(new UsuarioDAO(), new SeguridadDAO());
    }

    /** Para pruebas: permite usar DAOs falsos. */
    RegistroServicio(UsuarioDAO usuarios, SeguridadDAO seguridad) {
        this.usuarios = usuarios;
        this.seguridad = seguridad;
    }

    @Override
    public void atender(Paquete peticion, ManejadorCliente remitente) {
        remitente.enviar(procesar(peticion));
    }

    /** Hace el registro y devuelve la respuesta (OK o ERROR) que se le manda al cliente. */
    Paquete procesar(Paquete peticion) {
        if (!(peticion.getDatos() instanceof Map<?, ?> datos)) {
            return Paquete.error(peticion, "Datos de registro inválidos.");
        }

        String usuario = texto(datos, CAMPO_USUARIO).trim();
        String email = texto(datos, CAMPO_EMAIL).trim();
        String contrasena = texto(datos, CAMPO_CONTRASENA);
        String respuesta = texto(datos, CAMPO_RESPUESTA).trim();
        int idPregunta = numero(texto(datos, CAMPO_ID_PREGUNTA));

        String error = validar(usuario, email, contrasena, idPregunta, respuesta);
        if (error != null) {
            return Paquete.error(peticion, error);
        }

        synchronized (CANDADO) {
            if (usuarios.existeNombre(usuario)) {
                return Paquete.error(peticion, "El nombre de usuario ya existe.");
            }
            if (usuarios.existeEmail(email)) {
                return Paquete.error(peticion, "El correo ya está registrado.");
            }
            usuarios.registrar(usuario, email, Hash.generar(contrasena),
                    idPregunta, Hash.generarRespuesta(respuesta));
        }

        Eventos.registrar("Usuario registrado: " + usuario);
        return Paquete.ok(peticion, "Cuenta creada.");
    }

    /** Las mismas reglas que el cliente, más que la pregunta exista en la BD. */
    private String validar(String usuario, String email, String contrasena,
            int idPregunta, String respuesta) {
        if (usuario.isEmpty() || email.isEmpty() || contrasena.isEmpty() || respuesta.isEmpty()) {
            return "Llena todos los campos.";
        }
        if (usuario.length() > MAX_TEXTO) {
            return "El nombre de usuario puede tener máximo " + MAX_TEXTO + " caracteres.";
        }
        if (email.length() > MAX_TEXTO) {
            return "El correo puede tener máximo " + MAX_TEXTO + " caracteres.";
        }
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            return "Escribe un correo válido, por ejemplo nombre@correo.com.";
        }
        if (contrasena.length() < MIN_CONTRASENA) {
            return "La contraseña debe tener al menos " + MIN_CONTRASENA + " caracteres.";
        }
        if (!seguridad.listarPreguntas().containsKey(idPregunta)) {
            return "Elige una pregunta de seguridad válida.";
        }
        return null;
    }

    /** Valor de una clave del HashMap como texto; "" si no viene o no es texto. */
    private static String texto(Map<?, ?> datos, String clave) {
        Object valor = datos.get(clave);
        return valor instanceof String s ? s : "";
    }

    /** Convierte el id de la pregunta; -1 si no es un número. */
    private static int numero(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
