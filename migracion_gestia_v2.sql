-- Script de migración idempotente v2 para MaquetaGestia (Base de datos GESTIA)
-- Crea las tablas ComentarioTarea, EnlaceTarea y Subtarea con sus claves foráneas.

IF OBJECT_ID(N'dbo.ComentarioTarea', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ComentarioTarea (
        id VARCHAR(15) NOT NULL PRIMARY KEY,
        tarea_id VARCHAR(15) NOT NULL,
        usuario_id VARCHAR(15) NOT NULL,
        contenido NVARCHAR(500) NOT NULL,
        fecha_creacion DATETIME NOT NULL CONSTRAINT DF_ComentarioTarea_Fecha DEFAULT GETDATE(),
        CONSTRAINT FK_ComentarioTarea_Tarea FOREIGN KEY (tarea_id) REFERENCES dbo.Tarea(id),
        CONSTRAINT FK_ComentarioTarea_Usuario FOREIGN KEY (usuario_id) REFERENCES dbo.Usuario(id)
    );
END;
GO

IF OBJECT_ID(N'dbo.EnlaceTarea', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.EnlaceTarea (
        id VARCHAR(15) NOT NULL PRIMARY KEY,
        tarea_id VARCHAR(15) NOT NULL,
        usuario_id VARCHAR(15) NOT NULL,
        titulo NVARCHAR(120) NOT NULL,
        url NVARCHAR(500) NOT NULL,
        fecha_creacion DATETIME NOT NULL CONSTRAINT DF_EnlaceTarea_Fecha DEFAULT GETDATE(),
        CONSTRAINT FK_EnlaceTarea_Tarea FOREIGN KEY (tarea_id) REFERENCES dbo.Tarea(id),
        CONSTRAINT FK_EnlaceTarea_Usuario FOREIGN KEY (usuario_id) REFERENCES dbo.Usuario(id)
    );
END;
GO

IF OBJECT_ID(N'dbo.Subtarea', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Subtarea (
        id VARCHAR(15) NOT NULL PRIMARY KEY,
        tarea_id VARCHAR(15) NOT NULL,
        titulo NVARCHAR(200) NOT NULL,
        completada BIT NOT NULL CONSTRAINT DF_Subtarea_Completada DEFAULT 0,
        orden INT NOT NULL CONSTRAINT DF_Subtarea_Orden DEFAULT 1,
        CONSTRAINT FK_Subtarea_Tarea FOREIGN KEY (tarea_id) REFERENCES dbo.Tarea(id)
    );
END;
GO
