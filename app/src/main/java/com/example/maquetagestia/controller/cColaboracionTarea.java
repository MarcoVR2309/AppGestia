package com.example.maquetagestia.controller;

import android.util.Log;

import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.model.ComentarioTarea;
import com.example.maquetagestia.model.EnlaceTarea;
import com.example.maquetagestia.model.Subtarea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class cColaboracionTarea {

    private final conSQLServer conexionHelper = new conSQLServer();

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
            Log.e("CRUD_COLAB", "Error generando ID correlativo para " + tabla + ": " + e.getMessage());
            nuevoCodigo = prefijo + String.format(Locale.US, "%0" + digitosLongitud + "d", 1L);
        }

        return nuevoCodigo;
    }

    // ==================== COMENTARIOS ====================

    public List<ComentarioTarea> listarComentariosPorTarea(String tareaId) {
        List<ComentarioTarea> lista = new ArrayList<>();
        if (tareaId == null || tareaId.trim().isEmpty()) return lista;

        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT c.id, c.tarea_id, c.usuario_id, " +
                     "ISNULL(u.nombre_completo, 'Usuario') AS autor_nombre, " +
                     "c.contenido, CONVERT(VARCHAR(16), c.fecha_creacion, 120) AS fecha_creacion " +
                     "FROM ComentarioTarea c " +
                     "LEFT JOIN Usuario u ON c.usuario_id = u.id " +
                     "WHERE c.tarea_id = ? " +
                     "ORDER BY c.fecha_creacion ASC, c.id ASC";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
            ps.setString(1, tareaId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ComentarioTarea(
                            rs.getString("id"),
                            rs.getString("tarea_id"),
                            rs.getString("usuario_id"),
                            rs.getString("autor_nombre"),
                            rs.getString("contenido"),
                            rs.getString("fecha_creacion")
                    ));
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_COLAB", "Error al listar comentarios: " + e.getMessage());
        }
        return lista;
    }

    public boolean crearComentario(String tareaId, String usuarioId, String contenido) {
        if (tareaId == null || tareaId.trim().isEmpty()
                || usuarioId == null || usuarioId.trim().isEmpty()
                || contenido == null || contenido.trim().isEmpty()) {
            return false;
        }

        String textoLimpio = contenido.trim();
        if (textoLimpio.length() > 500) {
            textoLimpio = textoLimpio.substring(0, 500);
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            String idCom = generarIdSeguro(autoCloseCon, "ComentarioTarea", "id", "COM", 6);
            String sql = "INSERT INTO ComentarioTarea (id, tarea_id, usuario_id, contenido) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
                ps.setString(1, idCom);
                ps.setString(2, tareaId.trim());
                ps.setString(3, usuarioId.trim());
                ps.setString(4, textoLimpio);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_COLAB", "Error al crear comentario: " + e.getMessage());
            return false;
        }
    }

    // ==================== ENLACES DE ENTREGABLES ====================

    public List<EnlaceTarea> listarEnlacesPorTarea(String tareaId) {
        List<EnlaceTarea> lista = new ArrayList<>();
        if (tareaId == null || tareaId.trim().isEmpty()) return lista;

        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT e.id, e.tarea_id, e.usuario_id, " +
                     "ISNULL(u.nombre_completo, 'Usuario') AS autor_nombre, " +
                     "e.titulo, e.url, CONVERT(VARCHAR(16), e.fecha_creacion, 120) AS fecha_creacion " +
                     "FROM EnlaceTarea e " +
                     "LEFT JOIN Usuario u ON e.usuario_id = u.id " +
                     "WHERE e.tarea_id = ? " +
                     "ORDER BY e.fecha_creacion DESC, e.id DESC";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
            ps.setString(1, tareaId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new EnlaceTarea(
                            rs.getString("id"),
                            rs.getString("tarea_id"),
                            rs.getString("usuario_id"),
                            rs.getString("autor_nombre"),
                            rs.getString("titulo"),
                            rs.getString("url"),
                            rs.getString("fecha_creacion")
                    ));
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_COLAB", "Error al listar enlaces: " + e.getMessage());
        }
        return lista;
    }

    public boolean crearEnlace(String tareaId, String usuarioId, String titulo, String url) {
        if (tareaId == null || tareaId.trim().isEmpty()
                || usuarioId == null || usuarioId.trim().isEmpty()
                || titulo == null || titulo.trim().isEmpty()
                || url == null || url.trim().isEmpty()) {
            return false;
        }

        String urlLimpia = url.trim();
        String urlLower = urlLimpia.toLowerCase(Locale.ROOT);
        if (!urlLower.startsWith("http://") && !urlLower.startsWith("https://")) {
            return false;
        }

        String tituloLimpio = titulo.trim();
        if (tituloLimpio.length() > 120) tituloLimpio = tituloLimpio.substring(0, 120);
        if (urlLimpia.length() > 500) urlLimpia = urlLimpia.substring(0, 500);

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            String idEnl = generarIdSeguro(autoCloseCon, "EnlaceTarea", "id", "ENL", 6);
            String sql = "INSERT INTO EnlaceTarea (id, tarea_id, usuario_id, titulo, url) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
                ps.setString(1, idEnl);
                ps.setString(2, tareaId.trim());
                ps.setString(3, usuarioId.trim());
                ps.setString(4, tituloLimpio);
                ps.setString(5, urlLimpia);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_COLAB", "Error al crear enlace: " + e.getMessage());
            return false;
        }
    }

    // Solo el autor del enlace o el Líder del proyecto pueden eliminar el enlace
    public boolean eliminarEnlace(String enlaceId, String usuarioIdSolicitante, String proyectoId) {
        if (enlaceId == null || enlaceId.trim().isEmpty()
                || usuarioIdSolicitante == null || usuarioIdSolicitante.trim().isEmpty()) {
            return false;
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            String autorId = null;
            String proyectoReal = proyectoId;
            String sqlCheck = "SELECT e.usuario_id, t.proyecto_id " +
                              "FROM EnlaceTarea e INNER JOIN Tarea t ON e.tarea_id = t.id " +
                              "WHERE e.id = ?";
            try (PreparedStatement psC = autoCloseCon.prepareStatement(sqlCheck)) {
                psC.setString(1, enlaceId.trim());
                try (ResultSet rs = psC.executeQuery()) {
                    if (rs.next()) {
                        autorId = rs.getString("usuario_id");
                        proyectoReal = rs.getString("proyecto_id");
                    }
                }
            }

            if (autorId == null) return false;

            boolean esAutor = usuarioIdSolicitante.trim().equals(autorId);
            boolean esLider = new cEquipo().esLiderDelProyecto(autoCloseCon, proyectoReal, usuarioIdSolicitante.trim());
            if (!esAutor && !esLider) {
                return false;
            }

            String sqlDel = "DELETE FROM EnlaceTarea WHERE id = ?";
            try (PreparedStatement psDel = autoCloseCon.prepareStatement(sqlDel)) {
                psDel.setString(1, enlaceId.trim());
                return psDel.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_COLAB", "Error al eliminar enlace: " + e.getMessage());
            return false;
        }
    }

    // ==================== SUBTAREAS (CHECKLIST) ====================

    public List<Subtarea> listarSubtareasPorTarea(String tareaId) {
        List<Subtarea> lista = new ArrayList<>();
        if (tareaId == null || tareaId.trim().isEmpty()) return lista;

        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT id, tarea_id, titulo, completada, orden " +
                     "FROM Subtarea WHERE tarea_id = ? ORDER BY orden ASC, id ASC";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
            ps.setString(1, tareaId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Subtarea(
                            rs.getString("id"),
                            rs.getString("tarea_id"),
                            rs.getString("titulo"),
                            rs.getBoolean("completada"),
                            rs.getInt("orden")
                    ));
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_COLAB", "Error al listar subtareas: " + e.getMessage());
        }
        return lista;
    }

    public boolean crearSubtarea(String tareaId, String titulo) {
        if (tareaId == null || tareaId.trim().isEmpty()
                || titulo == null || titulo.trim().isEmpty()) {
            return false;
        }

        String tituloLimpio = titulo.trim();
        if (tituloLimpio.length() > 200) tituloLimpio = tituloLimpio.substring(0, 200);

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            int siguienteOrden = 1;
            String sqlOrden = "SELECT ISNULL(MAX(orden), 0) + 1 AS next_orden FROM Subtarea WHERE tarea_id = ?";
            try (PreparedStatement psO = autoCloseCon.prepareStatement(sqlOrden)) {
                psO.setString(1, tareaId.trim());
                try (ResultSet rs = psO.executeQuery()) {
                    if (rs.next()) {
                        siguienteOrden = rs.getInt("next_orden");
                    }
                }
            }

            String idSub = generarIdSeguro(autoCloseCon, "Subtarea", "id", "SUB", 6);
            String sql = "INSERT INTO Subtarea (id, tarea_id, titulo, completada, orden) VALUES (?, ?, ?, 0, ?)";
            try (PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
                ps.setString(1, idSub);
                ps.setString(2, tareaId.trim());
                ps.setString(3, tituloLimpio);
                ps.setInt(4, siguienteOrden);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_COLAB", "Error al crear subtarea: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarEstadoSubtarea(String subtareaId, boolean completada) {
        if (subtareaId == null || subtareaId.trim().isEmpty()) return false;

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String sql = "UPDATE Subtarea SET completada = ? WHERE id = ?";
        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
            ps.setBoolean(1, completada);
            ps.setString(2, subtareaId.trim());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            Log.e("CRUD_COLAB", "Error al actualizar subtarea: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarSubtarea(String subtareaId) {
        if (subtareaId == null || subtareaId.trim().isEmpty()) return false;

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String sql = "DELETE FROM Subtarea WHERE id = ?";
        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
            ps.setString(1, subtareaId.trim());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            Log.e("CRUD_COLAB", "Error al eliminar subtarea: " + e.getMessage());
            return false;
        }
    }
}
