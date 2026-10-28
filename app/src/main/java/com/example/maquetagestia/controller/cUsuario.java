package com.example.maquetagestia.controller;

import android.util.Log;
import com.example.maquetagestia.conexion.conSQLServer; // Ajusta si tu clase se llama conexionSQLServer
import com.example.maquetagestia.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

public class cUsuario {

    private final conSQLServer conexionHelper = new conSQLServer();


    public String generarIdSeguro(Connection con, String tabla, String columnaPk, String prefijo, int digitosLongitud) {
        String nuevoCodigo = prefijo + String.format(Locale.US, "%0" + digitosLongitud + "d", 1L);
        int longitudTotal = prefijo.length() + digitosLongitud;

        // T-SQL: TOP 1 en lugar de LIMIT 1, y LEN() en lugar de length()
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
            Log.e("CRUD_USUARIO", "Error generando ID correlativo: " + e.getMessage());
            nuevoCodigo = prefijo + String.format(Locale.US, "%0" + digitosLongitud + "d", 1L);
        }

        return nuevoCodigo;
    }

    // Verificar si el correo ya está registrado
    public boolean existeCorreo(String correo) {
        boolean existe = false;
        Connection con = conexionHelper.conectar();
        if (con != null) {
            String query = "SELECT COUNT(*) FROM Usuario WHERE correo = ?";
            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.setString(1, correo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        existe = true;
                    }
                }
                con.close();
            } catch (Exception e) {
                Log.e("CRUD_USUARIO", "Error al verificar correo: " + e.getMessage());
            }
        }
        return existe;
    }

    // Insertar usuario generando el ID correlativo
    public boolean registrar(Usuario u) {
        boolean insertado = false;
        Connection con = conexionHelper.conectar();
        if (con != null) {
            try {
                // 1. Generar el ID con formato 'USU000001'
                String idGenerado = generarIdSeguro(con, "Usuario", "id", "U", 6);
                u.setId(idGenerado);

                // 2. Ejecutar la inserción incluyendo la columna ID
                String query = "INSERT INTO Usuario (id, nombre_completo, correo, password_hash, telefono) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(query)) {
                    ps.setString(1, u.getId());
                    ps.setString(2, u.getNombreCompleto());
                    ps.setString(3, u.getCorreo());
                    ps.setString(4, u.getPasswordHash());
                    ps.setString(5, u.getTelefono() != null ? u.getTelefono() : "");

                    int filas = ps.executeUpdate();
                    insertado = (filas > 0);
                }

                con.close();
            } catch (Exception e) {
                Log.e("CRUD_USUARIO", "Error al registrar Usuario: " + e.getMessage());
            }
        }
        return insertado;
    }
}