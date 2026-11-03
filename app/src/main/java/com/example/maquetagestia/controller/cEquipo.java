package com.example.maquetagestia.controller;

import android.util.Log;

import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class cEquipo {

    private final conSQLServer conexionHelper = new conSQLServer();

    // DTO para representar a un miembro del equipo con su Usuario y Rol en el proyecto
    public static class MiembroEquipoItem {
        public final Usuario usuario;
        public final String rol;

        public MiembroEquipoItem(Usuario usuario, String rol) {
            this.usuario = usuario;
            this.rol = rol;
        }
    }

    // Listar miembros del equipo por proyecto mediante INNER JOIN
    public List<MiembroEquipoItem> listarEquipoPorProyecto(String idProyecto) {
        List<MiembroEquipoItem> lista = new ArrayList<>();
        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT u.id, u.nombre_completo, u.correo, u.telefono, u.avatar_url, pu.rol " +
                     "FROM Usuario u INNER JOIN ProyectoUsuario pu ON u.id = pu.usuario_id " +
                     "WHERE pu.proyecto_id = ?";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, idProyecto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getString("id"));
                    u.setNombreCompleto(rs.getString("nombre_completo"));
                    u.setCorreo(rs.getString("correo"));
                    u.setTelefono(rs.getString("telefono"));
                    u.setAvatarUrl(rs.getString("avatar_url"));
                    String rol = rs.getString("rol");
                    lista.add(new MiembroEquipoItem(u, rol));
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error al listar equipo por proyecto: " + e.getMessage());
        }
        return lista;
    }

    // Agregar nuevo miembro al equipo buscando el usuario por correo
    public boolean agregarMiembroEquipo(String idProyecto, String correoUsuario, String rol) {
        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            // 1. Buscar ID del usuario por su correo
            String sqlBusqueda = "SELECT id FROM Usuario WHERE correo = ?";
            String idUsuario = null;
            try (PreparedStatement psB = autoCloseCon.prepareStatement(sqlBusqueda)) {
                psB.setString(1, correoUsuario);
                try (ResultSet rsB = psB.executeQuery()) {
                    if (rsB.next()) {
                        idUsuario = rsB.getString("id");
                    }
                }
            }

            if (idUsuario == null) return false; // Usuario no registrado en la base de datos

            // 2. Generar ID con prefijo PU000001
            cProyecto controllerPry = new cProyecto();
            String idPu = controllerPry.generarIdSeguro(autoCloseCon, "ProyectoUsuario", "id", "PU", 6);

            // 3. Insertar en ProyectoUsuario
            String sqlInsert = "INSERT INTO ProyectoUsuario (id, proyecto_id, usuario_id, rol) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psI = autoCloseCon.prepareStatement(sqlInsert)) {
                psI.setString(1, idPu);
                psI.setString(2, idProyecto);
                psI.setString(3, idUsuario);
                psI.setString(4, rol != null ? rol : "Colaborador");
                return psI.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error al agregar miembro al equipo: " + e.getMessage());
            return false;
        }
    }
}
