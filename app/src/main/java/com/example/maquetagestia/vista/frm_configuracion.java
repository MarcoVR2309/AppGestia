package com.example.maquetagestia.vista;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cUsuario;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class frm_configuracion extends AppCompatActivity {

    private SwitchMaterial switchPush;
    private SwitchMaterial switchDark;
    private cUsuario usuarioControlador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_configuracion);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usuarioControlador = new cUsuario();
        switchPush = findViewById(R.id.switchPush);
        switchDark = findViewById(R.id.switchDark);

        // Cargar estado guardado ANTES de asignar listeners
        switchPush.setChecked(Sesion.isNotificacionesActivas(this));
        switchDark.setChecked(Sesion.isModoOscuro(this));

        switchPush.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            Sesion.setNotificacionesActivas(frm_configuracion.this, isChecked);
            Toast.makeText(
                    frm_configuracion.this,
                    isChecked ? "Notificaciones activadas" : "Notificaciones desactivadas",
                    Toast.LENGTH_SHORT
            ).show();

            String idUsuario = Sesion.getIdUsuario(frm_configuracion.this);
            if (idUsuario != null && !idUsuario.isEmpty()) {
                new Thread(() -> usuarioControlador.actualizarNotificacionesActivas(idUsuario, isChecked)).start();
            }
        });

        switchDark.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            Sesion.setModoOscuro(frm_configuracion.this, isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
    }

    public void volverAlPerfil(View view) {
        finish();
    }

    public void abrirAjustesBateriaNotificaciones(View view) {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallback.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(fallback);
            } catch (Exception ex) {
                Toast.makeText(this, "No se pudo abrir la configuración del dispositivo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void mostrarDialogoIdioma(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Idioma")
                .setMessage("Idioma activo: Español (Perú)")
                .setPositiveButton("Aceptar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void mostrarDialogoSeguridad(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Seguridad y privacidad")
                .setMessage("Tu cuenta utiliza cifrado SHA-256 para las credenciales almacenadas en SQL Server y tu sesión se resguarda localmente en almacenamiento privado del dispositivo.")
                .setPositiveButton("Cambiar contraseña", (dialog, which) -> abrirDialogoCambiarContrasena())
                .setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void abrirDialogoCambiarContrasena() {
        String idUsuario = Sesion.getIdUsuario(this);
        if (idUsuario == null || idUsuario.isEmpty()) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Cambiar contraseña");

        int paddingPx = Math.round(20 * getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2);

        EditText txtActual = new EditText(this);
        txtActual.setHint("Contraseña actual");
        txtActual.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(txtActual);

        EditText txtNueva = new EditText(this);
        txtNueva.setHint("Nueva contraseña (mínimo 6 caracteres)");
        txtNueva.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(txtNueva);

        EditText txtConfirmar = new EditText(this);
        txtConfirmar.setHint("Confirmar nueva contraseña");
        txtConfirmar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(txtConfirmar);

        builder.setView(layout);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String actual = txtActual.getText() != null ? txtActual.getText().toString().trim() : "";
            String nueva = txtNueva.getText() != null ? txtNueva.getText().toString().trim() : "";
            String confirmar = txtConfirmar.getText() != null ? txtConfirmar.getText().toString().trim() : "";

            if (TextUtils.isEmpty(actual) || TextUtils.isEmpty(nueva) || TextUtils.isEmpty(confirmar)) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (nueva.length() < 6) {
                Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!nueva.equals(confirmar)) {
                Toast.makeText(this, "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                boolean exito = usuarioControlador.cambiarContrasenaActual(idUsuario, actual, nueva);
                runOnUiThread(() -> {
                    if (exito) {
                        Toast.makeText(frm_configuracion.this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(frm_configuracion.this, "La contraseña actual no coincide o hubo un error", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void abrirDatosPersonales(View view) {
        finish();
    }

    public void mostrarAcercaDe(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Acerca de Gestia")
                .setMessage("MaquetaGestia v1.0.0\n\n"
                        + "Arquitectura: Android Java (MVC) conectado a SQL Server mediante jTDS.\n\n"
                        + "Desarrollado por el equipo UPN para la gestión integral de proyectos, fases, tareas y equipos de trabajo.")
                .setPositiveButton("Aceptar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void mostrarTerminosConfig(View view) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Términos y Condiciones de Gestia")
                .setMessage("1. Uso de la plataforma: MaquetaGestia está destinada a la gestión de proyectos académicos y empresariales, planificación de fases y seguimiento de tareas en equipo.\n\n"
                        + "2. Privacidad y credenciales: Tus contraseñas se almacenan cifradas mediante SHA-256 en SQL Server y tu sesión local se guarda exclusivamente en tu dispositivo.\n\n"
                        + "3. Responsabilidad: El usuario es responsable de la veracidad de los datos registrados y de la colaboración dentro de sus proyectos asignados.")
                .setPositiveButton("Aceptar", (dialog, which) -> dialog.dismiss())
                .show();
    }
}