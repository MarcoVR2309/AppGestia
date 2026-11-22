package com.example.maquetagestia.controller;

import android.util.Log;

import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.model.Notificacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class cNotificacion {

    private final conSQLServer conexionHelper = new conSQLServer();

    // Generador de código primario correlativo con prefijo (Ej: NOT000001)
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
            Log.e("CRUD_NOTIFICACION", "Error generando ID correlativo para " + tabla + ": " + e.getMessage());
            nuevoCodigo = prefijo + String.format(Locale.US, "%0" + digitosLongitud + "d", 1L);
        }

        return nuevoCodigo;
    }

    // Inserción de notificación reutilizando la conexión recibida por parámetro (soporta transacciones)
    public boolean crearNotificacion(Connection con, Notificacion n) {
        if (con == null || n == null) return false;

        try {
            String idNot = generarIdSeguro(con, "Notificacion", "id", "NOT", 6);
            n.setId(idNot);

            String sql = "INSERT INTO Notificacion (id, usuario_id, titulo, mensaje, tipo, referencia_id) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, n.getId());
                ps.setString(2, n.getUsuarioId());
                ps.setString(3, n.getTitulo());
                ps.setString(4, n.getMensaje());
                ps.setString(5, n.getTipo() != null ? n.getTipo() : "Sistema");

                if (n.getReferenciaId() != null && !n.getReferenciaId().trim().isEmpty()) {
                    ps.setString(6, n.getReferenciaId());
                } else {
                    ps.setNull(6, java.sql.Types.VARCHAR);
                }

                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_NOTIFICACION", "Error al crear notificación: " + e.getMessage());
            return false;
        }
    }

    // Listar notificaciones pertenecientes a un usuario ordenadas de la más reciente a la más antigua
    public List<Notificacion> listarPorUsuario(String idUsuario) {
        List<Notificacion> lista = new ArrayList<>();
        if (idUsuario == null || idUsuario.trim().isEmpty()) return lista;

        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT id, usuario_id, titulo, mensaje, tipo, referencia_id, leido, fecha_generacion " +
                     "FROM Notificacion WHERE usuario_id = ? ORDER BY fecha_generacion DESC";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notificacion n = new Notificacion();
                    n.setId(rs.getString("id"));
                    n.setUsuarioId(rs.getString("usuario_id"));
                    n.setTitulo(rs.getString("titulo"));
                    n.setMensaje(rs.getString("mensaje"));
                    n.setTipo(rs.getString("tipo"));
                    n.setReferenciaId(rs.getString("referencia_id"));
                    n.setLeido(rs.getBoolean("leido"));
                    n.setFechaGeneracion(rs.getString("fecha_generacion"));
                    lista.add(n);
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_NOTIFICACION", "Error al listar notificaciones por usuario: " + e.getMessage());
        }
        return lista;
    }

    // Marcar una notificación específica como leída
    public boolean marcarComoLeida(String idNotificacion) {
        if (idNotificacion == null || idNotificacion.trim().isEmpty()) return false;

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String sql = "UPDATE Notificacion SET leido = 1 WHERE id = ?";
        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, idNotificacion);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            Log.e("CRUD_NOTIFICACION", "Error al marcar notificación como leída: " + e.getMessage());
            return false;
        }
    }

    // Marcar todas las notificaciones del usuario como leídas (preservando invitaciones pendientes)
    public boolean marcarTodasComoLeidas(String idUsuario) {
        if (idUsuario == null || idUsuario.trim().isEmpty()) return false;

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String sql = "UPDATE Notificacion SET leido = 1 WHERE usuario_id = ? AND leido = 0 AND tipo != 'Invitacion'";
        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, idUsuario);
            return ps.executeUpdate() >= 0;
        } catch (Exception e) {
            Log.e("CRUD_NOTIFICACION", "Error al marcar todas las notificaciones como leídas: " + e.getMessage());
            return false;
        }
    }
}
