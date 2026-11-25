# AGENTS.md: cómo trabajar en este proyecto

Chat cliente-servidor en Java 17 (Swing + Sockets TCP + SQLite) que hacen 11 integrantes, cada uno dueño de un área.
El `README.md` es la guía completa. Este archivo resume las reglas que **siempre** debes seguir.

## ⛔ Paso 0, obligatorio: ¿quién eres y cuál es tu área?

**Antes de hacer cualquier cosa** (leer código para cambiarlo, escribir, compilar, hacer commits) pregúntale a la persona:

1. **Su nombre completo.**
2. **Su número y nombre de área** (por ejemplo `05 · Registro`). La lista está en la tabla "Qué carpetas son de quién" del README.

No supongas ninguno de los dos datos: no los deduzcas de `git config`, del nombre de la rama ni de commits anteriores. Si la persona no responde, **no avances**.

Con la respuesta, revisa que su nombre esté en estos lugares y agrégalo donde falte (en su rama, como primer commit):

| Dónde | Cómo debe quedar |
|---|---|
| Su encabezado en `README.md`, sección Tareas | `### 05 · Registro — Juan Pérez` (ver abajo). |
| El `package-info.java` de **cada una** de sus carpetas | `<b>Dueño: Integrante 05 · Registro — Juan Pérez</b> (servidor)` |
| `git config user.name` / `user.email` | Su nombre y el correo de su GitHub. Si no coinciden, díselo; sus commits cuentan en la coevaluación. |
| `git config core.hooksPath` | Debe ser `.githooks` (activa el aviso al hacer push). Si no lo es, córrelo: `git config core.hooksPath .githooks` |

Si el área ya tiene **otro** nombre, detente y avisa: puede que la persona se equivocó de número.

### ⚠️ La tabla de progreso NO se edita a mano

Todo lo que está entre `<!-- PROGRESO:INICIO -->` y `<!-- PROGRESO:FIN -->` lo genera `scripts/progreso.py`
a partir de los encabezados `### NN · Área — Nombre` y de las casillas `[x]`. Lo que escribas a mano ahí
**se borra** la próxima vez que corra el script, y copiar las barras desde otro lado las daña.

```diff
- | 02 · Base de datos | Sergio Luis Soto Pulido | ...      ❌ en la tabla: se pierde
+ ### 02 · Base de datos — Sergio Luis Soto Pulido         ✅ en el encabezado de Tareas
```

Después de cambiar un encabezado o una casilla, corre `python3 scripts/progreso.py` y sube el README que genere.

## 🔄 Siempre al día con GitHub

Tus compañeros suben cambios todo el tiempo. Trabajar sobre una copia vieja provoca conflictos y PRs con archivos ajenos.

- **Al empezar cada sesión**, antes de tocar nada, trae lo último y mételo en tu rama:
  ```bash
  git checkout main && git pull
  git checkout <su-rama> && git merge main
  ```
- **Al terminar**, sube lo que hiciste para no perderlo y para que los demás lo vean: `git push`.
- Si hace rato que abriste tu PR, repite el `git merge main` y vuelve a hacer `git push` antes de pedir la revisión.
- Hay dos avisos automáticos. **Ninguno bloquea**, solo advierten:
  - Al hacer `git push`, si tu rama está atrasada respecto a `main` sale un ⚠️ en la terminal (el push se hace igual).
  - En GitHub, el bot comenta en el PR cuando a su rama le falta algo de `main`.

## 🌿 Ramas: nunca en `main`

- `main` está **protegida**: solo entra por Pull Request con la aprobación de **01 · Arquitectura**. Un `git push` a `main` va a fallar.
- Rama de trabajo: `<número>-<tema>` (por ejemplo `05-registro`). Antes de empezar:
  ```bash
  git checkout main && git pull
  git checkout -b 05-registro    # o: git checkout 05-registro && git merge main
  ```
- Haz commits pequeños con mensajes claros (`Registro: validar que el usuario no exista`).
- El PR va de su rama a `main`, con título `[05] Registro: ...` y con *Reviewer* al responsable de 01. En **el mismo PR** se marcan con `[x]` las tareas terminadas en el README.
- Si ya hay cambios hechos en `main` sin commit: `git checkout -b <su-rama>` y se los lleva.
- **Para traer lo nuevo de `main` usa `git merge origin/main`.** Nunca copies ni pegues archivos de `main`
  o de otra rama en la tuya: git los toma como cambios tuyos, aparecen en tu PR y el historial dice que
  tú los escribiste.
- **Antes de abrir el PR** revisa qué lleva:
  ```bash
  git fetch origin && git diff --stat origin/main...HEAD
  ```
  Solo deben salir **tus carpetas** y tus líneas del README (tu encabezado, tus casillas y la tabla que generó el script).
  Si sale algo más, no abras el PR: arréglalo o pregunta.

## 📁 Solo sus carpetas

Cada integrante modifica **solo** sus carpetas (tabla "Qué carpetas son de quién" del README). Si necesita algo de otra área (un `Tipo` nuevo, una tabla, un método de un DAO), **no lo edites**: redacta el pedido para que lo mande al dueño de esa área.

El cliente nunca usa código del servidor ni la BD: todo lo pide por el protocolo.

## 🔌 Contrato técnico (lo hizo 01 · Arquitectura)

**Protocolo** (`comun/…/protocolo/`):
- Lo único que viaja por la red es un `Paquete(tipo, de, para, datos)`. No inventes otro formato.
- `Tipo` ya trae todos los tipos de mensaje. Uno nuevo se le pide a 01.
- `datos` debe ser `Serializable`: `String`, modelos de `comun/…/modelo/` o `ArrayList`. Una variable de tipo `List` no se acepta.
- El servidor contesta con `Paquete.ok(peticion, datos)` o `Paquete.error(peticion, "texto para el usuario")`. Esa respuesta lleva `respuestaA` = el tipo de la petición.

**Ventanas del cliente** (de 05 a 11):
- Son un `JFrame` con constructor **`public NombreVentana(Conexion conexion)`**, en el paquete y con el nombre que espera `VentanaPrincipal` (`chat.cliente.usuarios.VentanaUsuarios`, `chat.cliente.chatamigos.VentanaAmigos`, etc.). Si no coincide, el menú dice "todavía no está lista".
- El menú crea cada ventana **una vez al iniciar sesión, oculta**, y su X solo la esconde (cerrar una ventana no es cerrar la sesión). Suscríbete en el constructor y vas a recibir tus paquetes aunque la ventana no esté abierta:
  ```java
  conexion.alRecibir(Tipo.LOGIN, p -> {       // recibe LOGIN y también el OK/ERROR que lo contesta
      if (p.esOk()) { ... } else mostrarError((String) p.getDatos());
  });
  conexion.enviar(new Paquete(Tipo.LOGIN, usuario, null, contrasena));
  ```
- Los avisos de `alRecibir` llegan en el hilo de Swing: se puede tocar la ventana directamente.
- El usuario con sesión iniciada está en `conexion.getUsuario()`.
- **06 · Login**, al recibir OK: `conexion.setUsuario(u); new VentanaPrincipal(conexion).setVisible(true); dispose();`
- **07 · Cuenta**, al recibir OK de `ELIMINAR_CUENTA`: `VentanaPrincipal.volverAlLogin(conexion);`
- Solo el botón *Cerrar sesión* del menú manda `LOGOUT`.

**Servidor**:
- Base de datos solo a través de los DAOs de 02, con `PreparedStatement`. No escribas SQL en ningún otro lado.
- 03 · Servidor: crea primero el `ObjectOutputStream`, llama `flush()` y después crea el `ObjectInputStream`. Llama `reset()` después de cada `writeObject`. Escucha en `Paquete.PUERTO`. Con `LOGOUT` **no** cierres el socket (el cliente vuelve al login con la misma conexión). Después de un `ELIMINAR_CUENTA` exitoso, da la sesión por terminada.

## ✅ Antes de proponer un commit o un PR

- `mvn package` (o *Clean and Build* en el IDE) debe compilar: `main` es lo que se entrega.
- Si tu parte depende de otra que no está lista, usa datos falsos y avisa; no edites la carpeta ajena.
- La documentación (caso de uso, diagrama de secuencia, clases, conclusión) va en los Google Docs enlazados en el README, no en el repo.
