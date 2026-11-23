package com.example.maquetagestia.controller;

import android.util.Log;

import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.model.Fase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class cFase {

    private final conSQLServer conexionHelper = new conSQLServer();

    // Generador de código primario correlativo con prefijo FAS000001
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
            Log.e("CRUD_FASE", "Error generando ID correlativo para " + tabla + ": " + e.getMessage());
            nuevoCodigo = prefijo + String.format(Locale.US, "%0" + digitosLongitud + "d", 1L);
        }

        return nuevoCodigo;
    }

    private boolean esFechaValida(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            sdf.parse(fecha.trim());
            return true;
        } catch (Exception e1) {
            try {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                sdf2.setLenient(false);
                sdf2.parse(fecha.trim());
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    // Insertar una nueva fase personalizada con ID FAS000001 y orden correlativo en SQL Server
    public boolean crearFase(String proyectoId, String nombre, String fechaEstimada) {
        return crearFase(proyectoId, nombre, fechaEstimada, null);
    }

    public boolean crearFase(String proyectoId, String nombre, String fechaEstimada, String usuarioIdSolicitante) {
        if (proyectoId == null || proyectoId.trim().isEmpty()
                || nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        if (usuarioIdSolicitante != null && !usuarioIdSolicitante.trim().isEmpty()
                && !new cEquipo().esLiderDelProyecto(proyectoId.trim(), usuarioIdSolicitante.trim())) {
            return false;
        }

        Fase fase = new Fase();
        fase.setProyectoId(proyectoId.trim());
        fase.setNombre(nombre.trim());
        fase.setFechaEstimada(fechaEstimada != null ? fechaEstimada.trim() : null);
        return crearFase(fase);
    }

    public boolean crearFase(Fase f) {
        if (f == null || f.getProyectoId() == null || f.getProyectoId().trim().isEmpty()
                || f.getNombre() == null || f.getNombre().trim().isEmpty()) {
            return false;
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            int cantidadActual = 0;
            String sqlCount = "SELECT COUNT(*) AS total FROM Fase WHERE proyecto_id = ?";
            try (PreparedStatement psCount = autoCloseCon.prepareStatement(sqlCount)) {
                psCount.setString(1, f.getProyectoId());
                try (ResultSet rs = psCount.executeQuery()) {
                    if (rs.next()) {
                        cantidadActual = rs.getInt("total");
                    }
                }
            }

            int ordenCorrelativo = f.getOrden() > 0 ? f.getOrden() : (cantidadActual + 1);
            String estadoFase = (f.getEstado() != null && !f.getEstado().trim().isEmpty())
                    ? f.getEstado()
                    : (ordenCorrelativo == 1 ? "EnProgreso" : "Pendiente");

            String idFas = generarIdSeguro(autoCloseCon, "Fase", "id", "FAS", 6);
            f.setId(idFas);
            f.setOrden(ordenCorrelativo);
            f.setEstado(estadoFase);

            String sqlInsert = "INSERT INTO Fase (id, proyecto_id, nombre, orden, fecha_estimada, estado) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psInsert = autoCloseCon.prepareStatement(sqlInsert)) {
                psInsert.setString(1, f.getId());
                psInsert.setString(2, f.getProyectoId());
                psInsert.setString(3, f.getNombre());
                psInsert.setInt(4, f.getOrden());

                if (esFechaValida(f.getFechaEstimada())) {
                    psInsert.setString(5, f.getFechaEstimada().trim());
                } else {
                    psInsert.setNull(5, java.sql.Types.DATE);
                }

                psInsert.setString(6, f.getEstado());
                return psInsert.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_FASE", "Error al crear fase: " + e.getMessage());
            return false;
        }
    }

    // Actualizar nombre, fecha estimada y estado de una fase existente en SQL Server
    public boolean actualizarFase(Fase f) {
        return actualizarFase(f, null);
    }

    public boolean actualizarFase(Fase f, String usuarioIdSolicitante) {
        if (f == null || f.getId() == null || f.getId().trim().isEmpty()
                || f.getNombre() == null || f.getNombre().trim().isEmpty()) {
            return false;
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String nombreLimpio = f.getNombre().trim();
        String fechaLimpia = esFechaValida(f.getFechaEstimada()) ? f.getFechaEstimada().trim() : null;
        String estadoLimpio = (f.getEstado() != null && !f.getEstado().trim().isEmpty())
                ? f.getEstado().trim()
                : "Pendiente";

        try (Connection autoCloseCon = con) {
            if (usuarioIdSolicitante != null && !usuarioIdSolicitante.trim().isEmpty()) {
                String proyectoReal = obtenerProyectoIdPorFase(autoCloseCon, f.getId().trim());
                if (proyectoReal == null || !new cEquipo().esLiderDelProyecto(autoCloseCon, proyectoReal, usuarioIdSolicitante.trim())) {
                    return false;
                }
            }

            String sql = "UPDATE Fase SET nombre = ?, fecha_estimada = ?, estado = ? WHERE id = ?";
            try (PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
                ps.setString(1, nombreLimpio);
                if (fechaLimpia != null) {
                    ps.setString(2, fechaLimpia);
                } else {
                    ps.setNull(2, java.sql.Types.DATE);
                }
                ps.setString(3, estadoLimpio);
                ps.setString(4, f.getId().trim());

                boolean actualizado = ps.executeUpdate() > 0;
                if (actualizado) {
                    f.setNombre(nombreLimpio);
                    f.setFechaEstimada(fechaLimpia);
                    f.setEstado(estadoLimpio);
                }
                return actualizado;
            }
        } catch (Exception e) {
            Log.e("CRUD_FASE", "Error al actualizar fase: " + e.getMessage());
            return false;
        }
    }

    // Eliminar una fase desvinculando primero sus tareas asociadas (transacción atómica)
    public boolean eliminarFase(String faseId) {
        return eliminarFase(faseId, null);
    }

    public boolean eliminarFase(String faseId, String usuarioIdSolicitante) {
        if (faseId == null || faseId.trim().isEmpty()) return false;

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try {
            if (usuarioIdSolicitante != null && !usuarioIdSolicitante.trim().isEmpty()) {
                String proyectoReal = obtenerProyectoIdPorFase(con, faseId.trim());
                if (proyectoReal == null || !new cEquipo().esLiderDelProyecto(con, proyectoReal, usuarioIdSolicitante.trim())) {
                    con.close();
                    return false;
                }
            }

            con.setAutoCommit(false);

            String sqlDesvincular = "UPDATE Tarea SET fase_id = NULL WHERE fase_id = ?";
            try (PreparedStatement psDesv = con.prepareStatement(sqlDesvincular)) {
                psDesv.setString(1, faseId.trim());
                psDesv.executeUpdate();
            }

            String sqlEliminar = "DELETE FROM Fase WHERE id = ?";
            int eliminadas;
            try (PreparedStatement psDel = con.prepareStatement(sqlEliminar)) {
                psDel.setString(1, faseId.trim());
                eliminadas = psDel.executeUpdate();
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
            Log.e("CRUD_FASE", "Error al eliminar fase: " + e.getMessage());
            try {
                if (con != null && !con.isClosed()) {
                    con.rollback();
                    con.close();
                }
            } catch (Exception rollbackEx) {
                Log.e("CRUD_FASE", "Error en Rollback eliminarFase: " + rollbackEx.getMessage());
            }
            return false;
        }
    }

    private String obtenerProyectoIdPorFase(Connection con, String faseId) {
        String sql = "SELECT proyecto_id FROM Fase WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, faseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("proyecto_id");
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_FASE", "Error obteniendo proyecto de fase: " + e.getMessage());
        }
        return null;
    }

    // Listar las fases pertenecientes a un proyecto ordenadas por orden correlativo
    public List<Fase> listarPorProyecto(String proyectoId) {
        List<Fase> lista = new ArrayList<>();
        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT id, proyecto_id, nombre, orden, fecha_estimada, estado " +
                     "FROM Fase WHERE proyecto_id = ? ORDER BY orden ASC";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, proyectoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Fase f = new Fase();
                    f.setId(rs.getString("id"));
                    f.setProyectoId(rs.getString("proyecto_id"));
                    f.setNombre(rs.getString("nombre"));
                    f.setOrden(rs.getInt("orden"));
                    f.setFechaEstimada(rs.getString("fecha_estimada"));
                    f.setEstado(rs.getString("estado"));
                    lista.add(f);
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_FASE", "Error al listar fases por proyecto: " + e.getMessage());
        }
        return lista;
    }
}
