package com.example.maquetagestia.controller;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.maquetagestia.R;
import com.example.maquetagestia.conexion.conSQLServer;
import com.example.maquetagestia.vista.MainActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

public class AlertaVencimientoWorker extends Worker {

    public static final String CHANNEL_ID = "gestia_alertas_v2";
    private static final String PREFS_ALERTAS = "GestiaAlertasNotificadas";
    private static final String UNIQUE_PERIODIC_WORK = "gestia_alertas_worker";
    private static final String UNIQUE_IMMEDIATE_WORK = "gestia_alertas_inmediato";

    public AlertaVencimientoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void programarTrabajosAlerta(@NonNull Context context, boolean ejecutarInmediato) {
        Context appCtx = context.getApplicationContext();
        crearCanalNotificacion(appCtx);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodicRequest = new PeriodicWorkRequest.Builder(
                AlertaVencimientoWorker.class,
                15,
                TimeUnit.MINUTES
        ).setConstraints(constraints).build();

        WorkManager wm = WorkManager.getInstance(appCtx);
        wm.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
        );

        if (ejecutarInmediato) {
            OneTimeWorkRequest immediateRequest = new OneTimeWorkRequest.Builder(AlertaVencimientoWorker.class)
                    .setConstraints(constraints)
                    .build();
            wm.enqueueUniqueWork(
                    UNIQUE_IMMEDIATE_WORK,
                    ExistingWorkPolicy.REPLACE,
                    immediateRequest
            );
        }
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

            // 2. Tareas asignadas pendientes que vencen en los próximos 3 días (hora local SQL Server)
            String sqlTar = "SELECT t.id, t.titulo, " +
                    "CONVERT(VARCHAR(10), t.fecha_limite, 120) AS fecha_str, " +
                    "DATEDIFF(DAY, CAST(GETDATE() AS DATE), CAST(t.fecha_limite AS DATE)) AS dias_restantes " +
                    "FROM Tarea t " +
                    "INNER JOIN Tarea_Responsable tr ON t.id = tr.tarea_id " +
                    "WHERE tr.usuario_id = ? " +
                    "AND t.estado <> 'Finalizada' " +
                    "AND t.fecha_limite IS NOT NULL " +
                    "AND CAST(t.fecha_limite AS DATE) BETWEEN CAST(GETDATE() AS DATE) AND CAST(DATEADD(DAY, 3, GETDATE()) AS DATE)";
            try (PreparedStatement psTar = conn.prepareStatement(sqlTar)) {
                psTar.setString(1, idUsuario);
                try (ResultSet rs = psTar.executeQuery()) {
                    while (rs.next()) {
                        String idTarea = rs.getString("id");
                        String titulo = rs.getString("titulo");
                        int diasRestantes = rs.getInt("dias_restantes");
                        String clave = "TAR_" + idTarea + "_" + diasRestantes + "d";
                        if (prefs.contains(clave)) continue;

                        String tituloSeguro = !TextUtils.isEmpty(titulo) ? titulo : "Tarea pendiente";
                        String tituloNotif;
                        String mensajeNotif;
                        if (diasRestantes <= 0) {
                            tituloNotif = "¡Entregable vence hoy!";
                            mensajeNotif = "¡Tu entregable vence HOY! Revisa: " + tituloSeguro;
                        } else if (diasRestantes == 1) {
                            tituloNotif = "Entregable vence mañana";
                            mensajeNotif = "Te queda 1 día para completar tu entregable: " + tituloSeguro;
                        } else {
                            tituloNotif = "Entregable próximo a vencer";
                            mensajeNotif = "Te faltan " + diasRestantes + " días para completar tu entregable: " + tituloSeguro;
                        }

                        mostrarNotificacion(ctx, nm, clave.hashCode(), tituloNotif, mensajeNotif);
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
            channel.setDescription("Notificaciones de invitaciones a proyectos y entregables próximos a vencer");
            channel.enableVibration(true);
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttrs);
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
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        nm.notify(notificationId, builder.build());
    }
}
