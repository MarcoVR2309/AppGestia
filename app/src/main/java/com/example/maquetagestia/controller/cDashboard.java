package com.example.maquetagestia.controller;

import android.util.Log;

import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.model.DashboardDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class cDashboard {

    private final conSQLServer conexionHelper = new conSQLServer();

    // Consultar métricas dinámicas del usuario en SQL Server para el Dashboard
    public DashboardDTO obtenerMetricasUsuario(String idUsuario) {
        DashboardDTO dto = new DashboardDTO();
        if (idUsuario == null || idUsuario.isEmpty()) return dto;

        Connection con = conexionHelper.conectar();
        if (con == null) return dto;

        try (Connection autoCloseCon = con) {
            // 1. Conteo de proyectos activos del usuario (en los que participa y estado != 'Finalizado')
            String sqlPry = "SELECT COUNT(DISTINCT p.id) AS activos " +
                            "FROM Proyecto p " +
                            "INNER JOIN Proyecto_Usuario pu ON p.id = pu.proyecto_id " +
                            "WHERE pu.usuario_id = ? AND p.estado != 'Finalizado'";
            try (PreparedStatement ps1 = autoCloseCon.prepareStatement(sqlPry)) {
                ps1.setString(1, idUsuario);
                try (ResultSet rs1 = ps1.executeQuery()) {
                    if (rs1.next()) {
                        dto.setProyectosActivos(rs1.getInt("activos"));
                    }
                }
            }

            // 2. Conteo de tareas asignadas (Total, Completadas, Pendientes)
            String sqlTar = "SELECT COUNT(*) AS total, " +
                            "SUM(CASE WHEN t.estado = 'Finalizada' THEN 1 ELSE 0 END) AS completadas, " +
                            "SUM(CASE WHEN t.estado != 'Finalizada' THEN 1 ELSE 0 END) AS pendientes " +
                            "FROM Tarea t " +
                            "INNER JOIN Tarea_Responsable tr ON t.id = tr.tarea_id " +
                            "WHERE tr.usuario_id = ?";
            try (PreparedStatement ps2 = autoCloseCon.prepareStatement(sqlTar)) {
                ps2.setString(1, idUsuario);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        int total = rs2.getInt("total");
                        int completadas = rs2.getInt("completadas");
                        int pendientes = rs2.getInt("pendientes");

                        dto.setTareasAsignadas(total);
                        dto.setTareasCompletadas(completadas);
                        dto.setTareasPendientes(pendientes);

                        // Calcular porcentaje global
                        int porcentaje = total > 0 ? (completadas * 100) / total : 0;
                        dto.setPorcentajeAvanceGlobal(porcentaje);
                    }
                }
            }

        } catch (Exception e) {
            Log.e("CRUD_DASHBOARD", "Error al consultar métricas de dashboard: " + e.getMessage());
        }

        return dto;
    }

    // DTO para estadísticas detalladas
    public static class EstadisticasDTO {
        public int totalAsignadas = 0;
        public int completadas = 0;
        public int pendientes = 0;
        public int vencidas = 0;
        public int porcentajeCumplimiento = 0;
    }

    // Consultar analíticas detalladas para frm_estadisticas.java
    public EstadisticasDTO obtenerEstadisticasDetalladas(String idUsuario) {
        EstadisticasDTO dto = new EstadisticasDTO();
        if (idUsuario == null || idUsuario.isEmpty()) return dto;

        Connection con = conexionHelper.conectar();
        if (con == null) return dto;

        String sql = "SELECT COUNT(*) AS total, " +
                     "SUM(CASE WHEN t.estado = 'Finalizada' THEN 1 ELSE 0 END) AS completadas, " +
                     "SUM(CASE WHEN t.estado IN ('Pendiente', 'EnProgreso') THEN 1 ELSE 0 END) AS pendientes, " +
                     "SUM(CASE WHEN t.estado != 'Finalizada' AND t.fecha_limite IS NOT NULL AND t.fecha_limite < SYSUTCDATETIME() THEN 1 ELSE 0 END) AS vencidas " +
                     "FROM Tarea t " +
                     "INNER JOIN Tarea_Responsable tr ON t.id = tr.tarea_id " +
                     "WHERE tr.usuario_id = ?";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dto.totalAsignadas = rs.getInt("total");
                    dto.completadas = rs.getInt("completadas");
                    dto.pendientes = rs.getInt("pendientes");
                    dto.vencidas = rs.getInt("vencidas");

                    if (dto.totalAsignadas > 0) {
                        dto.porcentajeCumplimiento = (dto.completadas * 100) / dto.totalAsignadas;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_DASHBOARD", "Error al obtener estadísticas detalladas: " + e.getMessage());
        }

        return dto;
    }
}
