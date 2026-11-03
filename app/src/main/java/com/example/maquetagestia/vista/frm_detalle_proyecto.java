package com.example.maquetagestia.vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.example.maquetagestia.controller.cProyecto;
import com.example.maquetagestia.model.Fase;
import com.example.maquetagestia.model.Proyecto;

import java.util.List;

public class frm_detalle_proyecto extends AppCompatActivity {

    private String proyectoId;
    private RecyclerView rcvFasesStepper;
    private cProyecto proyectoControlador;
    private cFase faseControlador;
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

        rcvFasesStepper = findViewById(R.id.rcvFasesStepper);
        if (rcvFasesStepper != null) {
            rcvFasesStepper.setLayoutManager(new LinearLayoutManager(this));
        }

        proyectoControlador = new cProyecto();
        faseControlador = new cFase();

        cargarFasesDelProyecto();
    }

    private void cargarFasesDelProyecto() {
        if (proyectoId == null || proyectoId.isEmpty()) return;

        new Thread(() -> {
            List<Fase> listaFases = faseControlador.listarPorProyecto(proyectoId);

            runOnUiThread(() -> {
                if (rcvFasesStepper != null) {
                    faseAdapter = new FaseStepperAdapter(frm_detalle_proyecto.this, listaFases);
                    rcvFasesStepper.setAdapter(faseAdapter);
                }
            });
        }).start();
    }

    public void volverAProyectos(View view) {
        finish();
    }

    public void crearNuevaTarea(View view) {
        Intent intent = new Intent(this, frm_tareas.class);
        startActivity(intent);
    }

    public void abrirPantallaEquipo(View view) {
        Intent intent = new Intent(this, frm_equipo.class);
        startActivity(intent);
    }
}