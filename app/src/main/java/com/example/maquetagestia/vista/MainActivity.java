package com.example.maquetagestia.vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.maquetagestia.R;

public class MainActivity extends AppCompatActivity {
    private TextView tvGreeting;
    private ImageButton btnNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        tvGreeting = findViewById(R.id.tvGreeting);
        btnNotifications = findViewById(R.id.btnNotifications);
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
}