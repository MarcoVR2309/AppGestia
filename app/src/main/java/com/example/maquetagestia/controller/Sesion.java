package com.example.maquetagestia.controller;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.maquetagestia.model.Usuario;

public class Sesion {

    private static final String PREF_NAME = "GestiaSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USUARIO_ID = "idUsuario";
    private static final String KEY_NOMBRE = "nombreUsuario";
    private static final String KEY_CORREO = "correoUsuario";
    private static final String KEY_TELEFONO = "telefonoUsuario";

    private Sesion() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void guardar(Context context, Usuario usuario) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USUARIO_ID, usuario.getId());
        editor.putString(KEY_NOMBRE, usuario.getNombreCompleto());
        editor.putString(KEY_CORREO, usuario.getCorreo());
        editor.putString(KEY_TELEFONO, usuario.getTelefono());
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

    public static void cerrar(Context context) {
        getPrefs(context).edit().clear().apply();
    }
}
