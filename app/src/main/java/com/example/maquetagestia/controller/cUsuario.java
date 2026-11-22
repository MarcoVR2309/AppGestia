package com.example.maquetagestia.controller;

import android.util.Log;
import com.example.maquetagestia.conexion.conSQLServer; // Ajusta si tu clase se llama conexionSQLServer
import com.example.maquetagestia.model.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.UUID;

public class cUsuario {

    private final conSQLServer conexionHelper = new conSQLServer();

    // Cifrado de contraseña en texto plano con SHA-256 hexadecimal
    private static String hashSHA256(String password) {
        if (password == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e("CRUD_USUARIO", "Error al cifrar contraseña con SHA-256: " + e.getMessage());
            return password;
        }
    }


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
                // 1. Generar el ID con formato 'U000001'
                String idGenerado = generarIdSeguro(con, "Usuario", "id", "U", 6);
                u.setId(idGenerado);

                // 2. Aplicar hash SHA-256 a la contraseña antes de insertar
                String passwordHashed = hashSHA256(u.getPasswordHash());
                u.setPasswordHash(passwordHashed);

                // 3. Ejecutar la inserción incluyendo la columna ID y tipo_usuario
                String tipoUsuario = (u.getTipoUsuario() != null && !u.getTipoUsuario().trim().isEmpty())
                        ? u.getTipoUsuario().trim()
                        : "Estudiante";
                u.setTipoUsuario(tipoUsuario);

                String query = "INSERT INTO Usuario (id, nombre_completo, correo, password_hash, telefono, tipo_usuario) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(query)) {
                    ps.setString(1, u.getId());
                    ps.setString(2, u.getNombreCompleto());
                    ps.setString(3, u.getCorreo());
                    ps.setString(4, u.getPasswordHash());
                    ps.setString(5, u.getTelefono() != null ? u.getTelefono() : "");
                    ps.setString(6, tipoUsuario);

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

    // Estructura de resultado para autenticación
    public static class ResultadoLogin {
        public final Usuario usuario;
        public final String mensajeError;
        public final boolean esErrorConexion;

        public ResultadoLogin(Usuario usuario, String mensajeError, boolean esErrorConexion) {
            this.usuario = usuario;
            this.mensajeError = mensajeError;
            this.esErrorConexion = esErrorConexion;
        }
    }

    // Iniciar sesión con validación de credenciales y cierre seguro de conexión
    public ResultadoLogin iniciarSesionEx(String correo, String password) {
        Connection con = conexionHelper.conectar();
        if (con == null) {
            return new ResultadoLogin(null, "No se pudo conectar con el servidor SQL Server. Verifique su red.", true);
        }

        String query = "SELECT id, nombre_completo, correo, password_hash, telefono, avatar_url, notificaciones_activas, tipo_usuario " +
                       "FROM Usuario WHERE correo = ? AND password_hash = ?";

        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(query)) {

            String passwordHashed = hashSHA256(password);
            ps.setString(1, correo);
            ps.setString(2, passwordHashed);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario();
                    usuario.setId(rs.getString("id"));
                    usuario.setNombreCompleto(rs.getString("nombre_completo"));
                    usuario.setCorreo(rs.getString("correo"));
                    usuario.setPasswordHash(rs.getString("password_hash"));
                    usuario.setTelefono(rs.getString("telefono"));
                    usuario.setAvatarUrl(rs.getString("avatar_url"));
                    usuario.setNotificacionesActivas(rs.getBoolean("notificaciones_activas"));
                    usuario.setTipoUsuario(rs.getString("tipo_usuario"));
                    return new ResultadoLogin(usuario, null, false);
                } else {
                    return new ResultadoLogin(null, "Correo o contraseña incorrectos", false);
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_USUARIO", "Error en iniciarSesion: " + e.getMessage());
            return new ResultadoLogin(null, "Error al consultar la base de datos: " + e.getMessage(), true);
        }
    }

    // Restablecer contraseña validando coincidencia exacta de correo y teléfono registrado
    public boolean restablecerContrasena(String correo, String telefono, String nuevaPassword) {
        if (correo == null || correo.trim().isEmpty()
                || telefono == null || telefono.trim().isEmpty()
                || nuevaPassword == null || nuevaPassword.trim().length() < 6) {
            return false;
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String sqlBuscar = "SELECT id FROM Usuario WHERE correo = ? " +
                "AND LTRIM(RTRIM(ISNULL(telefono, ''))) = ? " +
                "AND LTRIM(RTRIM(ISNULL(telefono, ''))) <> ''";
        String sqlActualizar = "UPDATE Usuario SET password_hash = ?, updated_at = SYSUTCDATETIME() WHERE id = ?";

        try (Connection autoCloseCon = con) {
            String idUsuario = null;
            try (PreparedStatement ps = autoCloseCon.prepareStatement(sqlBuscar)) {
                ps.setString(1, correo.trim());
                ps.setString(2, telefono.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idUsuario = rs.getString("id");
                    }
                }
            }

            if (idUsuario == null) return false;

            try (PreparedStatement psUpd = autoCloseCon.prepareStatement(sqlActualizar)) {
                psUpd.setString(1, hashSHA256(nuevaPassword.trim()));
                psUpd.setString(2, idUsuario);
                return psUpd.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Log.e("CRUD_USUARIO", "Error al restablecer contraseña: " + e.getMessage());
            return false;
        }
    }

    // Cambiar contraseña del usuario autenticado validando su contraseña vigente
    public boolean cambiarContrasenaActual(String idUsuario, String passwordActual, String nuevaPassword) {
        if (idUsuario == null || idUsuario.trim().isEmpty()
                || passwordActual == null || passwordActual.trim().isEmpty()
                || nuevaPassword == null || nuevaPassword.trim().length() < 6) {
            return false;
        }

        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String sql = "UPDATE Usuario SET password_hash = ?, updated_at = SYSUTCDATETIME() WHERE id = ? AND password_hash = ?";
        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
            ps.setString(1, hashSHA256(nuevaPassword.trim()));
            ps.setString(2, idUsuario.trim());
            ps.setString(3, hashSHA256(passwordActual.trim()));
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            Log.e("CRUD_USUARIO", "Error al cambiar contraseña actual: " + e.getMessage());
            return false;
        }
    }

    // Actualizar preferencia de notificaciones activas en SQL Server
    public boolean actualizarNotificacionesActivas(String idUsuario, boolean activo) {
        if (idUsuario == null || idUsuario.trim().isEmpty()) return false;
        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String sql = "UPDATE Usuario SET notificaciones_activas = ?, updated_at = SYSUTCDATETIME() WHERE id = ?";
        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setString(2, idUsuario.trim());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            Log.e("CRUD_USUARIO", "Error al actualizar notificaciones_activas: " + e.getMessage());
            return false;
        }
    }

    // Autenticar o crear automáticamente usuario de Google en SQL Server
    public Usuario autenticarOCrearConGoogle(String correo, String nombreCompleto, String avatarUrl, String googleSub) {
        if (correo == null || correo.trim().isEmpty()) return null;

        Connection con = conexionHelper.conectar();
        if (con == null) return null;

        String correoLimpio = correo.trim();
        String avatarLimpio = (avatarUrl != null && !avatarUrl.trim().isEmpty()) ? avatarUrl.trim() : null;

        String sqlSelect = "SELECT id, nombre_completo, correo, password_hash, telefono, avatar_url, notificaciones_activas, tipo_usuario " +
                "FROM Usuario WHERE correo = ?";

        try (Connection autoCloseCon = con) {
            try (PreparedStatement ps = autoCloseCon.prepareStatement(sqlSelect)) {
                ps.setString(1, correoLimpio);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Usuario u = new Usuario();
                        u.setId(rs.getString("id"));
                        u.setNombreCompleto(rs.getString("nombre_completo"));
                        u.setCorreo(rs.getString("correo"));
                        u.setPasswordHash(rs.getString("password_hash"));
                        u.setTelefono(rs.getString("telefono"));
                        u.setAvatarUrl(rs.getString("avatar_url"));
                        u.setNotificacionesActivas(rs.getBoolean("notificaciones_activas"));
                        u.setTipoUsuario(rs.getString("tipo_usuario"));

                        if ((u.getAvatarUrl() == null || u.getAvatarUrl().trim().isEmpty()) && avatarLimpio != null) {
                            String sqlAvatar = "UPDATE Usuario SET avatar_url = ?, updated_at = SYSUTCDATETIME() WHERE id = ?";
                            try (PreparedStatement psAv = autoCloseCon.prepareStatement(sqlAvatar)) {
                                psAv.setString(1, avatarLimpio);
                                psAv.setString(2, u.getId());
                                psAv.executeUpdate();
                                u.setAvatarUrl(avatarLimpio);
                            }
                        }
                        return u;
                    }
                }
            }

            // Si no existe, crear nuevo usuario con prefijo U y tipo_usuario Estudiante
            String idGenerado = generarIdSeguro(autoCloseCon, "Usuario", "id", "U", 6);
            String prefijoCorreo = correoLimpio.contains("@")
                    ? correoLimpio.substring(0, correoLimpio.indexOf('@'))
                    : correoLimpio;
            String nombreFinal = (nombreCompleto != null && !nombreCompleto.trim().isEmpty())
                    ? nombreCompleto.trim()
                    : prefijoCorreo;
            String passwordSeguro = hashSHA256("GOOGLE_OAUTH_" + (googleSub != null ? googleSub : "") + "_" + UUID.randomUUID());

            String sqlInsert = "INSERT INTO Usuario (id, nombre_completo, correo, password_hash, avatar_url, telefono, notificaciones_activas, tipo_usuario) " +
                    "VALUES (?, ?, ?, ?, ?, NULL, 1, ?)";
            try (PreparedStatement psIns = autoCloseCon.prepareStatement(sqlInsert)) {
                psIns.setString(1, idGenerado);
                psIns.setString(2, nombreFinal);
                psIns.setString(3, correoLimpio);
                psIns.setString(4, passwordSeguro);
                if (avatarLimpio != null) {
                    psIns.setString(5, avatarLimpio);
                } else {
                    psIns.setNull(5, java.sql.Types.NVARCHAR);
                }
                psIns.setString(6, "Estudiante");

                if (psIns.executeUpdate() > 0) {
                    Usuario nuevo = new Usuario();
                    nuevo.setId(idGenerado);
                    nuevo.setNombreCompleto(nombreFinal);
                    nuevo.setCorreo(correoLimpio);
                    nuevo.setPasswordHash(passwordSeguro);
                    nuevo.setAvatarUrl(avatarLimpio);
                    nuevo.setTelefono(null);
                    nuevo.setNotificacionesActivas(true);
                    nuevo.setTipoUsuario("Estudiante");
                    return nuevo;
                }
            }
        } catch (Exception e) {
            Log.e("CRUD_USUARIO", "Error en autenticarOCrearConGoogle: " + e.getMessage());
        }
        return null;
    }

    // Actualizar nombre completo y teléfono del perfil del usuario
    public boolean actualizarPerfil(String idUsuario, String nuevoNombre, String nuevoTelefono) {
        Connection con = conexionHelper.conectar();
        if (con == null) return false;

        String sql = "UPDATE Usuario SET nombre_completo = ?, telefono = ?, updated_at = SYSUTCDATETIME() WHERE id = ?";
        try (Connection autoCloseCon = con;
             PreparedStatement ps = autoCloseCon.prepareStatement(sql)) {

            ps.setString(1, nuevoNombre);
            ps.setString(2, nuevoTelefono != null ? nuevoTelefono : "");
            ps.setString(3, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            Log.e("CRUD_USUARIO", "Error al actualizar perfil: " + e.getMessage());
            return false;
        }
    }
}