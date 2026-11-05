package com.example.maquetagestia.controller;

import android.util.Log;

import com.example.maquetagestia.conexion.conSQLServer;
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

    // Auxiliar para verificar formato de fecha válido (yyyy-MM-dd o yyyy-MM-dd HH:mm:ss)
    private boolean esFechaValida(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return false;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            sdf.parse(fecha);
            return true;
        } catch (Exception e1) {
            try {
                java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                sdf2.setLenient(false);
                sdf2.parse(fecha);
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
                    asignarResponsableTarea(autoCloseCon, t.getId(), responsableId);
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

    // Listar tareas pertenecientes a un proyecto consultando directamente por Tarea.proyecto_id
    public List<Tarea> listarTareasPorProyecto(String idProyecto) {
        List<Tarea> lista = new ArrayList<>();
        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT id, proyecto_id, fase_id, titulo, instrucciones, prioridad, estado, fecha_limite " +
                     "FROM Tarea WHERE proyecto_id = ? ORDER BY created_at DESC";

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
                    t.setFechaLimite(rs.getString("fecha_limite"));
                    lista.add(t);
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error al listar tareas por proyecto: " + e.getMessage());
        }
        return lista;
    }

    // Actualización rápida de estado de tarea
    public boolean actualizarEstadoTarea(String idTarea, String nuevoEstado) {
        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String sql = "UPDATE Tarea SET estado = ? WHERE id = ?";
        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setString(2, idTarea);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error al actualizar estado de tarea: " + e.getMessage());
            return false;
        }
    }
}
