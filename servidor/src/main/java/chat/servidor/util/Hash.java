package chat.servidor.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Hash {
    private static final int ITERACIONES = 65_000;
    private static final int BITS_CLAVE = 256;
    private static final int BYTES_SAL = 16;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Hash() { }

    public static String hashear(String texto) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(texto, sal, ITERACIONES);
        return ITERACIONES + ":" + Base64.getEncoder().encodeToString(sal)
                + ":" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verificar(String texto, String guardado) {
        if (texto == null || guardado == null) {
            return false;
        }
        String[] partes = guardado.split(":");
        if (partes.length != 3) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[0]);
            byte[] sal = Base64.getDecoder().decode(partes[1]);
            byte[] esperado = Base64.getDecoder().decode(partes[2]);
            return MessageDigest.isEqual(esperado, derivar(texto, sal, iteraciones));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Minúsculas, sin acentos y sin espacios sobrantes, para que coincidan. */
    public static String normalizarRespuesta(String respuesta) {
        String sinAcentos = Normalizer.normalize(respuesta.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase().replaceAll("\\s+", " ");
    }

    private static byte[] derivar(String texto, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(texto.toCharArray(), sal, iteraciones, BITS_CLAVE);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        }
    }
}
