package chat.comun.modelo;

import java.io.Serializable;

public class Grupo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String nombre;
    private final int idDueno;
    private final String nombreDueno;

    public Grupo(int id, String nombre, int idDueno, String nombreDueno) {
        this.id = id;
        this.nombre = nombre;
        this.idDueno = idDueno;
        this.nombreDueno = nombreDueno;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getIdDueno() { return idDueno; }
    public String getNombreDueno() { return nombreDueno; }

    @Override
    public String toString() { return nombre; }
}
