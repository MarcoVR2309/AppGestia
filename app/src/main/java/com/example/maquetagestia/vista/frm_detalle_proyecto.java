package com.example.maquetagestia.vista;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
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
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
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

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class frm_detalle_proyecto extends AppCompatActivity {

    private String proyectoId;
    private TextView lblTituloHeaderDetalle, lblNombreDetalle, lblDescripcionDetalle, lblFechasDetalle, badgeProgress;
    private TextView lblResumenTipo, lblResumenEstado, lblResumenFechas, lblResumenDescripcion;
    private TextView lblResumenFases, lblResumenTareasTotal, lblResumenTareasDesglose, lblResumenEquipo;
    private MaterialButton btnTabFases, btnTabResumen, btnAgregarFase, btnClonarProyecto, btnEliminarProyecto;
    private LinearLayout layoutFasesProyecto, layoutResumenProyecto;
    private RecyclerView rcvFasesStepper;
    private cProyecto proyectoControlador;
    private cFase faseControlador;
    private cTarea tareaControlador;
    private cEquipo equipoControlador;
    private FaseStepperAdapter faseAdapter;
    private boolean esUsuarioLider = false;

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
        btnAgregarFase = findViewById(R.id.btnAgregarFase);
        btnClonarProyecto = findViewById(R.id.btnClonarProyecto);
        btnEliminarProyecto = findViewById(R.id.btnEliminarProyecto);
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

        if (btnAgregarFase != null) btnAgregarFase.setVisibility(View.GONE);
        if (btnClonarProyecto != null) btnClonarProyecto.setVisibility(View.GONE);
        if (btnEliminarProyecto != null) btnEliminarProyecto.setVisibility(View.GONE);

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
        String idUsuario = Sesion.getIdUsuario(this);

        new Thread(() -> {
            Proyecto proyecto = proyectoControlador.obtenerPorId(proyectoId);
            boolean esLider = !TextUtils.isEmpty(idUsuario)
                    && equipoControlador.esLiderDelProyecto(proyectoId, idUsuario);

            runOnUiThread(() -> {
                esUsuarioLider = esLider;
                if (btnAgregarFase != null) btnAgregarFase.setVisibility(esLider ? View.VISIBLE : View.GONE);
                if (btnClonarProyecto != null) btnClonarProyecto.setVisibility(esLider ? View.VISIBLE : View.GONE);
                if (btnEliminarProyecto != null) btnEliminarProyecto.setVisibility(esLider ? View.VISIBLE : View.GONE);

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
        String idUsuario = Sesion.getIdUsuario(this);

        new Thread(() -> {
            List<Fase> listaFases = faseControlador.listarPorProyecto(proyectoId);
            boolean esLider = !TextUtils.isEmpty(idUsuario)
                    && equipoControlador.esLiderDelProyecto(proyectoId, idUsuario);

            runOnUiThread(() -> {
                esUsuarioLider = esLider;
                if (rcvFasesStepper != null) {
                    faseAdapter = new FaseStepperAdapter(frm_detalle_proyecto.this, listaFases, () -> {
                        cargarFasesDelProyecto();
                        if (layoutResumenProyecto != null && layoutResumenProyecto.getVisibility() == View.VISIBLE) {
                            cargarResumenProyecto();
                        }
                    }, esLider);
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
        if (!esUsuarioLider) {
            Toast.makeText(this, "Solo el líder del proyecto puede agregar fases", Toast.LENGTH_SHORT).show();
            return;
        }
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

            String idUsuario = Sesion.getIdUsuario(this);
            new Thread(() -> {
                boolean exito = faseControlador.crearFase(proyectoId, nombre, fechaEstimada, idUsuario);
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

    public void abrirCalendarioProyecto(View view) {
        Intent intent = new Intent(this, frm_calendario.class);
        intent.putExtra("PROYECTO_ID", proyectoId);
        startActivity(intent);
    }

    public void mostrarDialogoClonarProyecto(View view) {
        if (!esUsuarioLider) {
            Toast.makeText(this, "Solo el líder puede clonar la estructura del proyecto", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(proyectoId)) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Clonar proyecto como plantilla");
        builder.setMessage("Se copiarán todas las fases en orden, sin tareas ni notificaciones. Escribe el nombre del nuevo proyecto:");

        final EditText txtNuevoNombre = new EditText(this);
        txtNuevoNombre.setHint("Nombre del nuevo proyecto");
        String nombreActual = lblNombreDetalle != null ? lblNombreDetalle.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(nombreActual)) {
            txtNuevoNombre.setText(nombreActual + " (Copia)");
        }
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(txtNuevoNombre, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        builder.setView(box);

        builder.setPositiveButton("Clonar", (dialog, which) -> {
            String nuevoNombre = txtNuevoNombre.getText() != null ? txtNuevoNombre.getText().toString().trim() : "";
            if (TextUtils.isEmpty(nuevoNombre)) {
                Toast.makeText(this, "Debes ingresar un nombre para el nuevo proyecto", Toast.LENGTH_SHORT).show();
                return;
            }
            String idUsuario = Sesion.getIdUsuario(this);
            if (btnClonarProyecto != null) btnClonarProyecto.setEnabled(false);

            new Thread(() -> {
                String nuevoId = proyectoControlador.clonarProyectoComoPlantilla(proyectoId, nuevoNombre, idUsuario);
                runOnUiThread(() -> {
                    if (btnClonarProyecto != null) btnClonarProyecto.setEnabled(true);
                    if (nuevoId != null) {
                        Toast.makeText(this, "Proyecto clonado exitosamente", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, frm_detalle_proyecto.class);
                        intent.putExtra("PROYECTO_ID", nuevoId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "No se pudo clonar el proyecto", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void confirmarEliminarProyecto(View view) {
        if (!esUsuarioLider) {
            Toast.makeText(this, "Solo el líder puede eliminar el proyecto", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(proyectoId)) return;

        String nombreProy = lblNombreDetalle != null ? lblNombreDetalle.getText().toString().trim() : "este proyecto";
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar Proyecto")
                .setMessage("¿Seguro que deseas eliminar permanentemente \"" + nombreProy
                        + "\" junto con todas sus fases, tareas y equipo?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    String idUsuario = Sesion.getIdUsuario(this);
                    if (btnEliminarProyecto != null) btnEliminarProyecto.setEnabled(false);
                    new Thread(() -> {
                        boolean ok = proyectoControlador.eliminarProyecto(proyectoId, idUsuario);
                        runOnUiThread(() -> {
                            if (ok) {
                                Toast.makeText(this, "Proyecto eliminado", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                if (btnEliminarProyecto != null) btnEliminarProyecto.setEnabled(true);
                                Toast.makeText(this, "No se pudo eliminar el proyecto", Toast.LENGTH_LONG).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void exportarReportePdf(View view) {
        if (TextUtils.isEmpty(proyectoId)) return;
        if (view != null) view.setEnabled(false);
        Toast.makeText(this, "Generando reporte PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            Proyecto proyecto = proyectoControlador.obtenerPorId(proyectoId);
            List<Fase> fases = faseControlador.listarPorProyecto(proyectoId);
            List<Tarea> tareas = tareaControlador.listarTareasPorProyecto(proyectoId);
            List<MiembroEquipoItem> equipo = equipoControlador.listarEquipoPorProyecto(proyectoId);

            if (proyecto == null) {
                runOnUiThread(() -> {
                    if (view != null) view.setEnabled(true);
                    Toast.makeText(this, "No se pudieron obtener los datos del proyecto", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            File pdfFile = null;
            Exception error = null;
            PdfDocument document = new PdfDocument();
            try {
                PdfPageWriter writer = new PdfPageWriter(document);
                writer.startNewPage();

                Paint pTitulo = new Paint(Paint.ANTI_ALIAS_FLAG);
                pTitulo.setColor(Color.parseColor("#1A237E"));
                pTitulo.setTextSize(18f);
                pTitulo.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                Paint pSubtitulo = new Paint(Paint.ANTI_ALIAS_FLAG);
                pSubtitulo.setColor(Color.parseColor("#283593"));
                pSubtitulo.setTextSize(13f);
                pSubtitulo.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                Paint pTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
                pTexto.setColor(Color.parseColor("#212121"));
                pTexto.setTextSize(10.5f);

                Paint pMeta = new Paint(Paint.ANTI_ALIAS_FLAG);
                pMeta.setColor(Color.parseColor("#616161"));
                pMeta.setTextSize(9.5f);

                String nombreProy = !TextUtils.isEmpty(proyecto.getNombre()) ? proyecto.getNombre() : "Proyecto";
                String estadoProy = !TextUtils.isEmpty(proyecto.getEstado()) ? proyecto.getEstado() : "N/A";
                String tipoProy = !TextUtils.isEmpty(proyecto.getTipo()) ? proyecto.getTipo() : "N/A";
                String fechaInicio = !TextUtils.isEmpty(proyecto.getFechaInicio()) ? proyecto.getFechaInicio() : "Sin inicio";
                String fechaFin = !TextUtils.isEmpty(proyecto.getFechaFinEstimada()) ? proyecto.getFechaFinEstimada() : "Sin fin";
                String descProy = !TextUtils.isEmpty(proyecto.getDescripcion()) ? proyecto.getDescripcion() : "Sin descripción";

                int totalFases = fases != null ? fases.size() : 0;
                int fasesComp = 0;
                if (fases != null) {
                    for (Fase f : fases) {
                        if ("Completada".equalsIgnoreCase(f.getEstado())) fasesComp++;
                    }
                }
                int pctGlobal = "Finalizado".equalsIgnoreCase(estadoProy) ? 100
                        : (totalFases > 0 ? (fasesComp * 100) / totalFases : 0);

                String fechaReporte = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

                writer.drawWrappedText("GESTIA - REPORTE DE PROYECTO", pTitulo, 22);
                writer.drawWrappedText("Generado: " + fechaReporte, pMeta, 14);
                writer.addSpacing(6);
                writer.drawDivider();
                writer.addSpacing(8);

                writer.drawWrappedText(nombreProy, pSubtitulo, 18);
                writer.drawWrappedText("Tipo: " + tipoProy + "  |  Estado: " + estadoProy + "  |  Progreso global: " + pctGlobal + "%", pTexto, 15);
                writer.drawWrappedText("Fechas: " + fechaInicio + " a " + fechaFin, pTexto, 15);
                writer.drawWrappedText("Descripción: " + descProy, pMeta, 14);
                writer.addSpacing(8);

                // Miembros
                writer.drawWrappedText("1. Equipo del Proyecto (" + (equipo != null ? equipo.size() : 0) + ")", pSubtitulo, 18);
                writer.drawDivider();
                writer.addSpacing(4);
                if (equipo == null || equipo.isEmpty()) {
                    writer.drawWrappedText("Sin miembros registrados.", pMeta, 14);
                } else {
                    for (MiembroEquipoItem m : equipo) {
                        String nomM = m.usuario != null && !TextUtils.isEmpty(m.usuario.getNombreCompleto())
                                ? m.usuario.getNombreCompleto() : "Usuario";
                        String rolM = !TextUtils.isEmpty(m.rol) ? m.rol : "Colaborador";
                        writer.drawWrappedText("• " + nomM + " — Rol: " + rolM, pTexto, 15);
                    }
                }
                writer.addSpacing(10);

                // Fases
                writer.drawWrappedText("2. Fases del Proyecto (" + totalFases + ")", pSubtitulo, 18);
                writer.drawDivider();
                writer.addSpacing(4);
                if (fases == null || fases.isEmpty()) {
                    writer.drawWrappedText("Sin fases registradas.", pMeta, 14);
                } else {
                    for (Fase f : fases) {
                        String fFecha = !TextUtils.isEmpty(f.getFechaEstimada()) ? " | Est: " + f.getFechaEstimada() : "";
                        String fEst = !TextUtils.isEmpty(f.getEstado()) ? f.getEstado() : "Pendiente";
                        writer.drawWrappedText("• " + f.getOrden() + ". " + f.getNombre() + " [" + fEst + "]" + fFecha, pTexto, 15);
                    }
                }
                writer.addSpacing(10);

                // Tareas
                int totalTareas = tareas != null ? tareas.size() : 0;
                writer.drawWrappedText("3. Tareas del Proyecto (" + totalTareas + ")", pSubtitulo, 18);
                writer.drawDivider();
                writer.addSpacing(4);
                if (tareas == null || tareas.isEmpty()) {
                    writer.drawWrappedText("Sin tareas registradas.", pMeta, 14);
                } else {
                    for (Tarea t : tareas) {
                        String tEst = !TextUtils.isEmpty(t.getEstado()) ? t.getEstado() : "Pendiente";
                        String tPrio = !TextUtils.isEmpty(t.getPrioridad()) ? t.getPrioridad() : "Media";
                        String tFecha = !TextUtils.isEmpty(t.getFechaLimite()) ? t.getFechaLimite() : "Sin límite";
                        String tResp = !TextUtils.isEmpty(t.getResponsableNombre()) ? t.getResponsableNombre() : "Sin asignar";
                        writer.drawWrappedText("• " + t.getTitulo() + " [" + tEst + "]", pTexto, 14);
                        writer.drawWrappedText("   Prioridad: " + tPrio + " | Límite: " + tFecha + " | Responsable: " + tResp, pMeta, 14);
                    }
                }

                writer.finish();

                String safeId = proyectoId.replaceAll("[^a-zA-Z0-9_-]", "_");
                pdfFile = new File(getCacheDir(), "Reporte_" + safeId + ".pdf");
                try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                    document.writeTo(fos);
                }
            } catch (Exception e) {
                error = e;
            } finally {
                document.close();
            }

            File finalPdf = pdfFile;
            Exception finalError = error;
            String nombreParaAsunto = !TextUtils.isEmpty(proyecto.getNombre()) ? proyecto.getNombre() : "Proyecto";
            runOnUiThread(() -> {
                if (view != null) view.setEnabled(true);
                if (finalError != null || finalPdf == null || !finalPdf.exists()) {
                    Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", finalPdf);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Reporte de Proyecto: " + nombreParaAsunto);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Compartir reporte PDF"));
                } catch (Exception ex) {
                    Toast.makeText(this, "PDF guardado pero no se pudo compartir: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private static class PdfPageWriter {
        private static final int PAGE_WIDTH = 595;
        private static final int PAGE_HEIGHT = 842;
        private static final int MARGIN_X = 40;
        private static final int MAX_WIDTH = 515;
        private static final int MARGIN_BOTTOM = 790;

        private final PdfDocument doc;
        private final Paint footerPaint;
        private final Paint dividerPaint;
        private PdfDocument.Page currentPage;
        private Canvas canvas;
        private int pageNum = 0;
        private int y = 48;

        PdfPageWriter(PdfDocument doc) {
            this.doc = doc;
            this.footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.footerPaint.setColor(Color.parseColor("#9E9E9E"));
            this.footerPaint.setTextSize(8.5f);

            this.dividerPaint = new Paint();
            this.dividerPaint.setColor(Color.parseColor("#CFD8DC"));
            this.dividerPaint.setStrokeWidth(1f);
        }

        void startNewPage() {
            if (currentPage != null) {
                doc.finishPage(currentPage);
            }
            pageNum++;
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create();
            currentPage = doc.startPage(info);
            canvas = currentPage.getCanvas();
            canvas.drawText("Gestia - Página " + pageNum, MARGIN_X, 820, footerPaint);
            y = 48;
        }

        void ensureSpace(int neededPx) {
            if (currentPage == null || y + neededPx > MARGIN_BOTTOM) {
                startNewPage();
            }
        }

        void addSpacing(int px) {
            y += px;
        }

        void drawDivider() {
            ensureSpace(8);
            canvas.drawLine(MARGIN_X, y, MARGIN_X + MAX_WIDTH, y, dividerPaint);
            y += 6;
        }

        void drawWrappedText(String text, Paint paint, int lineHeight) {
            if (text == null) return;
            String[] paragraphs = text.split("\\r?\\n");
            for (String para : paragraphs) {
                if (para.isEmpty()) {
                    ensureSpace(lineHeight);
                    y += lineHeight;
                    continue;
                }
                List<String> lines = wrapParagraph(para, paint);
                for (String line : lines) {
                    ensureSpace(lineHeight);
                    canvas.drawText(line, MARGIN_X, y, paint);
                    y += lineHeight;
                }
            }
        }

        private List<String> wrapParagraph(String para, Paint paint) {
            List<String> out = new ArrayList<>();
            int start = 0;
            int len = para.length();
            while (start < len) {
                int count = paint.breakText(para, start, len, true, MAX_WIDTH, null);
                if (count <= 0) break;
                int end = start + count;
                if (end < len && para.charAt(end) != ' ') {
                    int lastSpace = para.lastIndexOf(' ', end);
                    if (lastSpace > start) {
                        end = lastSpace + 1;
                    }
                }
                out.add(para.substring(start, end).trim());
                start = end;
                while (start < len && para.charAt(start) == ' ') {
                    start++;
                }
            }
            return out;
        }

        void finish() {
            if (currentPage != null) {
                doc.finishPage(currentPage);
                currentPage = null;
            }
        }
    }
}