package com.example.maquetagestia.controller;

import android.util.Log;

import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.model.Notificacion;
import com.example.maquetagestia.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class cEquipo {

    private final conSQLServer conexionHelper = new conSQLServer();

    // DTO para representar a un miembro del equipo con su ID de relación, Usuario y Rol en el proyecto
    public static class MiembroEquipoItem {
        public String idRelacion;
        public final Usuario usuario;
        public String rol;

        public MiembroEquipoItem(Usuario usuario, String rol) {
            this(null, usuario, rol);
        }

        public MiembroEquipoItem(String idRelacion, Usuario usuario, String rol) {
            this.idRelacion = idRelacion;
            this.usuario = usuario;
            this.rol = rol;
        }

        public String getIdRelacion() {
            return idRelacion;
        }

        public void setIdRelacion(String idRelacion) {
            this.idRelacion = idRelacion;
        }

        public Usuario getUsuario() {
            return usuario;
        }

        public String getRol() {
            return rol;
        }

        public void setRol(String rol) {
            this.rol = rol;
        }
    }

    public static boolean esRolLider(String rol) {
        if (rol == null) return false;
        String limpio = Normalizer.normalize(rol.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return "lider".equals(limpio);
    }

    private String normalizarRol(String rol) {
        if (rol == null || rol.trim().isEmpty()) return "Colaborador";
        String r = rol.trim();
        if (esRolLider(r)) return "Lider";
        if ("Observador".equalsIgnoreCase(r)) return "Observador";
        return "Colaborador";
    }

    // Listar miembros del equipo por proyecto mediante INNER JOIN
    public List<MiembroEquipoItem> listarEquipoPorProyecto(String idProyecto) {
        List<MiembroEquipoItem> lista = new ArrayList<>();
        Connection con = conexionHelper.conectar();
        if (con == null) return lista;

        String sql = "SELECT pu.id AS id_relacion, u.id, u.nombre_completo, u.correo, u.telefono, u.avatar_url, pu.rol " +
                     "FROM Usuario u INNER JOIN Proyecto_Usuario pu ON u.id = pu.usuario_id " +
                     "WHERE pu.proyecto_id = ?";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, idProyecto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String idRelacion = rs.getString("id_relacion");
                    Usuario u = new Usuario();
                    u.setId(rs.getString("id"));
                    u.setNombreCompleto(rs.getString("nombre_completo"));
                    u.setCorreo(rs.getString("correo"));
                    u.setTelefono(rs.getString("telefono"));
                    u.setAvatarUrl(rs.getString("avatar_url"));
                    String rol = rs.getString("rol");
                    lista.add(new MiembroEquipoItem(idRelacion, u, rol));
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error al listar equipo por proyecto: " + e.getMessage());
        }
        return lista;
    }

    // Consultar el rol almacenado en Proyecto_Usuario para el usuario en sesión
    public String obtenerRolUsuarioEnProyecto(Connection con, String idProyecto, String idUsuario) {
        if (con == null || idProyecto == null || idProyecto.trim().isEmpty()
                || idUsuario == null || idUsuario.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT TOP 1 rol FROM Proyecto_Usuario WHERE proyecto_id = ? AND usuario_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idProyecto.trim());
            ps.setString(2, idUsuario.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("rol");
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error al obtener rol del usuario: " + e.getMessage());
        }
        return null;
    }

    public String obtenerRolUsuarioEnProyecto(String idProyecto, String idUsuario) {
        Connection con = conexionHelper.conectar();
        if (con == null) return null;
        try (Connection autoCloseCon = con) {
            return obtenerRolUsuarioEnProyecto(autoCloseCon, idProyecto, idUsuario);
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error en obtenerRolUsuarioEnProyecto: " + e.getMessage());
            return null;
        }
    }

    public boolean esLiderDelProyecto(Connection con, String idProyecto, String idUsuario) {
        if (con == null || idProyecto == null || idProyecto.trim().isEmpty()
                || idUsuario == null || idUsuario.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM Proyecto_Usuario " +
                "WHERE proyecto_id = ? AND usuario_id = ? " +
                "AND LOWER(LTRIM(RTRIM(rol))) IN (N'lider', N'líder')";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idProyecto.trim());
            ps.setString(2, idUsuario.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error al verificar líder en SQL: " + e.getMessage());
        }
        return esRolLider(obtenerRolUsuarioEnProyecto(con, idProyecto, idUsuario));
    }

    public boolean esLiderDelProyecto(String idProyecto, String idUsuario) {
        Connection con = conexionHelper.conectar();
        if (con == null) return false;
        try (Connection autoCloseCon = con) {
            return esLiderDelProyecto(autoCloseCon, idProyecto, idUsuario);
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error en esLiderDelProyecto: " + e.getMessage());
            return false;
        }
    }

    // Actualizar rol de un miembro en Proyecto_Usuario (validando rol de Líder si se indica solicitante)
    public boolean actualizarRol(String idRelacion, String nuevoRol) {
        return actualizarRol(idRelacion, nuevoRol, null);
    }

    public boolean actualizarRol(String idRelacion, String nuevoRol, String usuarioIdSolicitante) {
        if (idRelacion == null || idRelacion.trim().isEmpty()) return false;
        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String rolNormalizado = normalizarRol(nuevoRol);

        try (Connection autoCloseCon = con) {
            if (usuarioIdSolicitante != null && !usuarioIdSolicitante.trim().isEmpty()) {
                String proyectoId = obtenerProyectoPorRelacion(autoCloseCon, idRelacion.trim());
                if (proyectoId == null || !esLiderDelProyecto(autoCloseCon, proyectoId, usuarioIdSolicitante.trim())) {
                    return false;
                }
            }

            String sql = "UPDATE Proyecto_Usuario SET rol = ? WHERE id = ?";
            try (PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
                ps.setString(1, rolNormalizado);
                ps.setString(2, idRelacion.trim());
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error al actualizar rol del miembro: " + e.getMessage());
            return false;
        }
    }

    // Eliminar un miembro de Proyecto_Usuario (validando rol de Líder si se indica solicitante)
    public boolean eliminarMiembro(String idRelacion) {
        return eliminarMiembro(idRelacion, null);
    }

    public boolean eliminarMiembro(String idRelacion, String usuarioIdSolicitante) {
        if (idRelacion == null || idRelacion.trim().isEmpty()) return false;
        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            if (usuarioIdSolicitante != null && !usuarioIdSolicitante.trim().isEmpty()) {
                String proyectoId = obtenerProyectoPorRelacion(autoCloseCon, idRelacion.trim());
                if (proyectoId == null || !esLiderDelProyecto(autoCloseCon, proyectoId, usuarioIdSolicitante.trim())) {
                    return false;
                }
            }

            String sql = "DELETE FROM Proyecto_Usuario WHERE id = ?";
            try (PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
                ps.setString(1, idRelacion.trim());
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error al eliminar miembro del proyecto: " + e.getMessage());
            return false;
        }
    }

    private String obtenerProyectoPorRelacion(Connection con, String idRelacion) {
        String sql = "SELECT proyecto_id FROM Proyecto_Usuario WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idRelacion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("proyecto_id");
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error obteniendo proyecto por relación: " + e.getMessage());
        }
        return null;
    }

    // Enviar invitación al proyecto mediante Notificación (sin insertar aún en Proyecto_Usuario)
    public boolean agregarMiembroEquipo(String idProyecto, String idUsuario, String rol) {
        return agregarMiembroEquipo(idProyecto, idUsuario, rol, null);
    }

    public boolean agregarMiembroEquipo(String idProyecto, String idUsuario, String rol, String usuarioIdSolicitante) {
        if (idProyecto == null || idProyecto.trim().isEmpty()
                || idUsuario == null || idUsuario.trim().isEmpty()) {
            return false;
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            if (usuarioIdSolicitante != null && !usuarioIdSolicitante.trim().isEmpty()
                    && !esLiderDelProyecto(autoCloseCon, idProyecto.trim(), usuarioIdSolicitante.trim())) {
                return false;
            }
            // 1. Buscar ID del usuario por su ID o correo
            String sqlBusqueda = "SELECT id FROM Usuario WHERE id = ? OR correo = ?";
            String idUsuarioDestino = null;
            try (PreparedStatement psB = autoCloseCon.prepareStatement(sqlBusqueda)) {
                psB.setString(1, idUsuario.trim());
                psB.setString(2, idUsuario.trim());
                try (ResultSet rsB = psB.executeQuery()) {
                    if (rsB.next()) {
                        idUsuarioDestino = rsB.getString("id");
                    }
                }
            }

            if (idUsuarioDestino == null) return false; // Usuario no registrado en la base de datos

            // 2. Verificar que no pertenezca ya al proyecto
            String sqlExiste = "SELECT COUNT(*) FROM Proyecto_Usuario WHERE proyecto_id = ? AND usuario_id = ?";
            try (PreparedStatement psE = autoCloseCon.prepareStatement(sqlExiste)) {
                psE.setString(1, idProyecto.trim());
                psE.setString(2, idUsuarioDestino);
                try (ResultSet rsE = psE.executeQuery()) {
                    if (rsE.next() && rsE.getInt(1) > 0) {
                        return false;
                    }
                }
            }

            // 3. Enviar notificación de invitación al usuario
            String rolAsignado = normalizarRol(rol);
            cNotificacion notificacionControlador = new cNotificacion();
            Notificacion notificacion = new Notificacion(
                    idUsuarioDestino,
                    "Invitación a Proyecto",
                    rolAsignado,
                    "Invitacion",
                    idProyecto.trim()
            );
            return notificacionControlador.crearNotificacion(autoCloseCon, notificacion);
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error al enviar invitación de equipo: " + e.getMessage());
            return false;
        }
    }

    // Confirmar invitación insertando al usuario en Proyecto_Usuario con ID prefijo PU
    public boolean confirmarInvitacion(String idProyecto, String idUsuario, String rol) {
        if (idProyecto == null || idProyecto.trim().isEmpty()
                || idUsuario == null || idUsuario.trim().isEmpty()) {
            return false;
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        try (Connection autoCloseCon = con) {
            // Evitar duplicados si el usuario ya pertenece al proyecto
            String sqlExiste = "SELECT COUNT(*) FROM Proyecto_Usuario WHERE proyecto_id = ? AND usuario_id = ?";
            try (PreparedStatement psE = autoCloseCon.prepareStatement(sqlExiste)) {
                psE.setString(1, idProyecto.trim());
                psE.setString(2, idUsuario.trim());
                try (ResultSet rsE = psE.executeQuery()) {
                    if (rsE.next() && rsE.getInt(1) > 0) {
                        return true;
                    }
                }
            }

            cProyecto controllerPry = new cProyecto();
            String idPu = controllerPry.generarIdSeguro(autoCloseCon, "Proyecto_Usuario", "id", "PU", 6);
            String rolAsignado = normalizarRol(rol);

            String sqlInsert = "INSERT INTO Proyecto_Usuario (id, proyecto_id, usuario_id, rol) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psI = autoCloseCon.prepareStatement(sqlInsert)) {
                psI.setString(1, idPu);
                psI.setString(2, idProyecto.trim());
                psI.setString(3, idUsuario.trim());
                psI.setString(4, rolAsignado);
                return psI.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_EQUIPO", "Error al confirmar invitación de equipo: " + e.getMessage());
            return false;
        }
    }
}
