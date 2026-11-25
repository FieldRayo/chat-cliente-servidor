package com.cocochat.comun.modelo;

import java.io.Serializable;

/** Solicitud de amistad pendiente. */
public class Solicitud implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int idAmistad;
    private final int idSolicitante;
    private final String nombreSolicitante;
    private final int idReceptor;

    public Solicitud(int idAmistad, int idSolicitante, String nombreSolicitante, int idReceptor) {
        this.idAmistad = idAmistad;
        this.idSolicitante = idSolicitante;
        this.nombreSolicitante = nombreSolicitante;
        this.idReceptor = idReceptor;
    }

    public int getIdAmistad() { return idAmistad; }
    public int getIdSolicitante() { return idSolicitante; }
    public String getNombreSolicitante() { return nombreSolicitante; }
    public int getIdReceptor() { return idReceptor; }

    @Override
    public String toString() { return nombreSolicitante + " quiere ser tu amigo"; }
}
