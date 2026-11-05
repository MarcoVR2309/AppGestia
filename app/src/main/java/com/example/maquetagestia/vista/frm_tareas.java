package com.example.maquetagestia.vista;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.cFase;
import com.example.maquetagestia.controller.cTarea;
import com.example.maquetagestia.model.Fase;
import com.example.maquetagestia.model.Tarea;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class frm_tareas extends AppCompatActivity {

    private String proyectoId;
    private RecyclerView rcvTareas;
    private cTarea tareaControlador;
    private cFase faseControlador;
    private TareaAdapter tareaAdapter;
    private List<Fase> listaFasesProyecto = new ArrayList<>();

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

        rcvTareas = findViewById(R.id.rcvTareas);
        if (rcvTareas != null) {
            rcvTareas.setLayoutManager(new LinearLayoutManager(this));
        }

        tareaControlador = new cTarea();
        faseControlador = new cFase();

        cargarTareas();
    }

    public void cargarTareas() {
        if (proyectoId == null || proyectoId.isEmpty()) return;

        new Thread(() -> {
            List<Tarea> lista = tareaControlador.listarTareasPorProyecto(proyectoId);
            listaFasesProyecto = faseControlador.listarPorProyecto(proyectoId);

            runOnUiThread(() -> {
                if (rcvTareas != null) {
                    tareaAdapter = new TareaAdapter(frm_tareas.this, lista);
                    rcvTareas.setAdapter(tareaAdapter);
                }
            });
        }).start();
    }

    public void volverAtras(View view) {
        finish();
    }

    public void agregarNuevaTarea(View view) {
        if (listaFasesProyecto.isEmpty()) {
            Toast.makeText(this, "Cargando fases del proyecto...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear diálogo modal M3 para registrar tarea
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Nueva Tarea");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nueva_tarea, null, false);
        EditText txtTitulo = dialogView.findViewById(R.id.txtTituloTareaDialog);
        EditText txtInstrucciones = dialogView.findViewById(R.id.txtInstruccionesDialog);
        Spinner spPrioridad = dialogView.findViewById(R.id.spPrioridadDialog);
        Spinner spFase = dialogView.findViewById(R.id.spFaseDialog);

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

        builder.setView(dialogView);
        builder.setPositiveButton("Crear", (dialog, which) -> {
            String titulo = txtTitulo.getText() != null ? txtTitulo.getText().toString().trim() : "";
            String instrucciones = txtInstrucciones.getText() != null ? txtInstrucciones.getText().toString().trim() : "";
            String prioridadSel = spPrioridad.getSelectedItem().toString();
            int faseIndex = spFase.getSelectedItemPosition();

            if (TextUtils.isEmpty(titulo)) {
                Toast.makeText(this, "El título de la tarea es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            String faseIdSel = listaFasesProyecto.get(faseIndex).getId();

            Tarea nuevaTarea = new Tarea();
            nuevaTarea.setProyectoId(proyectoId);
            nuevaTarea.setFaseId(faseIdSel);
            nuevaTarea.setTitulo(titulo);
            nuevaTarea.setInstrucciones(instrucciones);
            nuevaTarea.setPrioridad(prioridadSel);
            nuevaTarea.setEstado("Pendiente");
            nuevaTarea.setFechaLimite(null); // Evita error de conversíon datetime2 en SQL Server

            new Thread(() -> {
                boolean exito = tareaControlador.crearTarea(nuevaTarea, null);
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
}