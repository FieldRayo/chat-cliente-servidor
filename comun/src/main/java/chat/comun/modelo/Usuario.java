package com.cocochat.comun.modelo;

import java.io.Serializable;

/** Datos públicos de un usuario. Nunca lleva la contraseña ni su hash. */
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String nombreUsuario;
    private final String email;
    private final boolean bloqueado;
    /** Lo llena el servidor con sus sockets activos; no viene de la BD. */
    private boolean conectado;

    public Usuario(int id, String nombreUsuario, String email, boolean bloqueado) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.email = email;
        this.bloqueado = bloqueado;
    }

    public int getId() { return id; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getEmail() { return email; }
    public boolean isBloqueado() { return bloqueado; }
    public boolean isConectado() { return conectado; }
    public void setConectado(boolean conectado) { this.conectado = conectado; }

    @Override
    public String toString() { return nombreUsuario + (conectado ? " (conectado)" : " (desconectado)"); }
}
