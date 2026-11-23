package com.example.maquetagestia.controller;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.maquetagestia.R;
import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.vista.MainActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AlertaVencimientoWorker extends Worker {

    public static final String CHANNEL_ID = "gestia_alertas";
    private static final String PREFS_ALERTAS = "GestiaAlertasNotificadas";

    public AlertaVencimientoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        if (!Sesion.estaActiva(ctx) || !Sesion.isNotificacionesActivas(ctx)) {
            return Result.success();
        }
        String idUsuario = Sesion.getIdUsuario(ctx);
        if (TextUtils.isEmpty(idUsuario)) {
            return Result.success();
        }

        crearCanalNotificacion(ctx);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return Result.success();
        }

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_ALERTAS, Context.MODE_PRIVATE);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return Result.success();
        }

        conSQLServer db = new conSQLServer();
        try (Connection conn = db.conectar()) {
            if (conn == null) {
                return Result.retry();
            }

            // 1. Invitaciones pendientes no leídas
            String sqlInv = "SELECT id, titulo, mensaje FROM Notificacion " +
                    "WHERE usuario_id = ? AND leida = 0 AND tipo = 'Invitacion'";
            try (PreparedStatement psInv = conn.prepareStatement(sqlInv)) {
                psInv.setString(1, idUsuario);
                try (ResultSet rs = psInv.executeQuery()) {
                    while (rs.next()) {
                        String idNotif = rs.getString("id");
                        String clave = "INV_" + idNotif;
                        if (prefs.contains(clave)) continue;

                        String titulo = rs.getString("titulo");
                        String mensaje = rs.getString("mensaje");
                        mostrarNotificacion(
                                ctx,
                                nm,
                                clave.hashCode(),
                                !TextUtils.isEmpty(titulo) ? titulo : "Nueva invitación a proyecto",
                                !TextUtils.isEmpty(mensaje) ? mensaje : "Tienes una invitación pendiente en Gestia."
                        );
                        prefs.edit().putBoolean(clave, true).apply();
                    }
                }
            }

            // 2. Tareas asignadas pendientes que vencen hoy o mañana (hora local SQL Server)
            String sqlTar = "SELECT t.id, t.titulo, CONVERT(VARCHAR(10), t.fecha_limite, 120) AS fecha_str " +
                    "FROM Tarea t " +
                    "INNER JOIN Tarea_Responsable tr ON t.id = tr.tarea_id " +
                    "WHERE tr.usuario_id = ? " +
                    "AND t.estado <> 'Finalizada' " +
                    "AND t.fecha_limite IS NOT NULL " +
                    "AND CAST(t.fecha_limite AS DATE) BETWEEN CAST(GETDATE() AS DATE) AND CAST(DATEADD(DAY, 1, GETDATE()) AS DATE)";
            try (PreparedStatement psTar = conn.prepareStatement(sqlTar)) {
                psTar.setString(1, idUsuario);
                try (ResultSet rs = psTar.executeQuery()) {
                    while (rs.next()) {
                        String idTarea = rs.getString("id");
                        String titulo = rs.getString("titulo");
                        String fechaStr = rs.getString("fecha_str");
                        String clave = "TAR24H_" + idTarea + "_" + (fechaStr != null ? fechaStr : "");
                        if (prefs.contains(clave)) continue;

                        mostrarNotificacion(
                                ctx,
                                nm,
                                clave.hashCode(),
                                "Tarea próxima a vencer",
                                (!TextUtils.isEmpty(titulo) ? titulo : "Tarea pendiente")
                                        + (TextUtils.isEmpty(fechaStr) ? "" : " (Límite: " + fechaStr + ")")
                        );
                        prefs.edit().putBoolean(clave, true).apply();
                    }
                }
            }

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    public static void crearCanalNotificacion(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alertas de Gestia",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones de invitaciones a proyectos y tareas próximas a vencer");
            nm.createNotificationChannel(channel);
        }
    }

    private void mostrarNotificacion(@NonNull Context ctx, @NonNull NotificationManager nm,
                                     int notificationId, @NonNull String titulo, @NonNull String mensaje) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                ctx,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        nm.notify(notificationId, builder.build());
    }
}
