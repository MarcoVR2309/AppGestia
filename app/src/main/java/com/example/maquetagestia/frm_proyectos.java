package com.example.maquetagestia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class frm_proyectos extends AppCompatActivity {
    private EditText txtBuscarProyecto;
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
    }
    public void volverAlHome(View view) {
        finish();
    }

    public void abrirDetalleProyecto(View view) {
        Intent intent = new Intent(this, frm_detalle_proyecto.class);
        startActivity(intent);
    }
}