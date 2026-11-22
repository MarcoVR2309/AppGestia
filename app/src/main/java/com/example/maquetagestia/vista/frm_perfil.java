package com.example.maquetagestia.vista;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cDashboard;
import com.example.maquetagestia.controller.cUsuario;
import com.example.maquetagestia.model.DashboardDTO;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class frm_perfil extends AppCompatActivity {

    private TextView tvNombreUsuario, tvEmailUsuario, lblPerfilProyectos, lblPerfilTareas;
    private cUsuario usuarioControlador;
    private cDashboard dashboardControlador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_perfil);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        tvEmailUsuario = findViewById(R.id.tvEmailUsuario);
        lblPerfilProyectos = findViewById(R.id.lblPerfilProyectos);
        lblPerfilTareas = findViewById(R.id.lblPerfilTareas);
        usuarioControlador = new cUsuario();
        dashboardControlador = new cDashboard();

        cargarDatosSesion();
        cargarResumenPerfil();

        View cardEditarPerfil = findViewById(R.id.cardEditarPerfil);
        if (cardEditarPerfil != null) {
            cardEditarPerfil.setOnClickListener(this::editarPerfil);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosSesion();
        cargarResumenPerfil();
    }

    private void cargarResumenPerfil() {
        String idUsuario = Sesion.getIdUsuario(this);
        if (idUsuario == null || idUsuario.isEmpty()) return;

        new Thread(() -> {
            DashboardDTO dto = dashboardControlador.obtenerMetricasUsuario(idUsuario);
            runOnUiThread(() -> {
                if (lblPerfilProyectos != null) {
                    lblPerfilProyectos.setText(String.valueOf(dto.getProyectosActivos()));
                }
                if (lblPerfilTareas != null) {
                    lblPerfilTareas.setText(String.valueOf(dto.getTareasAsignadas()));
                }
            });
        }).start();
    }

    private void cargarDatosSesion() {
        if (tvNombreUsuario != null) {
            tvNombreUsuario.setText(Sesion.getNombre(this));
        }
        if (tvEmailUsuario != null) {
            tvEmailUsuario.setText(Sesion.getCorreo(this));
        }
    }

    public void editarPerfil(View view) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Editar perfil");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2);

        EditText etNombre = new EditText(this);
        etNombre.setHint("Nombre completo");
        etNombre.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etNombre.setText(Sesion.getNombre(this));
        layout.addView(etNombre);

        EditText etTelefono = new EditText(this);
        etTelefono.setHint("Teléfono");
        etTelefono.setInputType(InputType.TYPE_CLASS_PHONE);
        etTelefono.setText(Sesion.getTelefono(this));
        LinearLayout.LayoutParams paramsTel = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsTel.topMargin = paddingPx / 2;
        etTelefono.setLayoutParams(paramsTel);
        layout.addView(etTelefono);

        builder.setView(layout);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoNombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
            String nuevoTelefono = etTelefono.getText() != null ? etTelefono.getText().toString().trim() : "";

            if (TextUtils.isEmpty(nuevoNombre)) {
                Toast.makeText(this, "El nombre completo es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            String idUsuario = Sesion.getIdUsuario(this);
            if (idUsuario == null || idUsuario.isEmpty()) {
                Toast.makeText(this, "No se encontró una sesión de usuario válida", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                boolean exito = usuarioControlador.actualizarPerfil(idUsuario, nuevoNombre, nuevoTelefono);
                runOnUiThread(() -> {
                    if (exito) {
                        Sesion.actualizarPerfil(frm_perfil.this, nuevoNombre, nuevoTelefono);
                        cargarDatosSesion();
                        Toast.makeText(frm_perfil.this, "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(frm_perfil.this, "Error al actualizar el perfil en SQL Server", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void volverAlHome(View view) {
        finish();
    }

    public void abrirConfiguracion(View view) {
        Intent intent = new Intent(this, frm_configuracion.class);
        startActivity(intent);
    }

    public void abrirProyectosPerfil(View view) {
        Intent intent = new Intent(this, frm_proyectos.class);
        startActivity(intent);
    }

    public void abrirTareasPerfil(View view) {
        Intent intent = new Intent(this, frm_tareas.class);
        intent.putExtra("ESTADO", "TODAS");
        startActivity(intent);
    }

    public void abrirNotificaciones(View view) {
        Intent intent = new Intent(this, frm_notificaciones.class);
        startActivity(intent);
    }

    public void cerrarSesion(View view) {
        Sesion.cerrar(this);
        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, frm_login.class);
        startActivity(intent);
        finishAffinity();
    }
}