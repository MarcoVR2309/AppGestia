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
}