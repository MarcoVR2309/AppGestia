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

    // Transacción Atómica Completa: Crear Proyecto (PRY), Asignar Líder en ProyectoUsuario (PU) y Crear 3 Fases (FAS)
    public boolean crearProyectoConFases(Proyecto p, String usuarioIdLider) {
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

            // 3. Insertar 3 Fases por defecto ("Planificación", "Desarrollo", "Cierre") con ID prefijo FAS000001
            String[] nombresFases = {"Planificación", "Desarrollo", "Cierre"};
            String sqlFas = "INSERT INTO Fase (id, proyecto_id, nombre, orden, estado) VALUES (?, ?, ?, ?, ?)";
            for (int i = 0; i < nombresFases.length; i++) {
                String idFas = generarIdSeguro(con, "Fase", "id", "FAS", 6);
                try (PreparedStatement psFas = con.prepareStatement(sqlFas)) {
                    psFas.setString(1, idFas);
                    psFas.setString(2, p.getId());
                    psFas.setString(3, nombresFases[i]);
                    psFas.setInt(4, i + 1);
                    psFas.setString(5, i == 0 ? "EnProgreso" : "Pendiente");
                    psFas.executeUpdate();
                }
            }

            con.commit(); // Confirmar la transacción atómica completa
            con.close();
            return true;
        } catch (Exception e) {
            Log.e("CRUD_PROYECTO", "Error en crearProyectoConFases: " + e.getMessage());
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

    // Listar proyectos asociados a un usuario con filtro por tipo
    public List<Proyecto> listarPorUsuario(String usuarioId, String filtroTipo) {
        List<Proyecto> lista = new ArrayList<>();
        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT p.id, p.nombre, p.descripcion, p.tipo, p.estado, p.fecha_inicio, p.fecha_fin_estimada, p.creado_por " +
                     "FROM Proyecto p " +
                     "INNER JOIN Proyecto_Usuario pu ON p.id = pu.proyecto_id " +
                     "WHERE pu.usuario_id = ?";

        if (filtroTipo != null && !filtroTipo.isEmpty() && !filtroTipo.equalsIgnoreCase("Todos")) {
            sql += " AND p.tipo = ?";
        }

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
}
