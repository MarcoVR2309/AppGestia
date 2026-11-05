package com.example.maquetagestia.vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cProyecto;
import com.example.maquetagestia.model.Proyecto;

import java.util.List;

public class frm_proyectos extends AppCompatActivity {

    private EditText txtBuscarProyecto;
    private RecyclerView rcvProyectos;
    private cProyecto proyectoControlador;
    private ProyectoAdapter proyectoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_proyectos);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtBuscarProyecto = findViewById(R.id.txtBuscarProyecto);
        rcvProyectos = findViewById(R.id.rcvProyectos);

        if (rcvProyectos != null) {
            rcvProyectos.setLayoutManager(new LinearLayoutManager(this));
        }

        proyectoControlador = new cProyecto();

        cargarProyectos("Todos");

        // Abrir automáticamente el diálogo de creación si vino desde el botón + del Dashboard
        if (getIntent().getBooleanExtra("ABRIR_DIALOGO_CREAR", false)) {
            mostrarDialogoCrearProyecto();
        }
    }

    public void cargarProyectos(String filtroTipo) {
        String usuarioId = Sesion.getIdUsuario(this);
        if (usuarioId == null) return;

        new Thread(() -> {
            List<Proyecto> lista = proyectoControlador.listarPorUsuario(usuarioId, filtroTipo);

            runOnUiThread(() -> {
                if (rcvProyectos != null) {
                    proyectoAdapter = new ProyectoAdapter(frm_proyectos.this, lista);
                    rcvProyectos.setAdapter(proyectoAdapter);
                }
            });
        }).start();
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

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle("Nuevo Proyecto");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nuevo_proyecto, null, false);
        EditText txtNombre = dialogView.findViewById(R.id.txtNombreProyectoDialog);
        EditText txtDescripcion = dialogView.findViewById(R.id.txtDescripcionProyectoDialog);
        android.widget.Spinner spTipo = dialogView.findViewById(R.id.spTipoProyectoDialog);
        EditText txtFechaInicio = dialogView.findViewById(R.id.txtFechaInicioDialog);
        EditText txtFechaFin = dialogView.findViewById(R.id.txtFechaFinDialog);

        String[] tipos = {"Academico", "Empresarial"};
        android.widget.ArrayAdapter<String> adapterTipo = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, tipos);
        if (spTipo != null) {
            spTipo.setAdapter(adapterTipo);
        }

        builder.setView(dialogView);
        builder.setPositiveButton("Crear Proyecto", (dialog, which) -> {
            String nombre = txtNombre.getText() != null ? txtNombre.getText().toString().trim() : "";
            String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().toString().trim() : "";
            String tipoSel = spTipo != null && spTipo.getSelectedItem() != null ? spTipo.getSelectedItem().toString() : "Academico";
            String fechaInicioInput = txtFechaInicio.getText() != null ? txtFechaInicio.getText().toString().trim() : "";
            String fechaFinInput = txtFechaFin.getText() != null ? txtFechaFin.getText().toString().trim() : "";

            if (android.text.TextUtils.isEmpty(nombre)) {
                Toast.makeText(this, "El nombre del proyecto es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            // Fecha actual como respaldo en formato YYYY-MM-DD
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            String fechaHoy = sdf.format(new java.util.Date());

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
                boolean exito = proyectoControlador.crearProyectoConFases(nuevoProyecto, usuarioId);
                runOnUiThread(() -> {
                    if (exito) {
                        Toast.makeText(frm_proyectos.this, "¡Proyecto y fases creados exitosamente!", Toast.LENGTH_SHORT).show();
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