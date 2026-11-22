package com.example.maquetagestia.vista;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.cEquipo;
import com.example.maquetagestia.controller.cEquipo.MiembroEquipoItem;
import com.example.maquetagestia.controller.cFase;
import com.example.maquetagestia.controller.cProyecto;
import com.example.maquetagestia.controller.cTarea;
import com.example.maquetagestia.model.Fase;
import com.example.maquetagestia.model.Proyecto;
import com.example.maquetagestia.model.Tarea;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class frm_detalle_proyecto extends AppCompatActivity {

    private String proyectoId;
    private TextView lblTituloHeaderDetalle, lblNombreDetalle, lblDescripcionDetalle, lblFechasDetalle, badgeProgress;
    private TextView lblResumenTipo, lblResumenEstado, lblResumenFechas, lblResumenDescripcion;
    private TextView lblResumenFases, lblResumenTareasTotal, lblResumenTareasDesglose, lblResumenEquipo;
    private MaterialButton btnTabFases, btnTabResumen;
    private LinearLayout layoutFasesProyecto, layoutResumenProyecto;
    private RecyclerView rcvFasesStepper;
    private cProyecto proyectoControlador;
    private cFase faseControlador;
    private cTarea tareaControlador;
    private cEquipo equipoControlador;
    private FaseStepperAdapter faseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_detalle_proyecto);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        proyectoId = getIntent().getStringExtra("PROYECTO_ID");

        lblTituloHeaderDetalle = findViewById(R.id.lblTituloHeaderDetalle);
        lblNombreDetalle = findViewById(R.id.lblNombreDetalle);
        lblDescripcionDetalle = findViewById(R.id.lblDescripcionDetalle);
        lblFechasDetalle = findViewById(R.id.lblFechasDetalle);
        badgeProgress = findViewById(R.id.badgeProgress);
        btnTabFases = findViewById(R.id.btnTabFases);
        btnTabResumen = findViewById(R.id.btnTabResumen);
        layoutFasesProyecto = findViewById(R.id.layoutFasesProyecto);
        layoutResumenProyecto = findViewById(R.id.layoutResumenProyecto);
        lblResumenTipo = findViewById(R.id.lblResumenTipo);
        lblResumenEstado = findViewById(R.id.lblResumenEstado);
        lblResumenFechas = findViewById(R.id.lblResumenFechas);
        lblResumenDescripcion = findViewById(R.id.lblResumenDescripcion);
        lblResumenFases = findViewById(R.id.lblResumenFases);
        lblResumenTareasTotal = findViewById(R.id.lblResumenTareasTotal);
        lblResumenTareasDesglose = findViewById(R.id.lblResumenTareasDesglose);
        lblResumenEquipo = findViewById(R.id.lblResumenEquipo);
        rcvFasesStepper = findViewById(R.id.rcvFasesStepper);
        if (rcvFasesStepper != null) {
            rcvFasesStepper.setLayoutManager(new LinearLayoutManager(this));
        }

        proyectoControlador = new cProyecto();
        faseControlador = new cFase();
        tareaControlador = new cTarea();
        equipoControlador = new cEquipo();

        cargarDetalleProyecto();
        cargarFasesDelProyecto();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDetalleProyecto();
        cargarFasesDelProyecto();
        if (layoutResumenProyecto != null && layoutResumenProyecto.getVisibility() == View.VISIBLE) {
            cargarResumenProyecto();
        }
    }

    private void cargarDetalleProyecto() {
        if (proyectoId == null || proyectoId.isEmpty()) return;

        new Thread(() -> {
            Proyecto proyecto = proyectoControlador.obtenerPorId(proyectoId);

            runOnUiThread(() -> {
                if (proyecto == null) return;

                String nombre = proyecto.getNombre() != null && !proyecto.getNombre().isEmpty()
                        ? proyecto.getNombre()
                        : "Detalle del Proyecto";
                String descripcion = proyecto.getDescripcion() != null && !proyecto.getDescripcion().isEmpty()
                        ? proyecto.getDescripcion()
                        : "Sin descripción";
                String inicio = proyecto.getFechaInicio() != null && !proyecto.getFechaInicio().isEmpty()
                        ? proyecto.getFechaInicio()
                        : "Sin inicio";
                String fin = proyecto.getFechaFinEstimada() != null && !proyecto.getFechaFinEstimada().isEmpty()
                        ? proyecto.getFechaFinEstimada()
                        : "Sin fin";

                if (lblTituloHeaderDetalle != null) lblTituloHeaderDetalle.setText(nombre);
                if (lblNombreDetalle != null) lblNombreDetalle.setText(nombre);
                if (lblDescripcionDetalle != null) lblDescripcionDetalle.setText(descripcion);
                if (lblFechasDetalle != null) lblFechasDetalle.setText(inicio + " - " + fin);
                if (badgeProgress != null && "Finalizado".equalsIgnoreCase(proyecto.getEstado())) {
                    badgeProgress.setText("100%");
                }
            });
        }).start();
    }

    private void cargarFasesDelProyecto() {
        if (proyectoId == null || proyectoId.isEmpty()) return;

        new Thread(() -> {
            List<Fase> listaFases = faseControlador.listarPorProyecto(proyectoId);

            runOnUiThread(() -> {
                if (rcvFasesStepper != null) {
                    faseAdapter = new FaseStepperAdapter(frm_detalle_proyecto.this, listaFases, () -> {
                        cargarFasesDelProyecto();
                        if (layoutResumenProyecto != null && layoutResumenProyecto.getVisibility() == View.VISIBLE) {
                            cargarResumenProyecto();
                        }
                    });
                    rcvFasesStepper.setAdapter(faseAdapter);
                }

                if (badgeProgress != null) {
                    int total = listaFases.size();
                    if (total > 0) {
                        int completadas = 0;
                        for (Fase f : listaFases) {
                            if ("Completada".equalsIgnoreCase(f.getEstado())) {
                                completadas++;
                            }
                        }
                        int porcentaje = (completadas * 100) / total;
                        badgeProgress.setText(porcentaje + "%");
                    } else if (!"100%".equals(badgeProgress.getText().toString())) {
                        badgeProgress.setText("0%");
                    }
                }
            });
        }).start();
    }

    public void mostrarDialogoAgregarFase(View view) {
        if (proyectoId == null || proyectoId.isEmpty()) {
            Toast.makeText(this, "No se pudo identificar el proyecto", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Nueva Fase");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nueva_fase, null, false);
        EditText txtNombreFase = dialogView.findViewById(R.id.txtNombreFaseDialog);
        EditText txtFechaEstimada = dialogView.findViewById(R.id.txtFechaEstimadaFaseDialog);

        txtFechaEstimada.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(
                    frm_detalle_proyecto.this,
                    (picker, year, month, dayOfMonth) -> txtFechaEstimada.setText(
                            String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            dp.show();
        });

        builder.setView(dialogView);
        builder.setPositiveButton("Agregar Fase", (dialog, which) -> {
            String nombre = txtNombreFase.getText() != null ? txtNombreFase.getText().toString().trim() : "";
            String fechaEstimada = txtFechaEstimada.getText() != null ? txtFechaEstimada.getText().toString().trim() : "";

            if (TextUtils.isEmpty(nombre)) {
                Toast.makeText(this, "El nombre de la fase es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                boolean exito = faseControlador.crearFase(proyectoId, nombre, fechaEstimada);
                runOnUiThread(() -> {
                    if (exito) {
                        Toast.makeText(frm_detalle_proyecto.this, "¡Fase agregada exitosamente!", Toast.LENGTH_SHORT).show();
                        cargarFasesDelProyecto();
                    } else {
                        Toast.makeText(frm_detalle_proyecto.this, "Error al crear la fase en SQL Server", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void mostrarTabFases(View view) {
        if (layoutFasesProyecto != null) layoutFasesProyecto.setVisibility(View.VISIBLE);
        if (layoutResumenProyecto != null) layoutResumenProyecto.setVisibility(View.GONE);
        actualizarEstiloTabs(true);
    }

    public void mostrarTabResumen(View view) {
        if (layoutFasesProyecto != null) layoutFasesProyecto.setVisibility(View.GONE);
        if (layoutResumenProyecto != null) layoutResumenProyecto.setVisibility(View.VISIBLE);
        actualizarEstiloTabs(false);
        cargarResumenProyecto();
    }

    private void actualizarEstiloTabs(boolean fasesActivo) {
        int strokePx = Math.round(1 * getResources().getDisplayMetrics().density);
        int bgActivo = ContextCompat.getColor(this, R.color.primary_container);
        int txtActivo = ContextCompat.getColor(this, R.color.on_primary);
        int bgInactivo = ContextCompat.getColor(this, android.R.color.transparent);
        int txtInactivo = ContextCompat.getColor(this, R.color.on_surface_variant);
        int strokeInactivo = ContextCompat.getColor(this, R.color.outline_variant);

        if (btnTabFases != null) {
            btnTabFases.setBackgroundTintList(ColorStateList.valueOf(fasesActivo ? bgActivo : bgInactivo));
            btnTabFases.setTextColor(fasesActivo ? txtActivo : txtInactivo);
            btnTabFases.setStrokeWidth(fasesActivo ? 0 : strokePx);
            btnTabFases.setStrokeColor(ColorStateList.valueOf(strokeInactivo));
        }
        if (btnTabResumen != null) {
            boolean resumenActivo = !fasesActivo;
            btnTabResumen.setBackgroundTintList(ColorStateList.valueOf(resumenActivo ? bgActivo : bgInactivo));
            btnTabResumen.setTextColor(resumenActivo ? txtActivo : txtInactivo);
            btnTabResumen.setStrokeWidth(resumenActivo ? 0 : strokePx);
            btnTabResumen.setStrokeColor(ColorStateList.valueOf(strokeInactivo));
        }
    }

    private void cargarResumenProyecto() {
        if (proyectoId == null || proyectoId.isEmpty()) return;

        new Thread(() -> {
            Proyecto proyecto = proyectoControlador.obtenerPorId(proyectoId);
            List<Fase> fases = faseControlador.listarPorProyecto(proyectoId);
            List<Tarea> tareas = tareaControlador.listarTareasPorProyecto(proyectoId);
            List<MiembroEquipoItem> equipo = equipoControlador.listarEquipoPorProyecto(proyectoId);

            int totalFases = fases != null ? fases.size() : 0;
            int fasesCompletadas = 0;
            if (fases != null) {
                for (Fase f : fases) {
                    if ("Completada".equalsIgnoreCase(f.getEstado())) {
                        fasesCompletadas++;
                    }
                }
            }

            int totalTareas = tareas != null ? tareas.size() : 0;
            int pendientes = 0;
            int enProgreso = 0;
            int finalizadas = 0;
            if (tareas != null) {
                for (Tarea t : tareas) {
                    String est = t.getEstado();
                    if ("Finalizada".equalsIgnoreCase(est)) {
                        finalizadas++;
                    } else if ("EnProgreso".equalsIgnoreCase(est)) {
                        enProgreso++;
                    } else if ("Pendiente".equalsIgnoreCase(est)) {
                        pendientes++;
                    }
                }
            }

            int totalMiembros = equipo != null ? equipo.size() : 0;
            int finalFasesCompletadas = fasesCompletadas;
            int finalPendientes = pendientes;
            int finalEnProgreso = enProgreso;
            int finalFinalizadas = finalizadas;

            runOnUiThread(() -> {
                if (proyecto != null) {
                    String tipo = proyecto.getTipo() != null && !proyecto.getTipo().isEmpty() ? proyecto.getTipo() : "No especificado";
                    String estado = proyecto.getEstado() != null && !proyecto.getEstado().isEmpty() ? proyecto.getEstado() : "Planificacion";
                    String inicio = proyecto.getFechaInicio() != null && !proyecto.getFechaInicio().isEmpty() ? proyecto.getFechaInicio() : "Sin inicio";
                    String fin = proyecto.getFechaFinEstimada() != null && !proyecto.getFechaFinEstimada().isEmpty() ? proyecto.getFechaFinEstimada() : "Sin fin";
                    String desc = proyecto.getDescripcion() != null && !proyecto.getDescripcion().isEmpty() ? proyecto.getDescripcion() : "Sin descripción";

                    if (lblResumenTipo != null) lblResumenTipo.setText("Tipo: " + tipo);
                    if (lblResumenEstado != null) lblResumenEstado.setText("Estado actual: " + estado);
                    if (lblResumenFechas != null) lblResumenFechas.setText("Fechas: " + inicio + " - " + fin);
                    if (lblResumenDescripcion != null) lblResumenDescripcion.setText(desc);
                }

                if (lblResumenFases != null) {
                    lblResumenFases.setText("Fases: " + finalFasesCompletadas + " completadas de " + totalFases);
                }
                if (lblResumenTareasTotal != null) {
                    lblResumenTareasTotal.setText("Tareas totales: " + totalTareas);
                }
                if (lblResumenTareasDesglose != null) {
                    lblResumenTareasDesglose.setText("Pendientes: " + finalPendientes
                            + "  |  En progreso: " + finalEnProgreso
                            + "  |  Finalizadas: " + finalFinalizadas);
                }
                if (lblResumenEquipo != null) {
                    lblResumenEquipo.setText("Miembros del equipo: " + totalMiembros);
                }
            });
        }).start();
    }

    public void volverAProyectos(View view) {
        finish();
    }

    public void abrirTareasProyecto(View view) {
        Intent intent = new Intent(this, frm_tareas.class);
        intent.putExtra("PROYECTO_ID", proyectoId);
        startActivity(intent);
    }

    public void crearNuevaTarea(View view) {
        Intent intent = new Intent(this, frm_tareas.class);
        intent.putExtra("PROYECTO_ID", proyectoId);
        intent.putExtra("ABRIR_DIALOGO_CREAR", true);
        startActivity(intent);
    }

    public void abrirPantallaEquipo(View view) {
        Intent intent = new Intent(this, frm_equipo.class);
        intent.putExtra("PROYECTO_ID", proyectoId);
        startActivity(intent);
    }
}