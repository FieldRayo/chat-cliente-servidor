-- Dueño: Integrante 02 · Base de datos — Sergio Luis Soto Pulido
-- Esquema SQLite. Las FK llevan ON DELETE CASCADE para el borrado en cascada
-- (BaseDatos activa foreign_keys en cada conexión).
-- Las sentencias se separan con punto y coma: no lo uses dentro de un comentario.

CREATE TABLE IF NOT EXISTS usuarios (
  id_usuario        INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre_usuario    TEXT    NOT NULL UNIQUE,
  email             TEXT    NOT NULL UNIQUE,
  psw               TEXT    NOT NULL,
  intentos_fallidos INTEGER NOT NULL DEFAULT 0,
  bloqueado         INTEGER NOT NULL DEFAULT 0 CHECK (bloqueado IN (0, 1))
);

CREATE TABLE IF NOT EXISTS preguntas_seguridad (
  id_pregunta INTEGER PRIMARY KEY AUTOINCREMENT,
  pregunta    TEXT    NOT NULL UNIQUE
);

-- Las mismas 5 del combo de 05 · Registro, con el id = posición en el combo + 1.
INSERT OR IGNORE INTO preguntas_seguridad (id_pregunta, pregunta) VALUES
  (1, '¿Cuál es el nombre de tu primera mascota?'),
  (2, '¿En qué ciudad naciste?'),
  (3, '¿Cuál es el nombre de tu mejor amigo de la infancia?'),
  (4, '¿Cuál es tu comida favorita?'),
  (5, '¿Cómo se llamaba tu escuela primaria?');

CREATE TABLE IF NOT EXISTS respuestas_seguridad (
  id_usuario     INTEGER NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
  id_pregunta    INTEGER NOT NULL REFERENCES preguntas_seguridad(id_pregunta) ON DELETE RESTRICT,
  respuesta_hash TEXT    NOT NULL,
  PRIMARY KEY (id_usuario, id_pregunta)
);

CREATE TABLE IF NOT EXISTS amistades (
  id_amistad     INTEGER PRIMARY KEY AUTOINCREMENT,
  id_solicitante INTEGER NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
  id_receptor    INTEGER NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
  estado         TEXT    NOT NULL DEFAULT 'pendiente' CHECK (estado IN ('pendiente', 'aceptada')),
  fecha          TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (id_solicitante, id_receptor)
);

CREATE TABLE IF NOT EXISTS mensajes_amigos (
  id_mensaje INTEGER PRIMARY KEY AUTOINCREMENT,
  id_amistad INTEGER NOT NULL REFERENCES amistades(id_amistad) ON DELETE CASCADE,
  id_emisor  INTEGER NOT NULL REFERENCES usuarios(id_usuario)  ON DELETE CASCADE,
  contenido  TEXT    NOT NULL,
  fecha      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_msgam_amistad_fecha ON mensajes_amigos (id_amistad, fecha);

CREATE TABLE IF NOT EXISTS grupos (
  id_grupo       INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre         TEXT    NOT NULL,
  id_dueno       INTEGER NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
  fecha_creacion TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS miembros_grupo (
  id_grupo   INTEGER NOT NULL REFERENCES grupos(id_grupo)     ON DELETE CASCADE,
  id_usuario INTEGER NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
  estado     TEXT    NOT NULL DEFAULT 'invitado' CHECK (estado IN ('invitado', 'aceptado')),
  fecha      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_grupo, id_usuario)
);

CREATE TABLE IF NOT EXISTS mensajes_grupo (
  id_mensaje INTEGER PRIMARY KEY AUTOINCREMENT,
  id_grupo   INTEGER NOT NULL REFERENCES grupos(id_grupo)     ON DELETE CASCADE,
  id_emisor  INTEGER NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
  contenido  TEXT    NOT NULL,
  fecha      TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_msggr_grupo_fecha ON mensajes_grupo (id_grupo, fecha);
