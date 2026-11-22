package com.example.maquetagestia.vista;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
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
import com.example.maquetagestia.controller.cNotificacion;
import com.example.maquetagestia.model.Notificacion;

import java.util.ArrayList;
import java.util.List;

public class frm_notificaciones extends AppCompatActivity {

    private RecyclerView rcvNotificaciones;
    private TextView tvSinNotificaciones;
    private cNotificacion notificacionControlador;
    private NotificacionAdapter notificacionAdapter;
    private List<Notificacion> listaNotificaciones = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_notificaciones);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rcvNotificaciones = findViewById(R.id.rcvNotificaciones);
        tvSinNotificaciones = findViewById(R.id.tvSinNotificaciones);

        if (rcvNotificaciones != null) {
            rcvNotificaciones.setLayoutManager(new LinearLayoutManager(this));
        }

        notificacionControlador = new cNotificacion();
        cargarNotificaciones();
    }

    private void cargarNotificaciones() {
        String idUsuario = Sesion.getIdUsuario(this);
        if (idUsuario == null || idUsuario.trim().isEmpty()) {
            mostrarEstadoVacio(true);
            return;
        }

        new Thread(() -> {
            List<Notificacion> resultado = notificacionControlador.listarPorUsuario(idUsuario);

            runOnUiThread(() -> {
                listaNotificaciones = resultado != null ? resultado : new ArrayList<>();
                if (listaNotificaciones.isEmpty()) {
                    mostrarEstadoVacio(true);
                } else {
                    mostrarEstadoVacio(false);
                    notificacionAdapter = new NotificacionAdapter(frm_notificaciones.this, listaNotificaciones);
                    if (rcvNotificaciones != null) {
                        rcvNotificaciones.setAdapter(notificacionAdapter);
                    }
                }
            });
        }).start();
    }

    private void mostrarEstadoVacio(boolean vacio) {
        if (tvSinNotificaciones != null) {
            tvSinNotificaciones.setVisibility(vacio ? View.VISIBLE : View.GONE);
        }
        if (rcvNotificaciones != null) {
            rcvNotificaciones.setVisibility(vacio ? View.GONE : View.VISIBLE);
        }
    }

    public void volverAlHome(View view) {
        finish();
    }

    public void marcarTodasLeidas(View view) {
        String idUsuario = Sesion.getIdUsuario(this);
        if (idUsuario == null || idUsuario.trim().isEmpty()) return;

        new Thread(() -> {
            boolean exito = notificacionControlador.marcarTodasComoLeidas(idUsuario);
            runOnUiThread(() -> {
                if (exito) {
                    for (Notificacion n : listaNotificaciones) {
                        if (!"Invitacion".equalsIgnoreCase(n.getTipo())) {
                            n.setLeido(true);
                        }
                    }
                    if (notificacionAdapter != null) {
                        notificacionAdapter.notifyDataSetChanged();
                    }
                    Toast.makeText(frm_notificaciones.this, "Todas las notificaciones fueron leídas", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(frm_notificaciones.this, "Error al marcar las notificaciones como leídas", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}
