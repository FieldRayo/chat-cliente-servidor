package chat.comun.protocolo;

/**
 * Qué clase de mensaje lleva un {@link Paquete}. Están TODOS desde el día 1:
 * si necesitan uno nuevo, pídeselo a 01 · Arquitectura.
 */
public enum Tipo {
    // Acceso
    REGISTRO, LOGIN, LOGOUT, RECUPERAR_PREGUNTA, RECUPERAR, ELIMINAR_CUENTA,
    // Usuarios (ventana 1)
    LISTA_USUARIOS, USUARIO_CONECTADO, USUARIO_DESCONECTADO, MSG_TEMPORAL,
    // Amigos (ventana 2)
    SOLICITUD_AMISTAD, RESPUESTA_AMISTAD, ELIMINAR_AMIGO, LISTA_AMIGOS, MSG_AMIGO, HISTORIAL_AMIGO,
    // Grupos (ventana 3)
    GRUPO_CREAR, GRUPO_INVITAR, GRUPO_RESPUESTA, GRUPO_MSG, GRUPO_HISTORIAL,
    GRUPO_SALIR, GRUPO_QUITAR, GRUPO_RENOMBRAR, GRUPO_ELIMINAR,
    // Generales
    NOTIFICACION, OK, ERROR
}
