package chat.servidor.registro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * Hash con sal para contraseñas y respuestas de seguridad.
 * <p>
 * Dueño: Integrante 05 · Registro — Mery Natalia Dávila González (servidor).
 * <p>
 * Lo que se guarda en la BD (columna psw de usuarios y respuesta_hash de
 * respuestas_seguridad) tiene el formato {@code sal:hash}, los dos en Base64.
 * Como la tabla no tiene columna para la sal, va junto con el hash.
 * <p>
 * Uso para las demás áreas (06 · Login, 07 · Cuenta, datos de prueba de 02):
 * <pre>
 * String guardado = Hash.generar(contrasena);              // al registrar
 * boolean correcta = Hash.verificar(contrasena, guardado); // al iniciar sesión
 *
 * String guardada = Hash.generarRespuesta(respuesta);              // al registrar
 * boolean ok = Hash.verificarRespuesta(respuesta, guardada);       // al recuperar acceso
 * </pre>
 */
public final class Hash {

    /** Bytes de sal aleatoria por cada valor guardado. */
    private static final int BYTES_SAL = 16;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Hash() {
        // Solo métodos estáticos
    }

    /** Genera una sal nueva y devuelve {@code sal:hash} listo para guardar. */
    public static String generar(String texto) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = sha256(sal, texto);
        Base64.Encoder b64 = Base64.getEncoder();
        return b64.encodeToString(sal) + ":" + b64.encodeToString(hash);
    }

    /**
     * Comprueba si {@code texto} es el mismo que se usó para generar {@code guardado}.
     *
     * @return false si no coincide o si {@code guardado} no tiene el formato sal:hash
     */
    public static boolean verificar(String texto, String guardado) {
        if (texto == null || guardado == null) {
            return false;
        }
        String[] partes = guardado.split(":");
        if (partes.length != 2) {
            return false;
        }
        try {
            Base64.Decoder b64 = Base64.getDecoder();
            byte[] sal = b64.decode(partes[0]);
            byte[] esperado = b64.decode(partes[1]);
            // isEqual compara en tiempo constante
            return MessageDigest.isEqual(esperado, sha256(sal, texto));
        } catch (IllegalArgumentException e) {
            return false; // Base64 inválido
        }
    }

    /**
     * Igual que {@link #generar}, pero sin distinguir mayúsculas ni espacios
     * en los extremos: "Firulais " y "firulais" se toman como la misma respuesta.
     */
    public static String generarRespuesta(String respuesta) {
        return generar(normalizar(respuesta));
    }

    /** Compara una respuesta de seguridad con la guardada (ver {@link #generarRespuesta}). */
    public static boolean verificarRespuesta(String respuesta, String guardada) {
        return verificar(normalizar(respuesta), guardada);
    }

    private static String normalizar(String respuesta) {
        return respuesta == null ? null : respuesta.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] sha256(byte[] sal, String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(sal);
            md.update(texto.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            // Todas las versiones de Java traen SHA-256
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
