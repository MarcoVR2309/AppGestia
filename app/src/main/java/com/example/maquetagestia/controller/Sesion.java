package com.example.maquetagestia.controller;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.maquetagestia.model.Usuario;

public class Sesion {

    private static final String PREF_NAME = "GestiaSession";
    private static final String CONFIG_PREF_NAME = "GestiaConfig";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USUARIO_ID = "idUsuario";
    private static final String KEY_NOMBRE = "nombreUsuario";
    private static final String KEY_CORREO = "correoUsuario";
    private static final String KEY_TELEFONO = "telefonoUsuario";
    private static final String KEY_TIPO_USUARIO = "tipoUsuario";
    private static final String KEY_NOTIF_ACTIVAS = "notificacionesActivas";
    private static final String KEY_MODO_OSCURO = "modoOscuro";

    private Sesion() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getConfigPrefs(Context context) {
        return context.getSharedPreferences(CONFIG_PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void guardar(Context context, Usuario usuario) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USUARIO_ID, usuario.getId());
        editor.putString(KEY_NOMBRE, usuario.getNombreCompleto());
        editor.putString(KEY_CORREO, usuario.getCorreo());
        editor.putString(KEY_TELEFONO, usuario.getTelefono() != null ? usuario.getTelefono() : "");
        editor.putString(KEY_TIPO_USUARIO, usuario.getTipoUsuario() != null ? usuario.getTipoUsuario() : "Estudiante");
        editor.putBoolean(KEY_NOTIF_ACTIVAS, usuario.isNotificacionesActivas());
        editor.apply();
    }

    public static boolean estaActiva(Context context) {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static String getIdUsuario(Context context) {
        return getPrefs(context).getString(KEY_USUARIO_ID, null);
    }

    public static String getNombre(Context context) {
        return getPrefs(context).getString(KEY_NOMBRE, "Usuario");
    }

    public static String getCorreo(Context context) {
        return getPrefs(context).getString(KEY_CORREO, "");
    }

    public static String getTelefono(Context context) {
        return getPrefs(context).getString(KEY_TELEFONO, "");
    }

    public static String getTipoUsuario(Context context) {
        return getPrefs(context).getString(KEY_TIPO_USUARIO, "Estudiante");
    }

    public static boolean isNotificacionesActivas(Context context) {
        return getPrefs(context).getBoolean(KEY_NOTIF_ACTIVAS, true);
    }

    public static void setNotificacionesActivas(Context context, boolean activas) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_ACTIVAS, activas).apply();
    }

    public static boolean isModoOscuro(Context context) {
        return getConfigPrefs(context).getBoolean(KEY_MODO_OSCURO, false);
    }

    public static void setModoOscuro(Context context, boolean oscuro) {
        getConfigPrefs(context).edit().putBoolean(KEY_MODO_OSCURO, oscuro).apply();
    }

    public static void actualizarPerfil(Context context, String nuevoNombre, String nuevoTelefono) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_NOMBRE, nuevoNombre);
        editor.putString(KEY_TELEFONO, nuevoTelefono);
        editor.apply();
    }

    public static void cerrar(Context context) {
        getPrefs(context).edit().clear().apply();
    }
}
