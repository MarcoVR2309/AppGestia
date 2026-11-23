package com.example.maquetagestia.controller;

import android.util.Log;

import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.model.Fase;
import com.example.maquetagestia.model.Proyecto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class cProyecto {

    private final conSQLServer conexionHelper = new conSQLServer();

    // Generador de código primario correlativo con prefijo (Ej: PRY000001, PU000001, FAS000001)
    public String generarIdSeguro(Connection con, String tabla, String columnaPk, String prefijo, int digitosLongitud) {
        String nuevoCodigo = prefijo + String.format(Locale.US, "%0" + digitosLongitud + "d", 1L);
        int longitudTotal = prefijo.length() + digitosLongitud;

        String sql = "SELECT TOP 1 " + columnaPk + " FROM " + tabla +
                " WHERE LEN(" + columnaPk + ") = ? AND " + columnaPk + " LIKE ? " +
                " ORDER BY " + columnaPk + " DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, longitudTotal);
            ps.setString(2, prefijo + "%");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String ultimoId = rs.getString(1);
                    if (ultimoId != null && ultimoId.length() >= longitudTotal) {
                        try {
                            long correlativo = Long.parseLong(ultimoId.substring(prefijo.length())) + 1;
                            nuevoCodigo = prefijo + String.format(Locale.US, "%0" + digitosLongitud + "d", correlativo);
                        } catch (NumberFormatException e) {
                            nuevoCodigo = prefijo + String.format(Locale.US, "%0" + digitosLongitud + "d", 1L);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_PROYECTO", "Error generando ID correlativo para " + tabla + ": " + e.getMessage());
            nuevoCodigo = prefijo + String.format(Locale.US, "%0" + digitosLongitud + "d", 1L);
        }

        return nuevoCodigo;
    }

    // Transacción Atómica: Crear Proyecto (PRY) y Asignar Líder en Proyecto_Usuario (PU)
    public boolean crearProyecto(Proyecto p, String usuarioIdLider) {
        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try {
            con.setAutoCommit(false); // Iniciar Transacción Atómica

            // 1. Insertar Proyecto con ID prefijo PRY000001
            String idPry = generarIdSeguro(con, "Proyecto", "id", "PRY", 6);
            p.setId(idPry);

            String sqlPry = "INSERT INTO Proyecto (id, nombre, descripcion, tipo, estado, fecha_inicio, fecha_fin_estimada, creado_por) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psPry = con.prepareStatement(sqlPry)) {
                psPry.setString(1, p.getId());
                psPry.setString(2, p.getNombre());
                psPry.setString(3, p.getDescripcion());
                psPry.setString(4, p.getTipo());
                psPry.setString(5, p.getEstado() != null ? p.getEstado() : "Planificacion");
                psPry.setString(6, p.getFechaInicio());
                psPry.setString(7, p.getFechaFinEstimada());
                psPry.setString(8, usuarioIdLider);
                psPry.executeUpdate();
            }

            // 2. Insertar asignación del líder en Proyecto_Usuario con ID prefijo PU000001
            String idPu = generarIdSeguro(con, "Proyecto_Usuario", "id", "PU", 6);
            String sqlPu = "INSERT INTO Proyecto_Usuario (id, proyecto_id, usuario_id, rol) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psPu = con.prepareStatement(sqlPu)) {
                psPu.setString(1, idPu);
                psPu.setString(2, p.getId());
                psPu.setString(3, usuarioIdLider);
                psPu.setString(4, "Lider");
                psPu.executeUpdate();
            }

            con.commit(); // Confirmar la transacción atómica
            con.close();

            Fase faseInicial = new Fase(idPry, "Borrador", 1, null, "EnProgreso");
            new cFase().crearFase(faseInicial);
            return true;
        } catch (Exception e) {
            Log.e("CRUD_PROYECTO", "Error en crearProyecto: " + e.getMessage());
            try {
                if (con != null && !con.isClosed()) {
                    con.rollback(); // Rollback en caso de error
                    con.close();
                }
            } catch (Exception rollbackEx) {
                Log.e("CRUD_PROYECTO", "Error en Rollback: " + rollbackEx.getMessage());
            }
            return false;
        }
    }

    // Listar proyectos asociados a un usuario con filtro por tipo y progreso real de fases
    public List<Proyecto> listarPorUsuario(String usuarioId, String filtroTipo) {
        List<Proyecto> lista = new ArrayList<>();
        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT p.id, p.nombre, p.descripcion, p.tipo, p.estado, p.fecha_inicio, p.fecha_fin_estimada, p.creado_por, p.created_at, " +
                     "ISNULL((SELECT CASE WHEN COUNT(*) = 0 THEN 0 " +
                     "ELSE (SUM(CASE WHEN f.estado = 'Completada' THEN 1 ELSE 0 END) * 100) / COUNT(*) END " +
                     "FROM Fase f WHERE f.proyecto_id = p.id), 0) AS progreso_porcentaje " +
                     "FROM Proyecto p " +
                     "INNER JOIN Proyecto_Usuario pu ON p.id = pu.proyecto_id " +
                     "WHERE pu.usuario_id = ?";

        if (filtroTipo != null && !filtroTipo.isEmpty() && !filtroTipo.equalsIgnoreCase("Todos")) {
            sql += " AND p.tipo = ?";
        }
        sql += " ORDER BY p.created_at DESC";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, usuarioId);
            if (filtroTipo != null && !filtroTipo.isEmpty() && !filtroTipo.equalsIgnoreCase("Todos")) {
                ps.setString(2, filtroTipo);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Proyecto p = new Proyecto();
                    p.setId(rs.getString("id"));
                    p.setNombre(rs.getString("nombre"));
                    p.setDescripcion(rs.getString("descripcion"));
                    p.setTipo(rs.getString("tipo"));
                    p.setEstado(rs.getString("estado"));
                    p.setFechaInicio(rs.getString("fecha_inicio"));
                    p.setFechaFinEstimada(rs.getString("fecha_fin_estimada"));
                    p.setCreadoPor(rs.getString("creado_por"));
                    p.setCreatedAt(rs.getString("created_at"));
                    p.setProgresoPorcentaje(rs.getInt("progreso_porcentaje"));
                    lista.add(p);
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_PROYECTO", "Error al listar proyectos por usuario: " + e.getMessage());
        }
        return lista;
    }

    // Obtener detalles de un proyecto por su ID
    public Proyecto obtenerPorId(String proyectoId) {
        Proyecto p = null;
        Connection con = conexionHelper.conectar();
        if (con == null) return null;

        String sql = "SELECT id, nombre, descripcion, tipo, estado, fecha_inicio, fecha_fin_estimada, creado_por FROM Proyecto WHERE id = ?";
        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, proyectoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p = new Proyecto();
                    p.setId(rs.getString("id"));
                    p.setNombre(rs.getString("nombre"));
                    p.setDescripcion(rs.getString("descripcion"));
                    p.setTipo(rs.getString("tipo"));
                    p.setEstado(rs.getString("estado"));
                    p.setFechaInicio(rs.getString("fecha_inicio"));
                    p.setFechaFinEstimada(rs.getString("fecha_fin_estimada"));
                    p.setCreadoPor(rs.getString("creado_por"));
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_PROYECTO", "Error al obtener proyecto por ID: " + e.getMessage());
        }
        return p;
    }

    // Clonar estructura administrativa de un proyecto (Proyecto + Líder en PU + Fases en orden)
    public String clonarProyectoComoPlantilla(String proyectoIdOrigen, String nuevoNombre, String usuarioIdLider) {
        if (proyectoIdOrigen == null || proyectoIdOrigen.trim().isEmpty()
                || nuevoNombre == null || nuevoNombre.trim().isEmpty()
                || usuarioIdLider == null || usuarioIdLider.trim().isEmpty()) {
            return null;
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return null;

        try {
            if (!new cEquipo().esLiderDelProyecto(con, proyectoIdOrigen.trim(), usuarioIdLider.trim())) {
                con.close();
                return null;
            }

            // 1. Obtener plantilla origen
            String descripcionOrigen = "";
            String tipoOrigen = "Academico";
            String fechaInicioOrigen = null;
            String fechaFinOrigen = null;

            String sqlOrigen = "SELECT descripcion, tipo, fecha_inicio, fecha_fin_estimada FROM Proyecto WHERE id = ?";
            try (PreparedStatement psOrg = con.prepareStatement(sqlOrigen)) {
                psOrg.setString(1, proyectoIdOrigen.trim());
                try (ResultSet rs = psOrg.executeQuery()) {
                    if (!rs.next()) {
                        con.close();
                        return null;
                    }
                    descripcionOrigen = rs.getString("descripcion");
                    tipoOrigen = rs.getString("tipo");
                    fechaInicioOrigen = rs.getString("fecha_inicio");
                    fechaFinOrigen = rs.getString("fecha_fin_estimada");
                }
            }

            // 2. Leer únicamente las fases en orden
            List<Fase> fasesOrigen = new ArrayList<>();
            String sqlFases = "SELECT nombre, orden FROM Fase WHERE proyecto_id = ? ORDER BY orden ASC";
            try (PreparedStatement psF = con.prepareStatement(sqlFases)) {
                psF.setString(1, proyectoIdOrigen.trim());
                try (ResultSet rsF = psF.executeQuery()) {
                    while (rsF.next()) {
                        Fase f = new Fase();
                        f.setNombre(rsF.getString("nombre"));
                        f.setOrden(rsF.getInt("orden"));
                        fasesOrigen.add(f);
                    }
                }
            }

            con.setAutoCommit(false);

            String idNuevoPry = generarIdSeguro(con, "Proyecto", "id", "PRY", 6);
            String sqlInsPry = "INSERT INTO Proyecto (id, nombre, descripcion, tipo, estado, fecha_inicio, fecha_fin_estimada, creado_por) " +
                               "VALUES (?, ?, ?, ?, 'Planificacion', ?, ?, ?)";
            try (PreparedStatement psIns = con.prepareStatement(sqlInsPry)) {
                psIns.setString(1, idNuevoPry);
                psIns.setString(2, nuevoNombre.trim());
                psIns.setString(3, descripcionOrigen != null ? descripcionOrigen : "");
                psIns.setString(4, tipoOrigen != null ? tipoOrigen : "Academico");
                psIns.setString(5, fechaInicioOrigen);
                psIns.setString(6, fechaFinOrigen);
                psIns.setString(7, usuarioIdLider.trim());
                psIns.executeUpdate();
            }

            String idNuevoPu = generarIdSeguro(con, "Proyecto_Usuario", "id", "PU", 6);
            String sqlInsPu = "INSERT INTO Proyecto_Usuario (id, proyecto_id, usuario_id, rol) VALUES (?, ?, ?, 'Lider')";
            try (PreparedStatement psPu = con.prepareStatement(sqlInsPu)) {
                psPu.setString(1, idNuevoPu);
                psPu.setString(2, idNuevoPry);
                psPu.setString(3, usuarioIdLider.trim());
                psPu.executeUpdate();
            }

            if (!fasesOrigen.isEmpty()) {
                String primerIdFas = generarIdSeguro(con, "Fase", "id", "FAS", 6);
                long correlativoBase = 1L;
                try {
                    correlativoBase = Long.parseLong(primerIdFas.substring(3));
                } catch (Exception ignored) {
                }

                String sqlInsFase = "INSERT INTO Fase (id, proyecto_id, nombre, orden, fecha_estimada, estado) VALUES (?, ?, ?, ?, NULL, ?)";
                try (PreparedStatement psFase = con.prepareStatement(sqlInsFase)) {
                    for (int i = 0; i < fasesOrigen.size(); i++) {
                        Fase f = fasesOrigen.get(i);
                        String idFas = "FAS" + String.format(Locale.US, "%06d", correlativoBase + i);
                        int orden = f.getOrden() > 0 ? f.getOrden() : (i + 1);
                        String estadoFase = (i == 0) ? "EnProgreso" : "Pendiente";

                        psFase.setString(1, idFas);
                        psFase.setString(2, idNuevoPry);
                        psFase.setString(3, f.getNombre());
                        psFase.setInt(4, orden);
                        psFase.setString(5, estadoFase);
                        psFase.executeUpdate();
                    }
                }
            }

            con.commit();
            con.close();
            return idNuevoPry;
        } catch (Exception e) {
            Log.e("CRUD_PROYECTO", "Error al clonar proyecto: " + e.getMessage());
            try {
                if (con != null && !con.isClosed()) {
                    con.rollback();
                    con.close();
                }
            } catch (Exception rollbackEx) {
                Log.e("CRUD_PROYECTO", "Error en Rollback clonarProyecto: " + rollbackEx.getMessage());
            }
            return null;
        }
    }

    // Eliminar proyecto completo validando rol de Líder en Proyecto_Usuario
    public boolean eliminarProyecto(String proyectoId, String usuarioIdSolicitante) {
        if (proyectoId == null || proyectoId.trim().isEmpty()
                || usuarioIdSolicitante == null || usuarioIdSolicitante.trim().isEmpty()) {
            return false;
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try {
            if (!new cEquipo().esLiderDelProyecto(con, proyectoId.trim(), usuarioIdSolicitante.trim())) {
                con.close();
                return false;
            }

            con.setAutoCommit(false);

            String[] sqlHijasTareas = {
                    "DELETE c FROM ComentarioTarea c INNER JOIN Tarea t ON c.tarea_id = t.id WHERE t.proyecto_id = ?",
                    "DELETE e FROM EnlaceTarea e INNER JOIN Tarea t ON e.tarea_id = t.id WHERE t.proyecto_id = ?",
                    "DELETE s FROM Subtarea s INNER JOIN Tarea t ON s.tarea_id = t.id WHERE t.proyecto_id = ?",
                    "DELETE tr FROM Tarea_Responsable tr INNER JOIN Tarea t ON tr.tarea_id = t.id WHERE t.proyecto_id = ?"
            };
            for (String sqlHija : sqlHijasTareas) {
                try (PreparedStatement ps = con.prepareStatement(sqlHija)) {
                    ps.setString(1, proyectoId.trim());
                    ps.executeUpdate();
                }
            }

            String[] sqlProyectoHijas = {
                    "DELETE FROM Tarea WHERE proyecto_id = ?",
                    "DELETE FROM Fase WHERE proyecto_id = ?",
                    "DELETE FROM Proyecto_Usuario WHERE proyecto_id = ?",
                    "DELETE FROM Proyecto WHERE id = ?"
            };
            int eliminados = 0;
            for (int i = 0; i < sqlProyectoHijas.length; i++) {
                try (PreparedStatement ps = con.prepareStatement(sqlProyectoHijas[i])) {
                    ps.setString(1, proyectoId.trim());
                    int res = ps.executeUpdate();
                    if (i == sqlProyectoHijas.length - 1) {
                        eliminados = res;
                    }
                }
            }

            if (eliminados > 0) {
                con.commit();
                con.close();
                return true;
            } else {
                con.rollback();
                con.close();
                return false;
            }
        } catch (Exception e) {
            Log.e("CRUD_PROYECTO", "Error al eliminar proyecto: " + e.getMessage());
            try {
                if (con != null && !con.isClosed()) {
                    con.rollback();
                    con.close();
                }
            } catch (Exception rollbackEx) {
                Log.e("CRUD_PROYECTO", "Error en Rollback eliminarProyecto: " + rollbackEx.getMessage());
            }
            return false;
        }
    }
}
