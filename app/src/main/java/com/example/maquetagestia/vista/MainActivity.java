package com.example.maquetagestia.vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Toast;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cDashboard;
import com.example.maquetagestia.controller.cProyecto;
import com.example.maquetagestia.model.DashboardDTO;
import com.example.maquetagestia.model.Proyecto;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private ImageButton btnNotifications;
    private ScrollView scrollDashboard;
    private TextView lblKpiProyectos, lblKpiTareas, lblKpiCompletadas, lblKpiPendientes, lblPorcentajeProgreso;
    private ProgressBar progressBarGlobal;
    private ImageView btnAddProjectNav;
    private RecyclerView rcvProyectosRecientes;
    private ProyectoAdapter proyectosRecientesAdapter;
    private cDashboard dashboardControlador;
    private cProyecto proyectoControlador;
    private int tareasAsignadasCount = 0;
    private int tareasCompletadasCount = 0;
    private int tareasPendientesCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Sesion.estaActiva(this)) {
            startActivity(new Intent(this, frm_login.class));
            finish();
            return;
        }

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
        scrollDashboard = findViewById(R.id.scrollDashboard);
        lblKpiProyectos = findViewById(R.id.lblKpiProyectos);
        lblKpiTareas = findViewById(R.id.lblKpiTareas);
        lblKpiCompletadas = findViewById(R.id.lblKpiCompletadas);
        lblKpiPendientes = findViewById(R.id.lblKpiPendientes);
        lblPorcentajeProgreso = findViewById(R.id.lblPorcentajeProgreso);
        progressBarGlobal = findViewById(R.id.progressBarGlobal);
        btnAddProjectNav = findViewById(R.id.btnAddProjectNav);
        rcvProyectosRecientes = findViewById(R.id.rcvProyectosRecientes);

        if (rcvProyectosRecientes != null) {
            rcvProyectosRecientes.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        }

        dashboardControlador = new cDashboard();
        proyectoControlador = new cProyecto();

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
            List<Proyecto> proyectosRecientes = proyectoControlador.listarPorUsuario(idUsuario, "Todos");

            runOnUiThread(() -> {
                tareasAsignadasCount = dto.getTareasAsignadas();
                tareasCompletadasCount = dto.getTareasCompletadas();
                tareasPendientesCount = dto.getTareasPendientes();

                if (lblKpiProyectos != null) lblKpiProyectos.setText(String.valueOf(dto.getProyectosActivos()));
                if (lblKpiTareas != null) lblKpiTareas.setText(String.valueOf(dto.getTareasAsignadas()));
                if (lblKpiCompletadas != null) lblKpiCompletadas.setText(String.valueOf(dto.getTareasCompletadas()));
                if (lblKpiPendientes != null) lblKpiPendientes.setText(String.valueOf(dto.getTareasPendientes()));
                if (progressBarGlobal != null) progressBarGlobal.setProgress(dto.getPorcentajeAvanceGlobal());
                if (lblPorcentajeProgreso != null) lblPorcentajeProgreso.setText(dto.getPorcentajeAvanceGlobal() + "% completado");

                if (rcvProyectosRecientes != null) {
                    proyectosRecientesAdapter = new ProyectoAdapter(MainActivity.this, proyectosRecientes);
                    rcvProyectosRecientes.setAdapter(proyectosRecientesAdapter);
                }
            });
        }).start();
    }

    public void abrirTareasAsignadas(View view) {
        if (tareasAsignadasCount <= 0) {
            Toast.makeText(this, "No tienes tareas asignadas", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, frm_tareas.class);
        intent.putExtra("ESTADO", "TODAS");
        startActivity(intent);
    }

    public void refrescarDashboard(View view) {
        if (scrollDashboard != null) {
            scrollDashboard.smoothScrollTo(0, 0);
        }
        cargarMetricasDashboard();
        Toast.makeText(this, "Dashboard actualizado", Toast.LENGTH_SHORT).show();
    }

    public void abrirTareasCompletadas(View view) {
        if (tareasCompletadasCount <= 0) {
            Toast.makeText(this, "Aún no hay tareas completadas", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, frm_tareas.class);
        intent.putExtra("ESTADO", "Finalizada");
        startActivity(intent);
    }

    public void abrirTareasPendientes(View view) {
        if (tareasPendientesCount <= 0) {
            Toast.makeText(this, "Aún no hay tareas pendientes", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, frm_tareas.class);
        intent.putExtra("ESTADO", "Pendiente");
        startActivity(intent);
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