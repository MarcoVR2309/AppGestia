package com.example.maquetagestia.controller;

import android.util.Log;

import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.model.Fase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class cFase {

    private final conSQLServer conexionHelper = new conSQLServer();

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
