-- Dueño: Integrante 02 · Base de datos
-- Esquema SQLite. Usar FOREIGN KEY ... ON DELETE CASCADE para el borrado en cascada.
-- Recuerda: SQLite requiere `PRAGMA foreign_keys = ON;` en cada conexión.
CREATE TABLE IF NOT EXISTS `usuarios` (
  `id_usuario`        INT(11)      NOT NULL AUTO_INCREMENT,
  `nombre_usuario`    VARCHAR(50)  NOT NULL,
  `email`             VARCHAR(50)  NOT NULL,
  `psw`               VARCHAR(255) NOT NULL,
  `intentos_fallidos` INT(11)      NOT NULL DEFAULT 0,
  `bloqueado`         TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uq_nombre_usuario` (`nombre_usuario`),
  UNIQUE KEY `uq_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `preguntas_seguridad` (
  `id_pregunta` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `pregunta`    VARCHAR(200)     NOT NULL,
  PRIMARY KEY (`id_pregunta`),
  UNIQUE KEY `pregunta` (`pregunta`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `respuestas_seguridad` (
  `id_usuario`     INT(11)          NOT NULL,
  `id_pregunta`    INT(10) UNSIGNED NOT NULL,
  `respuesta_hash` VARCHAR(255)     NOT NULL,
  PRIMARY KEY (`id_usuario`, `id_pregunta`),
  CONSTRAINT `fk_resp_usuario`
    FOREIGN KEY (`id_usuario`)  REFERENCES `usuarios`(`id_usuario`) ON DELETE CASCADE,
  CONSTRAINT `fk_resp_pregunta`
    FOREIGN KEY (`id_pregunta`) REFERENCES `preguntas_seguridad`(`id_pregunta`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `amistades` (
  `id_amistad`     INT(11) NOT NULL AUTO_INCREMENT,
  `id_solicitante` INT(11) NOT NULL,
  `id_receptor`    INT(11) NOT NULL,
  `estado`         ENUM('pendiente','aceptada') NOT NULL DEFAULT 'pendiente',
  `fecha`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_amistad`),
  UNIQUE KEY `uq_amistad` (`id_solicitante`, `id_receptor`),
  CONSTRAINT `fk_amist_solicitante`
    FOREIGN KEY (`id_solicitante`) REFERENCES `usuarios`(`id_usuario`) ON DELETE CASCADE,
  CONSTRAINT `fk_amist_receptor`
    FOREIGN KEY (`id_receptor`)    REFERENCES `usuarios`(`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `mensajes_amigos` (
  `id_mensaje` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `id_amistad` INT(11)    NOT NULL,
  `id_emisor`  INT(11)    NOT NULL,
  `contenido`  TEXT       NOT NULL,
  `fecha`      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_mensaje`),
  KEY `idx_msgam_amistad_fecha` (`id_amistad`, `fecha`),
  CONSTRAINT `fk_msgam_amistad`
    FOREIGN KEY (`id_amistad`) REFERENCES `amistades`(`id_amistad`) ON DELETE CASCADE,
  CONSTRAINT `fk_msgam_emisor`
    FOREIGN KEY (`id_emisor`)  REFERENCES `usuarios`(`id_usuario`)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `grupos` (
  `id_grupo`       INT(11)      NOT NULL AUTO_INCREMENT,
  `nombre`         VARCHAR(100) NOT NULL,
  `id_dueno`       INT(11)      NOT NULL,
  `fecha_creacion` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_grupo`),
  CONSTRAINT `fk_grupo_dueno`
    FOREIGN KEY (`id_dueno`) REFERENCES `usuarios`(`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `miembros_grupo` (
  `id_grupo`   INT(11) NOT NULL,
  `id_usuario` INT(11) NOT NULL,
  `estado`     ENUM('invitado','aceptado') NOT NULL DEFAULT 'invitado',
  `fecha`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_grupo`, `id_usuario`),
  CONSTRAINT `fk_miem_grupo`
    FOREIGN KEY (`id_grupo`)   REFERENCES `grupos`(`id_grupo`)     ON DELETE CASCADE,
  CONSTRAINT `fk_miem_usuario`
    FOREIGN KEY (`id_usuario`) REFERENCES `usuarios`(`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `mensajes_grupo` (
  `id_mensaje` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `id_grupo`   INT(11)    NOT NULL,
  `id_emisor`  INT(11)    NOT NULL,
  `contenido`  TEXT       NOT NULL,
  `fecha`      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_mensaje`),
  KEY `idx_msggr_grupo_fecha` (`id_grupo`, `fecha`),
  CONSTRAINT `fk_msggr_grupo`
    FOREIGN KEY (`id_grupo`)  REFERENCES `grupos`(`id_grupo`)     ON DELETE CASCADE,
  CONSTRAINT `fk_msggr_emisor`
    FOREIGN KEY (`id_emisor`) REFERENCES `usuarios`(`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
