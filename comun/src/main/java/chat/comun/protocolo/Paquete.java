package chat.comun.protocolo;

import java.io.Serializable;

/**
 * El sobre: lo ÚNICO que viaja por la red entre cliente y servidor.
 *
 * <pre>
 * new Paquete(Tipo.LOGIN, "ana", null, "contraseña")          // petición
 * Paquete.ok(peticion, listaDeUsuarios)                       // respuesta del servidor
 * Paquete.error(peticion, "Contraseña incorrecta")
 * </pre>
 *
 * Las respuestas OK/ERROR llevan en {@code respuestaA} el tipo de la petición,
 * para que cada ventana sepa que la respuesta es suya.
 * Lo que vaya en {@code datos} tiene que ser Serializable (String, modelos de comun, ArrayList...).
 */
public class Paquete implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Puerto en el que escucha el servidor si nadie dice otro. */
    public static final int PUERTO = 5000;

    private final Tipo tipo;
    private final String de;
    private final String para;
    private final Serializable datos;
    private final Tipo respuestaA;

    public Paquete(Tipo tipo, String de, String para, Serializable datos) {
        this(tipo, de, para, datos, null);
    }

    private Paquete(Tipo tipo, String de, String para, Serializable datos, Tipo respuestaA) {
        if (tipo == null) throw new IllegalArgumentException("El paquete necesita un tipo");
        this.tipo = tipo;
        this.de = de;
        this.para = para;
        this.datos = datos;
        this.respuestaA = respuestaA;
    }

    /** Respuesta exitosa a {@code peticion}; va dirigida a quien la mandó. */
    public static Paquete ok(Paquete peticion, Serializable datos) {
        return new Paquete(Tipo.OK, null, peticion.de, datos, peticion.tipo);
    }

    /** Respuesta de error a {@code peticion}; {@code motivo} es el texto que verá el usuario. */
    public static Paquete error(Paquete peticion, String motivo) {
        return new Paquete(Tipo.ERROR, null, peticion.de, motivo, peticion.tipo);
    }

    public Tipo getTipo() { return tipo; }
    public String getDe() { return de; }
    public String getPara() { return para; }
    public Serializable getDatos() { return datos; }
    /** Tipo de la petición que contesta este OK/ERROR; null en cualquier otro paquete. */
    public Tipo getRespuestaA() { return respuestaA; }
    public boolean esOk() { return tipo == Tipo.OK; }
    public boolean esError() { return tipo == Tipo.ERROR; }

    @Override
    public String toString() {
        return tipo + (respuestaA != null ? "(" + respuestaA + ")" : "")
                + " de=" + de + " para=" + para + " datos=" + datos;
    }
}
