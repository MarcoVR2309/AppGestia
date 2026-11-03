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

    // Inserción de nueva tarea con try-with-resources
    public boolean crearTarea(Tarea t) {
        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            String idTar = generarIdSeguro(autoCloseCon, "Tarea", "id", "TAR", 6);
            t.setId(idTar);

            String sql = "INSERT INTO Tarea (id, fase_id, asignado_a, titulo, instrucciones, prioridad, estado, fecha_limite) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
                ps.setString(1, t.getId());
                ps.setString(2, t.getFaseId());
                ps.setString(3, t.getAsignadoA() != null && !t.getAsignadoA().isEmpty() ? t.getAsignadoA() : null);
                ps.setString(4, t.getTitulo());
                ps.setString(5, t.getInstrucciones());
                ps.setString(6, t.getPrioridad() != null ? t.getPrioridad() : "Media");
                ps.setString(7, t.getEstado() != null ? t.getEstado() : "Pendiente");
                ps.setString(8, t.getFechaLimite());

                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_TAREA", "Error al crear tarea: " + e.getMessage());
            return false;
        }
    }

    // Listar tareas pertenecientes a un proyecto uniendo con la tabla Fase
    public List<Tarea> listarTareasPorProyecto(String idProyecto) {
        List<Tarea> lista = new ArrayList<>();
        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT t.id, t.fase_id, t.asignado_a, t.titulo, t.instrucciones, t.prioridad, t.estado, t.fecha_limite " +
                     "FROM Tarea t INNER JOIN Fase f ON t.fase_id = f.id " +
                     "WHERE f.proyecto_id = ? ORDER BY t.created_at DESC";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, idProyecto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Tarea t = new Tarea();
                    t.setId(rs.getString("id"));
                    t.setFaseId(rs.getString("fase_id"));
                    t.setAsignadoA(rs.getString("asignado_a"));
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
