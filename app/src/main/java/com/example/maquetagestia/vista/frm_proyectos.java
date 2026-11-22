package com.example.maquetagestia.vista;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
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
import com.example.maquetagestia.controller.cProyecto;
import com.example.maquetagestia.model.Proyecto;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class frm_proyectos extends AppCompatActivity {

    private static final String CHIP_TODOS = "TODOS";
    private static final String CHIP_ACTIVOS = "ACTIVOS";
    private static final String CHIP_ACADEMICOS = "Academico";
    private static final String CHIP_EMPRESARIALES = "Empresarial";

    private static final String ORDEN_RECIENTES = "RECIENTES";
    private static final String ORDEN_NOMBRE_AZ = "NOMBRE_AZ";
    private static final String ORDEN_PROGRESO = "PROGRESO";

    private EditText txtBuscarProyecto;
    private ImageButton btnFiltrar;
    private MaterialButton btnChipTodos, btnChipActivos, btnChipAcademicos, btnChipEmpresariales;
    private TextView tvSinProyectos;
    private RecyclerView rcvProyectos;
    private cProyecto proyectoControlador;
    private ProyectoAdapter proyectoAdapter;
    private List<Proyecto> listaProyectosCompleta = new ArrayList<>();
    private final List<Proyecto> listaProyectosFiltrada = new ArrayList<>();
    private String chipActual = CHIP_TODOS;
    private String ordenActual = ORDEN_RECIENTES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Sesion.estaActiva(this)) {
            startActivity(new Intent(this, frm_login.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_proyectos);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtBuscarProyecto = findViewById(R.id.txtBuscarProyecto);
        btnFiltrar = findViewById(R.id.btnFiltrar);
        btnChipTodos = findViewById(R.id.btnChipTodos);
        btnChipActivos = findViewById(R.id.btnChipActivos);
        btnChipAcademicos = findViewById(R.id.btnChipAcademicos);
        btnChipEmpresariales = findViewById(R.id.btnChipEmpresariales);
        tvSinProyectos = findViewById(R.id.tvSinProyectos);
        rcvProyectos = findViewById(R.id.rcvProyectos);

        if (rcvProyectos != null) {
            rcvProyectos.setLayoutManager(new LinearLayoutManager(this));
        }

        proyectoControlador = new cProyecto();

        configurarFiltrosYBuscador();
        cargarProyectos("Todos");

        // Abrir automáticamente el diálogo de creación si vino desde el botón + del Dashboard
        if (getIntent().getBooleanExtra("ABRIR_DIALOGO_CREAR", false)) {
            mostrarDialogoCrearProyecto();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarProyectos("Todos");
    }

    private void configurarFiltrosYBuscador() {
        if (btnChipTodos != null) {
            btnChipTodos.setOnClickListener(v -> {
                chipActual = CHIP_TODOS;
                aplicarFiltrosProyectos();
            });
        }
        if (btnChipActivos != null) {
            btnChipActivos.setOnClickListener(v -> {
                chipActual = CHIP_ACTIVOS;
                aplicarFiltrosProyectos();
            });
        }
        if (btnChipAcademicos != null) {
            btnChipAcademicos.setOnClickListener(v -> {
                chipActual = CHIP_ACADEMICOS;
                aplicarFiltrosProyectos();
            });
        }
        if (btnChipEmpresariales != null) {
            btnChipEmpresariales.setOnClickListener(v -> {
                chipActual = CHIP_EMPRESARIALES;
                aplicarFiltrosProyectos();
            });
        }

        if (txtBuscarProyecto != null) {
            txtBuscarProyecto.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    aplicarFiltrosProyectos();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        if (btnFiltrar != null) {
            btnFiltrar.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(frm_proyectos.this, btnFiltrar);
                popup.getMenu().add(0, 1, 0, "Más recientes");
                popup.getMenu().add(0, 2, 1, "Nombre (A-Z)");
                popup.getMenu().add(0, 3, 2, "Mayor progreso");
                popup.getMenu().add(0, 4, 3, "Limpiar filtros");
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == 1) {
                        ordenActual = ORDEN_RECIENTES;
                        aplicarFiltrosProyectos();
                        return true;
                    } else if (id == 2) {
                        ordenActual = ORDEN_NOMBRE_AZ;
                        aplicarFiltrosProyectos();
                        return true;
                    } else if (id == 3) {
                        ordenActual = ORDEN_PROGRESO;
                        aplicarFiltrosProyectos();
                        return true;
                    } else if (id == 4) {
                        chipActual = CHIP_TODOS;
                        ordenActual = ORDEN_RECIENTES;
                        if (txtBuscarProyecto != null) {
                            txtBuscarProyecto.setText("");
                        }
                        aplicarFiltrosProyectos();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        actualizarEstiloChips();
    }

    public void cargarProyectos(String filtroTipo) {
        String usuarioId = Sesion.getIdUsuario(this);
        if (usuarioId == null) return;

        new Thread(() -> {
            List<Proyecto> lista = proyectoControlador.listarPorUsuario(usuarioId, filtroTipo);

            runOnUiThread(() -> {
                listaProyectosCompleta = lista != null ? lista : new ArrayList<>();
                aplicarFiltrosProyectos();
            });
        }).start();
    }

    private void aplicarFiltrosProyectos() {
        actualizarEstiloChips();

        String busqueda = (txtBuscarProyecto != null && txtBuscarProyecto.getText() != null)
                ? txtBuscarProyecto.getText().toString().trim().toLowerCase(Locale.ROOT)
                : "";

        listaProyectosFiltrada.clear();
        for (Proyecto p : listaProyectosCompleta) {
            boolean coincideChip = false;
            if (CHIP_TODOS.equals(chipActual)) {
                coincideChip = true;
            } else if (CHIP_ACTIVOS.equals(chipActual)) {
                String est = p.getEstado();
                coincideChip = est == null
                        || (!"Finalizado".equalsIgnoreCase(est) && !"Completado".equalsIgnoreCase(est));
            } else {
                coincideChip = chipActual.equalsIgnoreCase(p.getTipo());
            }

            if (!coincideChip) continue;

            if (!busqueda.isEmpty()) {
                String nombre = p.getNombre() != null ? p.getNombre().toLowerCase(Locale.ROOT) : "";
                String desc = p.getDescripcion() != null ? p.getDescripcion().toLowerCase(Locale.ROOT) : "";
                if (!nombre.contains(busqueda) && !desc.contains(busqueda)) {
                    continue;
                }
            }

            listaProyectosFiltrada.add(p);
        }

        if (ORDEN_NOMBRE_AZ.equals(ordenActual)) {
            Collections.sort(listaProyectosFiltrada, (p1, p2) -> {
                String n1 = p1.getNombre() != null ? p1.getNombre() : "";
                String n2 = p2.getNombre() != null ? p2.getNombre() : "";
                return n1.compareToIgnoreCase(n2);
            });
        } else if (ORDEN_PROGRESO.equals(ordenActual)) {
            Collections.sort(listaProyectosFiltrada, (p1, p2) ->
                    Integer.compare(p2.getProgresoPorcentaje(), p1.getProgresoPorcentaje()));
        } else {
            Collections.sort(listaProyectosFiltrada, (p1, p2) -> {
                String f1 = p1.getCreatedAt() != null ? p1.getCreatedAt() : "";
                String f2 = p2.getCreatedAt() != null ? p2.getCreatedAt() : "";
                return f2.compareTo(f1);
            });
        }

        if (listaProyectosFiltrada.isEmpty()) {
            if (tvSinProyectos != null) tvSinProyectos.setVisibility(View.VISIBLE);
            if (rcvProyectos != null) rcvProyectos.setVisibility(View.GONE);
        } else {
            if (tvSinProyectos != null) tvSinProyectos.setVisibility(View.GONE);
            if (rcvProyectos != null) {
                rcvProyectos.setVisibility(View.VISIBLE);
                proyectoAdapter = new ProyectoAdapter(frm_proyectos.this, listaProyectosFiltrada);
                rcvProyectos.setAdapter(proyectoAdapter);
            }
        }
    }

    private void actualizarEstiloChips() {
        actualizarChip(btnChipTodos, CHIP_TODOS.equals(chipActual));
        actualizarChip(btnChipActivos, CHIP_ACTIVOS.equals(chipActual));
        actualizarChip(btnChipAcademicos, CHIP_ACADEMICOS.equals(chipActual));
        actualizarChip(btnChipEmpresariales, CHIP_EMPRESARIALES.equals(chipActual));
    }

    private void actualizarChip(MaterialButton boton, boolean activo) {
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

    public void volverAlHome(View view) {
        finish();
    }

    public void abrirDetalleProyecto(View view) {
        Intent intent = new Intent(this, frm_detalle_proyecto.class);
        startActivity(intent);
    }

    public void mostrarDialogoCrearProyecto(View view) {
        mostrarDialogoCrearProyecto();
    }

    public void mostrarDialogoCrearProyecto() {
        String usuarioId = Sesion.getIdUsuario(this);
        if (usuarioId == null) {
            Toast.makeText(this, "Debe iniciar sesión para crear un proyecto", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Nuevo Proyecto");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nuevo_proyecto, null, false);
        EditText txtNombre = dialogView.findViewById(R.id.txtNombreProyectoDialog);
        EditText txtDescripcion = dialogView.findViewById(R.id.txtDescripcionProyectoDialog);
        Spinner spTipo = dialogView.findViewById(R.id.spTipoProyectoDialog);
        EditText txtFechaInicio = dialogView.findViewById(R.id.txtFechaInicioDialog);
        EditText txtFechaFin = dialogView.findViewById(R.id.txtFechaFinDialog);

        String[] tipos = {"Academico", "Empresarial"};
        ArrayAdapter<String> adapterTipo = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, tipos);
        if (spTipo != null) {
            spTipo.setAdapter(adapterTipo);
        }

        View.OnClickListener selectorFecha = v -> {
            EditText target = (EditText) v;
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(
                    frm_proyectos.this,
                    (picker, year, month, dayOfMonth) -> target.setText(
                            String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            dp.show();
        };
        txtFechaInicio.setOnClickListener(selectorFecha);
        txtFechaFin.setOnClickListener(selectorFecha);

        builder.setView(dialogView);
        builder.setPositiveButton("Crear Proyecto", (dialog, which) -> {
            String nombre = txtNombre.getText() != null ? txtNombre.getText().toString().trim() : "";
            String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().toString().trim() : "";
            String tipoSel = spTipo != null && spTipo.getSelectedItem() != null ? spTipo.getSelectedItem().toString() : "Academico";
            String fechaInicioInput = txtFechaInicio.getText() != null ? txtFechaInicio.getText().toString().trim() : "";
            String fechaFinInput = txtFechaFin.getText() != null ? txtFechaFin.getText().toString().trim() : "";

            if (TextUtils.isEmpty(nombre)) {
                Toast.makeText(this, "El nombre del proyecto es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);

            if (!fechaInicioInput.isEmpty() && !fechaFinInput.isEmpty()) {
                try {
                    Date dIni = sdf.parse(fechaInicioInput);
                    Date dFin = sdf.parse(fechaFinInput);
                    if (dIni != null && dFin != null && dFin.before(dIni)) {
                        Toast.makeText(this, "La fecha fin no puede ser anterior a la fecha inicio", Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (Exception ignored) {
                }
            }

            // Fecha actual como respaldo en formato YYYY-MM-DD
            String fechaHoy = sdf.format(new Date());
            String fechaInicio = !fechaInicioInput.isEmpty() ? fechaInicioInput : fechaHoy;
            String fechaFin = !fechaFinInput.isEmpty() ? fechaFinInput : fechaHoy;

            Proyecto nuevoProyecto = new Proyecto();
            nuevoProyecto.setNombre(nombre);
            nuevoProyecto.setDescripcion(descripcion);
            nuevoProyecto.setTipo(tipoSel);
            nuevoProyecto.setEstado("Planificacion");
            nuevoProyecto.setFechaInicio(fechaInicio);
            nuevoProyecto.setFechaFinEstimada(fechaFin);
            nuevoProyecto.setCreadoPor(usuarioId);

            new Thread(() -> {
                boolean exito = proyectoControlador.crearProyecto(nuevoProyecto, usuarioId);
                runOnUiThread(() -> {
                    if (exito) {
                        Toast.makeText(frm_proyectos.this, "¡Proyecto creado exitosamente!", Toast.LENGTH_SHORT).show();
                        cargarProyectos("Todos");
                    } else {
                        Toast.makeText(frm_proyectos.this, "Error al crear el proyecto en SQL Server", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}