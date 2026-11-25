package chat.comun.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Mensaje de la vista de amigos o de un grupo. */
public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private final int idEmisor;
    private final String nombreEmisor;
    private final String contenido;
    private final LocalDateTime fecha;

    public Mensaje(long id, int idEmisor, String nombreEmisor, String contenido, LocalDateTime fecha) {
        this.id = id;
        this.idEmisor = idEmisor;
        this.nombreEmisor = nombreEmisor;
        this.contenido = contenido;
        this.fecha = fecha;
    }

    public long getId() { return id; }
    public int getIdEmisor() { return idEmisor; }
    public String getNombreEmisor() { return nombreEmisor; }
    public String getContenido() { return contenido; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() { return nombreEmisor + ": " + contenido; }
}
