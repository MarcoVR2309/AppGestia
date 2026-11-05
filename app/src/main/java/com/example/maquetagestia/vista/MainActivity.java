package com.example.maquetagestia.vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cDashboard;
import com.example.maquetagestia.model.DashboardDTO;

public class MainActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private ImageButton btnNotifications;
    private TextView lblKpiProyectos, lblKpiTareas, lblKpiCompletadas, lblKpiPendientes, lblPorcentajeProgreso;
    private ProgressBar progressBarGlobal;
    private ImageView btnAddProjectNav;
    private cDashboard dashboardControlador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Enlace de vistas
        tvGreeting = findViewById(R.id.tvGreeting);
        btnNotifications = findViewById(R.id.btnNotifications);
        lblKpiProyectos = findViewById(R.id.lblKpiProyectos);
        lblKpiTareas = findViewById(R.id.lblKpiTareas);
        lblKpiCompletadas = findViewById(R.id.lblKpiCompletadas);
        lblKpiPendientes = findViewById(R.id.lblKpiPendientes);
        lblPorcentajeProgreso = findViewById(R.id.lblPorcentajeProgreso);
        progressBarGlobal = findViewById(R.id.progressBarGlobal);
        btnAddProjectNav = findViewById(R.id.btnAddProjectNav);

        dashboardControlador = new cDashboard();

        // Saludo personalizado con el primer nombre
        String nombreCompleto = Sesion.getNombre(this);
        String primerNombre = nombreCompleto != null && !nombreCompleto.isEmpty()
                ? nombreCompleto.split(" ")[0]
                : "Usuario";
        if (tvGreeting != null) {
            tvGreeting.setText("Hola, " + primerNombre + " 👋");
        }

        // Listener del botón central de añadir proyecto
        if (btnAddProjectNav != null) {
            btnAddProjectNav.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, frm_proyectos.class);
                intent.putExtra("ABRIR_DIALOGO_CREAR", true);
                startActivity(intent);
            });
        }

        cargarMetricasDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarMetricasDashboard();
    }

    private void cargarMetricasDashboard() {
        String idUsuario = Sesion.getIdUsuario(this);
        if (idUsuario == null) return;

        new Thread(() -> {
            DashboardDTO dto = dashboardControlador.obtenerMetricasUsuario(idUsuario);

            runOnUiThread(() -> {
                if (lblKpiProyectos != null) lblKpiProyectos.setText(String.valueOf(dto.getProyectosActivos()));
                if (lblKpiTareas != null) lblKpiTareas.setText(String.valueOf(dto.getTareasAsignadas()));
                if (lblKpiCompletadas != null) lblKpiCompletadas.setText(String.valueOf(dto.getTareasCompletadas()));
                if (lblKpiPendientes != null) lblKpiPendientes.setText(String.valueOf(dto.getTareasPendientes()));
                if (progressBarGlobal != null) progressBarGlobal.setProgress(dto.getPorcentajeAvanceGlobal());
                if (lblPorcentajeProgreso != null) lblPorcentajeProgreso.setText(dto.getPorcentajeAvanceGlobal() + "% completado");
            });
        }).start();
    }

    public void abrirNotificaciones(View view) {
        Intent intent = new Intent(this, frm_notificaciones.class);
        startActivity(intent);
    }

    public void abrirPantallaProyectos(View view) {
        Intent intent = new Intent(this, frm_proyectos.class);
        startActivity(intent);
    }

    public void abrirEstadisticas(View view) {
        Intent intent = new Intent(this, frm_estadisticas.class);
        startActivity(intent);
    }

    public void abrirPerfil(View view) {
        Intent intent = new Intent(this, frm_perfil.class);
        startActivity(intent);
    }
}