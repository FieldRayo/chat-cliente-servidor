# División de tareas: Chat Cliente-Servidor (11 integrantes)

**Stack:** Java 17 + Swing + Sockets TCP + SQLite (JDBC).
SQLite va dentro del `.jar` del servidor, así que el usuario final no instala ningún motor de BD.
Entregables: `Servidor.jar` y `Cliente.jar`.

> Reglas para todos
> - Nadie inventa mensajes de red: todos usan el **protocolo** del integrante 1.
> - Nadie toca la BD con SQL suelto: todos usan los **DAO** del integrante 2.
> - Cada quien trabaja en su rama `feature/<su-parte>` y abre un PR a `main`.
> - Cada quien documenta **su parte** en el Google Doc (ver sección al final).

---

## Asignación

| # | Integrante | Parte del programa | Caso de uso + diagrama de secuencia |
|---|---|---|---|
| 1 | _______ | **Arquitectura e integración:** protocolo de mensajes (`Paquete`, tipos de mensaje), clase de conexión del cliente, estructura del proyecto, generar los `.jar` | Conectar cliente al servidor |
| 2 | _______ | **Base de datos:** esquema SQLite, DAOs, borrado en cascada (FK `ON DELETE CASCADE`) al eliminar usuario | Eliminar cuenta (cascada) |
| 3 | _______ | **Núcleo del servidor:** `ServerSocket`, pool de hilos (para no saturar), enrutamiento de mensajes, difusión de conectado/desconectado | Enrutar mensaje de C1 a C3 |
| 4 | _______ | **Ventana del servidor:** bitácora en tiempo real de todo lo que pasa, historial y consulta de toda la información | Consultar bitácora del servidor |
| 5 | _______ | **Registro:** formulario GUI, validaciones, alta en el servidor y pregunta de seguridad para recuperar la cuenta después | Registrar usuario |
| 6 | _______ | **Login:** ventana de acceso, validación de credenciales, bloqueo tras 3 intentos fallidos o envío al registro, sesión única | Iniciar sesión |
| 7 | _______ | **Recuperar acceso y eliminar cuenta (lado cliente):** recuperación con la pregunta de seguridad, desbloqueo, botón "eliminar mi cuenta" | Recuperar acceso |
| 8 | _______ | **Ventana 1, Usuarios:** lista de todos, separada en conectados y desconectados en tiempo real; chat temporal solo con conectados (se borra al cerrar sesión); diferencia entre cerrar ventana y cerrar sesión | Enviar mensaje a usuario conectado |
| 9 | _______ | **Amistades y notificaciones:** enviar solicitud desde la ventana 1, aceptar, rechazar o eliminar amigos; **ventana de notificaciones** | Enviar y aceptar solicitud de amistad |
| 10 | _______ | **Ventana 2, Chat de amigos:** historial que se conserva, mensajes a amigos desconectados (se entregan al conectarse), al eliminar al amigo se borra la conversación, chat separado del de la ventana 1 | Enviar mensaje a amigo desconectado |
| 11 | _______ | **Ventana 3, Grupos:** crear grupo (mínimo 3 contando invitaciones pendientes), invitar amigos por el chat de amigos, dueño (agregar, quitar, renombrar, eliminar), salir, autoeliminar si quedan menos de 3, historial completo para los nuevos | Crear grupo e invitar amigos |

### Documentación compartida (además de la parte de cada uno)

| Sección | Responsable |
|---|---|
| Portada, índice y armado final del PDF (ambas partes) | 1 |
| Problemática (desarrollada, no 3 renglones) | 1 redacta, 3 y 4 aportan la parte del servidor |
| Diagrama de clases completo con relaciones | 1 lo integra, cada uno entrega sus clases |
| Diagrama de la base de datos | 2 |
| Parte 2: Descripción del sistema como producto | 1 |
| Parte 2: Requisitos mínimos y Proceso de instalación | 1 y 2 |
| Parte 2: ¿Cómo se usa? | Cada uno describe **su ventana** con capturas: 4 servidor, 5 registro, 6 login, 7 recuperación, 8 usuarios, 9 amigos y notificaciones, 10 chat de amigos, 11 grupos |
| Conclusión | Cada uno la suya |

---

## Orden de trabajo (dependencias)

1. **Semana 1:** 1 publica el protocolo y 2 publica el esquema y los DAOs. **Todos los demás dependen de esto.**
2. **Semana 1-2:** 3 deja corriendo el servidor. 5, 6 y 7 hacen el acceso.
3. **Semana 2-3:** 8, 9, 10 y 11 hacen las ventanas. 4 hace la ventana del servidor.
4. **Semana final:** 1 integra y genera los `.jar`. Prueba en **dos computadoras distintas** de la misma red, que es lo mínimo para calificar.

---

## Plantilla para el Google Doc

```
PARTE 1: PUNTO DE VISTA DEL DESARROLLADOR
  Portada
  Índice
  1. Problemática
  2. Casos de uso
     2.1 ... 2.11  (uno por integrante, con su nombre)
  3. Diagramas de secuencia
     3.1 ... 3.11  (uno por integrante, una acción cada uno)
  4. Diagrama de clases
  5. Diagrama de la base de datos
  6. Conclusiones
     6.1 ... 6.11  (una por integrante)

PARTE 2: PUNTO DE VISTA DEL CLIENTE
  Portada
  Índice
  1. Descripción del sistema
  2. Requisitos mínimos
  3. Proceso de instalación
  4. ¿Cómo se usa?
     4.1 Servidor · 4.2 Registro · 4.3 Inicio de sesión · 4.4 Recuperar acceso
     4.5 Usuarios · 4.6 Amigos y notificaciones · 4.7 Chat de amigos · 4.8 Grupos
```

Formato del caso de uso de cada integrante: **Nombre, Actor, Precondición, Flujo principal, Flujos alternos, Postcondición** y el diagrama.
