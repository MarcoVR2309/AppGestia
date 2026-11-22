package com.example.maquetagestia.vista;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
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
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cEquipo;
import com.example.maquetagestia.controller.cEquipo.MiembroEquipoItem;
import com.example.maquetagestia.controller.cFase;
import com.example.maquetagestia.controller.cTarea;
import com.example.maquetagestia.model.Fase;
import com.example.maquetagestia.model.Tarea;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class frm_tareas extends AppCompatActivity {

    private static final String FILTRO_TODAS = "TODAS";
    private static final String FILTRO_PENDIENTES = "Pendiente";
    private static final String FILTRO_EN_PROGRESO = "EnProgreso";
    private static final String FILTRO_COMPLETADAS = "Finalizada";

    private String proyectoId;
    private String estadoFiltro;
    private boolean abrirDialogoCrearPendiente;
    private String filtroActual = FILTRO_TODAS;

    private RecyclerView rcvTareas;
    private TextView tvSinTareas;
    private EditText txtBuscarTarea;
    private ImageButton btnBuscarTarea;
    private MaterialButton btnFiltroTodas, btnFiltroPendientes, btnFiltroEnProgreso, btnFiltroCompletadas;

    private cTarea tareaControlador;
    private cFase faseControlador;
    private cEquipo equipoControlador;
    private TareaAdapter tareaAdapter;
    private List<Tarea> listaTareasCompleta = new ArrayList<>();
    private final List<Tarea> listaTareasFiltrada = new ArrayList<>();
    private List<Fase> listaFasesProyecto = new ArrayList<>();
    private List<MiembroEquipoItem> listaEquipoProyecto = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_tareas);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        proyectoId = getIntent().getStringExtra("PROYECTO_ID");
        estadoFiltro = getIntent().getStringExtra("ESTADO");
        abrirDialogoCrearPendiente = getIntent().getBooleanExtra("ABRIR_DIALOGO_CREAR", false);

        if ((proyectoId == null || proyectoId.isEmpty()) && FILTRO_COMPLETADAS.equalsIgnoreCase(estadoFiltro)) {
            filtroActual = FILTRO_COMPLETADAS;
        }

        rcvTareas = findViewById(R.id.rcvTareas);
        tvSinTareas = findViewById(R.id.tvSinTareas);
        txtBuscarTarea = findViewById(R.id.txtBuscarTarea);
        btnBuscarTarea = findViewById(R.id.btnBuscarTarea);
        btnFiltroTodas = findViewById(R.id.btnFiltroTodas);
        btnFiltroPendientes = findViewById(R.id.btnFiltroPendientes);
        btnFiltroEnProgreso = findViewById(R.id.btnFiltroEnProgreso);
        btnFiltroCompletadas = findViewById(R.id.btnFiltroCompletadas);

        if (rcvTareas != null) {
            rcvTareas.setLayoutManager(new LinearLayoutManager(this));
        }

        configurarFiltros();

        tareaControlador = new cTarea();
        faseControlador = new cFase();
        equipoControlador = new cEquipo();

        cargarTareas();
    }

    private void configurarFiltros() {
        if (btnFiltroTodas != null) {
            btnFiltroTodas.setOnClickListener(v -> aplicarFiltro(FILTRO_TODAS));
        }
        if (btnFiltroPendientes != null) {
            btnFiltroPendientes.setOnClickListener(v -> aplicarFiltro(FILTRO_PENDIENTES));
        }
        if (btnFiltroEnProgreso != null) {
            btnFiltroEnProgreso.setOnClickListener(v -> aplicarFiltro(FILTRO_EN_PROGRESO));
        }
        if (btnFiltroCompletadas != null) {
            btnFiltroCompletadas.setOnClickListener(v -> aplicarFiltro(FILTRO_COMPLETADAS));
        }
        if (btnBuscarTarea != null && txtBuscarTarea != null) {
            btnBuscarTarea.setOnClickListener(v -> {
                if (txtBuscarTarea.getVisibility() == View.VISIBLE) {
                    txtBuscarTarea.setText("");
                    txtBuscarTarea.setVisibility(View.GONE);
                } else {
                    txtBuscarTarea.setVisibility(View.VISIBLE);
                    txtBuscarTarea.requestFocus();
                }
            });
        }
        if (txtBuscarTarea != null) {
            txtBuscarTarea.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    aplicarFiltro(filtroActual);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
        actualizarEstiloFiltros();
    }

    public void cargarTareas() {
        if (proyectoId != null && !proyectoId.isEmpty()) {
            new Thread(() -> {
                List<Tarea> lista = tareaControlador.listarTareasPorProyecto(proyectoId);
                List<Fase> fases = faseControlador.listarPorProyecto(proyectoId);

                runOnUiThread(() -> {
                    listaTareasCompleta = lista != null ? lista : new ArrayList<>();
                    listaFasesProyecto = fases != null ? fases : new ArrayList<>();
                    aplicarFiltro(filtroActual);

                    if (abrirDialogoCrearPendiente) {
                        abrirDialogoCrearPendiente = false;
                        agregarNuevaTarea(null);
                    }
                });
            }).start();
        } else if (estadoFiltro != null && !estadoFiltro.isEmpty()) {
            String idUsuario = Sesion.getIdUsuario(this);
            if (idUsuario == null || idUsuario.isEmpty()) return;

            new Thread(() -> {
                List<Tarea> lista = tareaControlador.listarTareasPorUsuarioYEstado(idUsuario, estadoFiltro);

                runOnUiThread(() -> {
                    listaTareasCompleta = lista != null ? lista : new ArrayList<>();
                    aplicarFiltro(filtroActual);
                });
            }).start();
        }
    }

    public void aplicarFiltro(String filtro) {
        filtroActual = filtro != null ? filtro : FILTRO_TODAS;
        actualizarEstiloFiltros();

        String busqueda = (txtBuscarTarea != null && txtBuscarTarea.getText() != null)
                ? txtBuscarTarea.getText().toString().trim().toLowerCase(Locale.ROOT)
                : "";

        listaTareasFiltrada.clear();
        for (Tarea t : listaTareasCompleta) {
            boolean coincideEstado = FILTRO_TODAS.equals(filtroActual) || filtroActual.equalsIgnoreCase(t.getEstado());
            if (!coincideEstado) continue;

            if (!busqueda.isEmpty()) {
                String titulo = t.getTitulo() != null ? t.getTitulo().toLowerCase(Locale.ROOT) : "";
                String instr = t.getInstrucciones() != null ? t.getInstrucciones().toLowerCase(Locale.ROOT) : "";
                String resp = t.getResponsableNombre() != null ? t.getResponsableNombre().toLowerCase(Locale.ROOT) : "";
                if (!titulo.contains(busqueda) && !instr.contains(busqueda) && !resp.contains(busqueda)) {
                    continue;
                }
            }

            listaTareasFiltrada.add(t);
        }

        if (listaTareasFiltrada.isEmpty()) {
            if (tvSinTareas != null) {
                tvSinTareas.setText(FILTRO_TODAS.equals(filtroActual)
                        ? "No hay tareas registradas"
                        : "No hay tareas en este estado");
                tvSinTareas.setVisibility(View.VISIBLE);
            }
            if (rcvTareas != null) {
                rcvTareas.setVisibility(View.GONE);
            }
        } else {
            if (tvSinTareas != null) {
                tvSinTareas.setVisibility(View.GONE);
            }
            if (rcvTareas != null) {
                rcvTareas.setVisibility(View.VISIBLE);
                tareaAdapter = new TareaAdapter(frm_tareas.this, listaTareasFiltrada);
                rcvTareas.setAdapter(tareaAdapter);
            }
        }
    }

    private void actualizarEstiloFiltros() {
        actualizarBotonFiltro(btnFiltroTodas, FILTRO_TODAS.equals(filtroActual));
        actualizarBotonFiltro(btnFiltroPendientes, FILTRO_PENDIENTES.equals(filtroActual));
        actualizarBotonFiltro(btnFiltroEnProgreso, FILTRO_EN_PROGRESO.equals(filtroActual));
        actualizarBotonFiltro(btnFiltroCompletadas, FILTRO_COMPLETADAS.equals(filtroActual));
    }

    private void actualizarBotonFiltro(MaterialButton boton, boolean activo) {
        if (boton == null) return;
        int strokePx = Math.round(1 * getResources().getDisplayMetrics().density);
        if (activo) {
            boton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_container)));
            boton.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
            boton.setStrokeWidth(0);
        } else {
            boton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.transparent)));
            boton.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            boton.setStrokeWidth(strokePx);
            boton.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.outline_variant)));
        }
    }

    public void volverAtras(View view) {
        finish();
    }

    public void agregarNuevaTarea(View view) {
        if (proyectoId == null || proyectoId.isEmpty()) {
            Toast.makeText(this, "Seleccione un proyecto para agregar nuevas tareas", Toast.LENGTH_SHORT).show();
            return;
        }
        if (listaFasesProyecto.isEmpty()) {
            Toast.makeText(this, "Agregue al menos una fase al proyecto antes de crear tareas", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear diálogo modal M3 para registrar tarea
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Nueva Tarea");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nueva_tarea, null, false);
        EditText txtTitulo = dialogView.findViewById(R.id.txtTituloTareaDialog);
        EditText txtInstrucciones = dialogView.findViewById(R.id.txtInstruccionesDialog);
        EditText txtFechaLimite = dialogView.findViewById(R.id.txtFechaLimiteDialog);
        Spinner spPrioridad = dialogView.findViewById(R.id.spPrioridadDialog);
        Spinner spFase = dialogView.findViewById(R.id.spFaseDialog);
        Spinner spAsignado = dialogView.findViewById(R.id.spAsignadoDialog);

        // Selector de fecha nativo (solo hoy o posterior)
        txtFechaLimite.setOnClickListener(v -> {
            Calendar minCal = obtenerInicioDeHoy();
            Calendar cal = (Calendar) minCal.clone();
            String actual = txtFechaLimite.getText() != null ? txtFechaLimite.getText().toString().trim() : "";
            if (actual.length() >= 10 && actual.charAt(4) == '-' && actual.charAt(7) == '-') {
                try {
                    int y = Integer.parseInt(actual.substring(0, 4));
                    int m = Integer.parseInt(actual.substring(5, 7)) - 1;
                    int d = Integer.parseInt(actual.substring(8, 10));
                    Calendar sel = Calendar.getInstance();
                    sel.set(y, m, d, 0, 0, 0);
                    sel.set(Calendar.MILLISECOND, 0);
                    if (!sel.before(minCal)) {
                        cal = sel;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            DatePickerDialog datePicker = new DatePickerDialog(
                    frm_tareas.this,
                    (picker, year, month, dayOfMonth) -> {
                        String fechaFormateada = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        txtFechaLimite.setText(fechaFormateada);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.getDatePicker().setMinDate(minCal.getTimeInMillis());
            datePicker.show();
        });

        // Opciones de Prioridad
        String[] prioridades = {"Baja", "Media", "Alta", "Urgente"};
        ArrayAdapter<String> adapterPrio = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, prioridades);
        spPrioridad.setAdapter(adapterPrio);

        // Opciones de Fases reales
        List<String> nombresFases = new ArrayList<>();
        for (Fase f : listaFasesProyecto) {
            nombresFases.add(f.getNombre());
        }
        ArrayAdapter<String> adapterFase = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombresFases);
        spFase.setAdapter(adapterFase);

        // Cargar miembros del equipo en hilo secundario, manteniendo siempre "Sin asignar" como primera opción
        listaEquipoProyecto = new ArrayList<>();
        List<String> opcionesIniciales = new ArrayList<>();
        opcionesIniciales.add("Sin asignar");
        ArrayAdapter<String> adapterInicial = new ArrayAdapter<>(
                frm_tareas.this,
                android.R.layout.simple_spinner_dropdown_item,
                opcionesIniciales
        );
        spAsignado.setAdapter(adapterInicial);

        new Thread(() -> {
            List<MiembroEquipoItem> miembros = equipoControlador.listarEquipoPorProyecto(proyectoId);
            runOnUiThread(() -> {
                listaEquipoProyecto = miembros != null ? miembros : new ArrayList<>();
                List<String> nombresMiembros = new ArrayList<>();
                nombresMiembros.add("Sin asignar");
                for (MiembroEquipoItem item : listaEquipoProyecto) {
                    nombresMiembros.add(item.usuario.getNombreCompleto());
                }
                ArrayAdapter<String> adapterAsignado = new ArrayAdapter<>(
                        frm_tareas.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        nombresMiembros
                );
                spAsignado.setAdapter(adapterAsignado);
            });
        }).start();

        builder.setView(dialogView);
        builder.setPositiveButton("Crear", (dialog, which) -> {
            String titulo = txtTitulo.getText() != null ? txtTitulo.getText().toString().trim() : "";
            String instrucciones = txtInstrucciones.getText() != null ? txtInstrucciones.getText().toString().trim() : "";
            String fechaLimite = txtFechaLimite.getText() != null ? txtFechaLimite.getText().toString().trim() : "";
            String prioridadSel = spPrioridad.getSelectedItem().toString();
            int faseIndex = spFase.getSelectedItemPosition();
            int asignadoIndex = spAsignado.getSelectedItemPosition();

            if (TextUtils.isEmpty(titulo)) {
                Toast.makeText(this, "El título de la tarea es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!fechaLimite.isEmpty() && esFechaAnteriorAHoy(fechaLimite)) {
                Toast.makeText(this, "La fecha límite debe ser hoy o una fecha posterior", Toast.LENGTH_SHORT).show();
                return;
            }

            String faseIdSel = (faseIndex >= 0 && faseIndex < listaFasesProyecto.size())
                    ? listaFasesProyecto.get(faseIndex).getId()
                    : null;
            String idUsuarioAsignado = (asignadoIndex > 0 && (asignadoIndex - 1) < listaEquipoProyecto.size())
                    ? listaEquipoProyecto.get(asignadoIndex - 1).usuario.getId()
                    : null;

            Tarea nuevaTarea = new Tarea();
            nuevaTarea.setProyectoId(proyectoId);
            nuevaTarea.setFaseId(faseIdSel);
            nuevaTarea.setTitulo(titulo);
            nuevaTarea.setInstrucciones(instrucciones);
            nuevaTarea.setPrioridad(prioridadSel);
            nuevaTarea.setEstado("Pendiente");
            nuevaTarea.setFechaLimite(fechaLimite.isEmpty() ? null : fechaLimite);

            new Thread(() -> {
                boolean exito = tareaControlador.crearTarea(nuevaTarea, idUsuarioAsignado);
                runOnUiThread(() -> {
                    if (exito) {
                        Toast.makeText(frm_tareas.this, "¡Tarea creada exitosamente!", Toast.LENGTH_SHORT).show();
                        cargarTareas();
                    } else {
                        Toast.makeText(frm_tareas.this, "Error al crear la tarea en SQL Server", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private Calendar obtenerInicioDeHoy() {
        Calendar minCal = Calendar.getInstance();
        minCal.set(Calendar.HOUR_OF_DAY, 0);
        minCal.set(Calendar.MINUTE, 0);
        minCal.set(Calendar.SECOND, 0);
        minCal.set(Calendar.MILLISECOND, 0);
        return minCal;
    }

    private boolean esFechaAnteriorAHoy(String fechaStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            Date fecha = sdf.parse(fechaStr.trim());
            Date hoy = obtenerInicioDeHoy().getTime();
            return fecha == null || fecha.before(hoy);
        } catch (Exception e) {
            return true;
        }
    }
}