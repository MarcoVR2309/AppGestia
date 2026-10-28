package com.example.maquetagestia.conexion;

import android.os.StrictMode;
import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;

public class conSQLServer {

    // PARÁMETROS DE CONEXIÓN
    // - para app en el Emulador de Android Studio: usa "10.0.2.2" (localhost de mi pc).
    // - para app en un Celular físico por WiFi: IP local de tu pc (ej. "192.168.1.15").
    private static final String IP = "10.0.2.2";
    private static final String PUERTO = "1433";
    private static final String BASE_DATOS = "GESTIA";
    private static final String USUARIO = "sa";
    private static final String PASSWORD = "Admin12345";

    public Connection conectar() {
        Connection conexion = null;
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            Class.forName("net.sourceforge.jtds.jdbc.Driver");

            String url = "jdbc:jtds:sqlserver://" + IP + ":" + PUERTO + "/" + BASE_DATOS;
            conexion = DriverManager.getConnection(url, USUARIO, PASSWORD);

            Log.d("SQLSERVER_OK", "Conexión establecida exitosamente con la base de datos Gestia.");
        } catch (Exception e) {
            Log.e("SQLSERVER_ERROR", "Fallo al conectar con SQL Server: " + e.getMessage());
        }
        return conexion;
    }
}