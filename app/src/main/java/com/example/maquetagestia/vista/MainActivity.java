package com.example.maquetagestia.vista;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.AlertaVencimientoWorker;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cDashboard;
import com.example.maquetagestia.controller.cProyecto;
import com.example.maquetagestia.controller.cTarea;
import com.example.maquetagestia.model.DashboardDTO;
import com.example.maquetagestia.model.Proyecto;
import com.example.maquetagestia.model.Tarea;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CODE_NOTIF = 1001;

    private TextView tvGreeting;
    private ImageButton btnNotifications;
    private ScrollView scrollDashboard;
    private EditText txtBusquedaGlobal;
    private LinearLayout layoutResultadosBusqueda, containerResultadosProyectos, containerResultadosTareas;
    private TextView lblKpiProyectos, lblKpiTareas, lblKpiCompletadas, lblKpiPendientes, lblPorcentajeProgreso;
    private ProgressBar progressBarGlobal;
    private ImageView btnAddProjectNav;
    private RecyclerView rcvProyectosRecientes;
    private ProyectoAdapter proyectosRecientesAdapter;
    private cDashboard dashboardControlador;
    private cProyecto proyectoControlador;
    private cTarea tareaControlador;
    private int tokenBusquedaGlobal = 0;
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
        txtBusquedaGlobal = findViewById(R.id.txtBusquedaGlobal);
        layoutResultadosBusqueda = findViewById(R.id.layoutResultadosBusqueda);
        containerResultadosProyectos = findViewById(R.id.containerResultadosProyectos);
        containerResultadosTareas = findViewById(R.id.containerResultadosTareas);
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
        tareaControlador = new cTarea();

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

        configurarBusquedaGlobal();
        configurarAlertasSegundoPlano();
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

    public void abrirCalendarioHome(View view) {
        Intent intent = new Intent(this, frm_calendario.class);
        startActivity(intent);
    }

    private void configurarAlertasSegundoPlano() {
        AlertaVencimientoWorker.programarTrabajosAlerta(this, true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_CODE_NOTIF
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions,
                                           @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE_NOTIF && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            AlertaVencimientoWorker.programarTrabajosAlerta(this, true);
        }
    }

    private void configurarBusquedaGlobal() {
        if (txtBusquedaGlobal == null) return;

        txtBusquedaGlobal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String q = s != null ? s.toString().trim() : "";
                if (TextUtils.isEmpty(q)) {
                    tokenBusquedaGlobal++;
                    if (layoutResultadosBusqueda != null) {
                        layoutResultadosBusqueda.setVisibility(View.GONE);
                    }
                    return;
                }

                String idUsuario = Sesion.getIdUsuario(MainActivity.this);
                if (TextUtils.isEmpty(idUsuario)) return;

                final int token = ++tokenBusquedaGlobal;
                final String qLower = q.toLowerCase(Locale.ROOT);

                new Thread(() -> {
                    List<Proyecto> todosProyectos = proyectoControlador.listarPorUsuario(idUsuario, "Todos");
                    List<Proyecto> proyCoincidentes = new ArrayList<>();
                    if (todosProyectos != null) {
                        for (Proyecto p : todosProyectos) {
                            String nom = p.getNombre() != null ? p.getNombre().toLowerCase(Locale.ROOT) : "";
                            String desc = p.getDescripcion() != null ? p.getDescripcion().toLowerCase(Locale.ROOT) : "";
                            if (nom.contains(qLower) || desc.contains(qLower)) {
                                proyCoincidentes.add(p);
                            }
                        }
                    }

                    List<Tarea> tareasCoincidentes = tareaControlador.buscarTareasGlobalPorUsuario(idUsuario, q);

                    runOnUiThread(() -> {
                        if (token != tokenBusquedaGlobal || layoutResultadosBusqueda == null) return;
                        layoutResultadosBusqueda.setVisibility(View.VISIBLE);
                        renderizarResultadosProyectos(proyCoincidentes);
                        renderizarResultadosTareas(tareasCoincidentes);
                    });
                }).start();
            }
        });
    }

    private void renderizarResultadosProyectos(List<Proyecto> proyectos) {
        if (containerResultadosProyectos == null) return;
        containerResultadosProyectos.removeAllViews();

        if (proyectos == null || proyectos.isEmpty()) {
            TextView tvVacio = new TextView(this);
            tvVacio.setText("Sin proyectos coincidentes");
            tvVacio.setTextSize(12);
            tvVacio.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            tvVacio.setPadding(0, dpToPx(2), 0, dpToPx(4));
            containerResultadosProyectos.addView(tvVacio);
            return;
        }

        for (Proyecto p : proyectos) {
            MaterialCardView card = crearTarjetaResultado();
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            TextView tvTitulo = new TextView(this);
            tvTitulo.setText(p.getNombre());
            tvTitulo.setTextSize(14);
            tvTitulo.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
            content.addView(tvTitulo);

            TextView tvMeta = new TextView(this);
            String estado = !TextUtils.isEmpty(p.getEstado()) ? p.getEstado() : "Activo";
            String tipo = !TextUtils.isEmpty(p.getTipo()) ? p.getTipo() : "General";
            tvMeta.setText(tipo + "  ·  Estado: " + estado);
            tvMeta.setTextSize(12);
            tvMeta.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            content.addView(tvMeta);

            card.addView(content);
            card.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, frm_detalle_proyecto.class);
                intent.putExtra("PROYECTO_ID", p.getId());
                startActivity(intent);
            });
            containerResultadosProyectos.addView(card);
        }
    }

    private void renderizarResultadosTareas(List<Tarea> tareas) {
        if (containerResultadosTareas == null) return;
        containerResultadosTareas.removeAllViews();

        if (tareas == null || tareas.isEmpty()) {
            TextView tvVacio = new TextView(this);
            tvVacio.setText("Sin tareas coincidentes");
            tvVacio.setTextSize(12);
            tvVacio.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            tvVacio.setPadding(0, dpToPx(2), 0, dpToPx(4));
            containerResultadosTareas.addView(tvVacio);
            return;
        }

        for (Tarea t : tareas) {
            MaterialCardView card = crearTarjetaResultado();
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            TextView tvTitulo = new TextView(this);
            tvTitulo.setText(t.getTitulo());
            tvTitulo.setTextSize(14);
            tvTitulo.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
            content.addView(tvTitulo);

            TextView tvMeta = new TextView(this);
            String estado = !TextUtils.isEmpty(t.getEstado()) ? t.getEstado() : "Pendiente";
            String fecha = !TextUtils.isEmpty(t.getFechaLimite()) ? "  ·  Límite: " + t.getFechaLimite() : "";
            tvMeta.setText("Estado: " + estado + fecha);
            tvMeta.setTextSize(12);
            tvMeta.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            content.addView(tvMeta);

            card.addView(content);
            card.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, frm_tareas.class);
                if (!TextUtils.isEmpty(t.getProyectoId())) {
                    intent.putExtra("PROYECTO_ID", t.getProyectoId());
                } else {
                    intent.putExtra("ESTADO", "TODAS");
                }
                startActivity(intent);
            });
            containerResultadosTareas.addView(card);
        }
    }

    private MaterialCardView crearTarjetaResultado() {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dpToPx(6);
        card.setLayoutParams(lp);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_container_lowest));
        card.setRadius(dpToPx(10));
        card.setCardElevation(dpToPx(1));
        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.outline_variant));
        return card;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}