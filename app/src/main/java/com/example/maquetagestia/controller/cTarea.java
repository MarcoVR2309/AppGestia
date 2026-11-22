package com.example.maquetagestia.controller;

import android.util.Log;

import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.model.Notificacion;
import com.example.maquetagestia.model.Tarea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class cTarea {

    private final conSQLServer conexionHelper = new conSQLServer();

    // Generador de código primario correlativo con prefijo TAR000001
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
            Log.e("CRUD_TAREA", "Error generando ID correlativo para " + tabla + ": " + e.getMessage());
            nuevoCodigo = prefijo + String.format(Locale.US, "%0" + digitosLongitud + "d", 1L);
        }

        return nuevoCodigo;
    }

    private String extraerFechaCorta(String fecha) {
        if (fecha == null) return null;
        String f = fecha.trim();
        if (f.isEmpty()) return null;
        if (f.length() >= 10 && f.charAt(4) == '-' && f.charAt(7) == '-') {
            return f.substring(0, 10);
        }
        return f;
    }

    // Auxiliar para verificar formato de fecha válido (yyyy-MM-dd o yyyy-MM-dd HH:mm:ss)
    private boolean esFechaValida(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return false;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            sdf.parse(fecha.trim());
            return true;
        } catch (Exception e1) {
            try {
                java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                sdf2.setLenient(false);
                sdf2.parse(fecha.trim());
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    // Inserción de nueva tarea con validación de fecha y asignación en Tarea_Responsable (Prefijo RES000001)
    public boolean crearTarea(Tarea t, String usuarioIdAsignado) {
        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            String idTar = generarIdSeguro(autoCloseCon, "Tarea", "id", "TAR", 6);
            t.setId(idTar);

            String sql = "INSERT INTO Tarea (id, proyecto_id, fase_id, titulo, instrucciones, prioridad, estado, fecha_limite) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
                ps.setString(1, t.getId());
                ps.setString(2, t.getProyectoId());

                if (t.getFaseId() != null && !t.getFaseId().trim().isEmpty()) {
                    ps.setString(3, t.getFaseId());
                } else {
                    ps.setNull(3, java.sql.Types.VARCHAR);
                }

                ps.setString(4, t.getTitulo());
                ps.setString(5, t.getInstrucciones());
                ps.setString(6, t.getPrioridad() != null ? t.getPrioridad() : "Media");
                ps.setString(7, t.getEstado() != null ? t.getEstado() : "Pendiente");

                // Validación estricta de fecha_limite para datetime2(7)
                if (t.getFechaLimite() != null && esFechaValida(t.getFechaLimite())) {
                    ps.setString(8, t.getFechaLimite());
                } else {
                    ps.setNull(8, java.sql.Types.TIMESTAMP);
                }

                boolean insertado = ps.executeUpdate() > 0;

                // Si se proporcionó un usuario asignado, registrar en Tarea_Responsable con prefijo RES000001
                String responsableId = (usuarioIdAsignado != null && !usuarioIdAsignado.trim().isEmpty())
                        ? usuarioIdAsignado
                        : t.getAsignadoA();

                if (insertado && responsableId != null && !responsableId.trim().isEmpty()) {
                    boolean responsableAsignado = asignarResponsableTarea(autoCloseCon, t.getId(), responsableId);
                    if (responsableAsignado) {
                        cNotificacion notificacionControlador = new cNotificacion();
                        Notificacion notificacion = new Notificacion(
                                responsableId,
                                "Nueva Tarea",
                                "Se te ha asignado la tarea: " + t.getTitulo(),
                                "Asignacion",
                                t.getId()
                        );
                        notificacionControlador.crearNotificacion(autoCloseCon, notificacion);
                    }
                }

                return insertado;
            }
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error al crear tarea: " + e.getMessage());
            return false;
        }
    }

    public boolean crearTarea(Tarea t) {
        return crearTarea(t, t.getAsignadoA());
    }

    // Asignar responsable a una tarea en Tarea_Responsable (Prefijo RES000001)
    public boolean asignarResponsableTarea(Connection con, String idTarea, String idUsuario) {
        if (con == null || idTarea == null || idUsuario == null || idUsuario.trim().isEmpty()) return false;
        try {
            String idRes = generarIdSeguro(con, "Tarea_Responsable", "id", "RES", 6);
            String sql = "INSERT INTO Tarea_Responsable (id, tarea_id, usuario_id) VALUES (?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, idRes);
                ps.setString(2, idTarea);
                ps.setString(3, idUsuario);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error al asignar responsable de tarea en Tarea_Responsable: " + e.getMessage());
            return false;
        }
    }

    public boolean asignarResponsableTarea(String idTarea, String idUsuario) {
        Connection con = conexionHelper.conectar();
        if (con == null) return false;
        try (Connection autoCloseCon = con) {
            return asignarResponsableTarea(autoCloseCon, idTarea, idUsuario);
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error en asignarResponsableTarea: " + e.getMessage());
            return false;
        }
    }

    // Listar tareas pertenecientes a un proyecto consultando por Tarea.proyecto_id y su responsable (si existe)
    public List<Tarea> listarTareasPorProyecto(String idProyecto) {
        List<Tarea> lista = new ArrayList<>();
        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT t.id, t.proyecto_id, t.fase_id, t.titulo, t.instrucciones, t.prioridad, t.estado, t.fecha_limite, " +
                     "tr.usuario_id AS responsable_id, u.nombre_completo AS responsable_nombre " +
                     "FROM Tarea t " +
                     "LEFT JOIN Tarea_Responsable tr ON t.id = tr.tarea_id " +
                     "LEFT JOIN Usuario u ON tr.usuario_id = u.id " +
                     "WHERE t.proyecto_id = ? ORDER BY t.created_at DESC";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, idProyecto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Tarea t = new Tarea();
                    t.setId(rs.getString("id"));
                    t.setProyectoId(rs.getString("proyecto_id"));
                    t.setFaseId(rs.getString("fase_id"));
                    t.setTitulo(rs.getString("titulo"));
                    t.setInstrucciones(rs.getString("instrucciones"));
                    t.setPrioridad(rs.getString("prioridad"));
                    t.setEstado(rs.getString("estado"));
                    t.setFechaLimite(extraerFechaCorta(rs.getString("fecha_limite")));
                    String respId = rs.getString("responsable_id");
                    t.setResponsableId(respId);
                    t.setAsignadoA(respId);
                    t.setResponsableNombre(rs.getString("responsable_nombre"));
                    lista.add(t);
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error al listar tareas por proyecto: " + e.getMessage());
        }
        return lista;
    }

    // Listar tareas asignadas a un usuario filtradas por estado ('Finalizada', 'Pendiente', etc.)
    public List<Tarea> listarTareasPorUsuarioYEstado(String idUsuario, String estado) {
        List<Tarea> lista = new ArrayList<>();
        if (idUsuario == null || idUsuario.trim().isEmpty()) return lista;

        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        StringBuilder sql = new StringBuilder(
                "SELECT t.id, t.proyecto_id, t.fase_id, t.titulo, t.instrucciones, t.prioridad, t.estado, t.fecha_limite, " +
                "tr.usuario_id AS responsable_id, u.nombre_completo AS responsable_nombre " +
                "FROM Tarea t " +
                "LEFT JOIN Tarea_Responsable tr ON t.id = tr.tarea_id " +
                "LEFT JOIN Usuario u ON tr.usuario_id = u.id " +
                "WHERE tr.usuario_id = ?"
        );

        boolean filtrarPorParametro = false;
        if ("TODAS".equalsIgnoreCase(estado) || estado == null || estado.trim().isEmpty()) {
            // Retorna todas las tareas asignadas al usuario sin filtro de estado
        } else if ("Finalizada".equalsIgnoreCase(estado)) {
            sql.append(" AND t.estado = 'Finalizada'");
        } else if ("Pendiente".equalsIgnoreCase(estado)) {
            sql.append(" AND t.estado != 'Finalizada'");
        } else {
            sql.append(" AND t.estado = ?");
            filtrarPorParametro = true;
        }
        sql.append(" ORDER BY t.created_at DESC");

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql.toString())) {

            ps.setString(1, idUsuario);
            if (filtrarPorParametro) {
                ps.setString(2, estado);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Tarea t = new Tarea();
                    t.setId(rs.getString("id"));
                    t.setProyectoId(rs.getString("proyecto_id"));
                    t.setFaseId(rs.getString("fase_id"));
                    t.setTitulo(rs.getString("titulo"));
                    t.setInstrucciones(rs.getString("instrucciones"));
                    t.setPrioridad(rs.getString("prioridad"));
                    t.setEstado(rs.getString("estado"));
                    t.setFechaLimite(extraerFechaCorta(rs.getString("fecha_limite")));
                    String respId = rs.getString("responsable_id");
                    t.setResponsableId(respId);
                    t.setAsignadoA(respId);
                    t.setResponsableNombre(rs.getString("responsable_nombre"));
                    lista.add(t);
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error al listar tareas por usuario y estado: " + e.getMessage());
        }
        return lista;
    }

    // Transacción atómica para actualizar datos de la tarea y gestionar su responsable en Tarea_Responsable
    public boolean actualizarTareaYResponsable(Tarea t, String nuevoUsuarioId) {
        if (t == null || t.getId() == null || t.getId().trim().isEmpty()
                || t.getTitulo() == null || t.getTitulo().trim().isEmpty()) {
            return false;
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try {
            con.setAutoCommit(false);

            String estadoLimpio = (t.getEstado() != null && !t.getEstado().trim().isEmpty())
                    ? t.getEstado().trim()
                    : "Pendiente";
            String prioridadLimpia = (t.getPrioridad() != null && !t.getPrioridad().trim().isEmpty())
                    ? t.getPrioridad().trim()
                    : "Media";

            String sqlUpdate = "UPDATE Tarea SET fase_id = ?, titulo = ?, instrucciones = ?, prioridad = ?, " +
                               "estado = ?, fecha_limite = ?, " +
                               "fecha_culminacion = CASE WHEN ? = 'Finalizada' THEN SYSUTCDATETIME() ELSE NULL END " +
                               "WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                if (t.getFaseId() != null && !t.getFaseId().trim().isEmpty()) {
                    ps.setString(1, t.getFaseId().trim());
                } else {
                    ps.setNull(1, java.sql.Types.VARCHAR);
                }

                ps.setString(2, t.getTitulo().trim());
                ps.setString(3, t.getInstrucciones() != null ? t.getInstrucciones().trim() : "");
                ps.setString(4, prioridadLimpia);
                ps.setString(5, estadoLimpio);

                if (t.getFechaLimite() != null && esFechaValida(t.getFechaLimite())) {
                    ps.setString(6, t.getFechaLimite().trim());
                } else {
                    ps.setNull(6, java.sql.Types.TIMESTAMP);
                }

                ps.setString(7, estadoLimpio);
                ps.setString(8, t.getId().trim());

                if (ps.executeUpdate() <= 0) {
                    con.rollback();
                    con.close();
                    return false;
                }
            }

            // Consultar el usuario actualmente asignado en Tarea_Responsable
            String usuarioAnterior = null;
            String sqlActual = "SELECT TOP 1 usuario_id FROM Tarea_Responsable WHERE tarea_id = ?";
            try (PreparedStatement psAct = con.prepareStatement(sqlActual)) {
                psAct.setString(1, t.getId().trim());
                try (ResultSet rsAct = psAct.executeQuery()) {
                    if (rsAct.next()) {
                        usuarioAnterior = rsAct.getString("usuario_id");
                    }
                }
            }

            String nuevoResp = (nuevoUsuarioId != null && !nuevoUsuarioId.trim().isEmpty())
                    ? nuevoUsuarioId.trim()
                    : null;

            if (nuevoResp == null) {
                String sqlDel = "DELETE FROM Tarea_Responsable WHERE tarea_id = ?";
                try (PreparedStatement psDel = con.prepareStatement(sqlDel)) {
                    psDel.setString(1, t.getId().trim());
                    psDel.executeUpdate();
                }
            } else if (!nuevoResp.equals(usuarioAnterior)) {
                String sqlDel = "DELETE FROM Tarea_Responsable WHERE tarea_id = ?";
                try (PreparedStatement psDel = con.prepareStatement(sqlDel)) {
                    psDel.setString(1, t.getId().trim());
                    psDel.executeUpdate();
                }

                boolean asignado = asignarResponsableTarea(con, t.getId().trim(), nuevoResp);
                if (!asignado) {
                    con.rollback();
                    con.close();
                    return false;
                }

                cNotificacion notificacionControlador = new cNotificacion();
                Notificacion notificacion = new Notificacion(
                        nuevoResp,
                        "Tarea Asignada",
                        "Se te ha asignado la tarea: " + t.getTitulo().trim(),
                        "Asignacion",
                        t.getId().trim()
                );
                notificacionControlador.crearNotificacion(con, notificacion);
            }

            con.commit();
            con.close();
            return true;
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error en actualizarTareaYResponsable: " + e.getMessage());
            try {
                if (con != null && !con.isClosed()) {
                    con.rollback();
                    con.close();
                }
            } catch (Exception rollbackEx) {
                Log.e("CRUD_TAREA", "Error en Rollback: " + rollbackEx.getMessage());
            }
            return false;
        }
    }

    // Eliminar una tarea y su asignación en Tarea_Responsable (transacción atómica)
    public boolean eliminarTarea(String tareaId) {
        if (tareaId == null || tareaId.trim().isEmpty()) return false;

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try {
            con.setAutoCommit(false);

            String sqlDelResp = "DELETE FROM Tarea_Responsable WHERE tarea_id = ?";
            try (PreparedStatement psResp = con.prepareStatement(sqlDelResp)) {
                psResp.setString(1, tareaId.trim());
                psResp.executeUpdate();
            }

            String sqlDelTarea = "DELETE FROM Tarea WHERE id = ?";
            int eliminadas;
            try (PreparedStatement psTar = con.prepareStatement(sqlDelTarea)) {
                psTar.setString(1, tareaId.trim());
                eliminadas = psTar.executeUpdate();
            }

            if (eliminadas > 0) {
                con.commit();
                con.close();
                return true;
            } else {
                con.rollback();
                con.close();
                return false;
            }
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error al eliminar tarea: " + e.getMessage());
            try {
                if (con != null && !con.isClosed()) {
                    con.rollback();
                    con.close();
                }
            } catch (Exception rollbackEx) {
                Log.e("CRUD_TAREA", "Error en Rollback eliminarTarea: " + rollbackEx.getMessage());
            }
            return false;
        }
    }

    // Actualización rápida de estado de tarea
    public boolean actualizarEstadoTarea(String idTarea, String nuevoEstado) {
        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String sql = "UPDATE Tarea SET estado = ?, fecha_culminacion = CASE WHEN ? = 'Finalizada' THEN SYSUTCDATETIME() ELSE NULL END WHERE id = ?";
        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setString(2, nuevoEstado);
            ps.setString(3, idTarea);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error al actualizar estado de tarea: " + e.getMessage());
            return false;
        }
    }
}
