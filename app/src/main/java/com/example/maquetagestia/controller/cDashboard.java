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

    // Consultar analíticas detalladas para frm_estadisticas.java (histórico completo)
    public EstadisticasDTO obtenerEstadisticasDetalladas(String idUsuario) {
        return obtenerEstadisticasDetalladas(idUsuario, "Todo");
    }

    // Consultar analíticas detalladas filtradas por periodo usando Tarea.created_at
    public EstadisticasDTO obtenerEstadisticasDetalladas(String idUsuario, String periodo) {
        EstadisticasDTO dto = new EstadisticasDTO();
        if (idUsuario == null || idUsuario.isEmpty()) return dto;

        Connection con = conexionHelper.conectar();
        if (con == null) return dto;

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) AS total, " +
                "ISNULL(SUM(CASE WHEN t.estado = 'Finalizada' THEN 1 ELSE 0 END), 0) AS completadas, " +
                "ISNULL(SUM(CASE WHEN t.estado IN ('Pendiente', 'EnProgreso') THEN 1 ELSE 0 END), 0) AS pendientes, " +
                "ISNULL(SUM(CASE WHEN t.estado != 'Finalizada' AND t.fecha_limite IS NOT NULL AND t.fecha_limite < SYSUTCDATETIME() THEN 1 ELSE 0 END), 0) AS vencidas " +
                "FROM Tarea t " +
                "INNER JOIN Tarea_Responsable tr ON t.id = tr.tarea_id " +
                "WHERE tr.usuario_id = ?"
        );

        if ("Este mes".equalsIgnoreCase(periodo)) {
            sql.append(" AND YEAR(t.created_at) = YEAR(SYSUTCDATETIME()) AND MONTH(t.created_at) = MONTH(SYSUTCDATETIME())");
        } else if ("Mes anterior".equalsIgnoreCase(periodo)) {
            sql.append(" AND t.created_at >= DATEADD(MONTH, DATEDIFF(MONTH, 0, SYSUTCDATETIME()) - 1, 0)");
            sql.append(" AND t.created_at < DATEADD(MONTH, DATEDIFF(MONTH, 0, SYSUTCDATETIME()), 0)");
        } else if ("Últimos 3 meses".equalsIgnoreCase(periodo)) {
            sql.append(" AND t.created_at >= DATEADD(MONTH, -3, SYSUTCDATETIME())");
        } else if ("Este año".equalsIgnoreCase(periodo)) {
            sql.append(" AND YEAR(t.created_at) = YEAR(SYSUTCDATETIME())");
        }

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql.toString())) {

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
