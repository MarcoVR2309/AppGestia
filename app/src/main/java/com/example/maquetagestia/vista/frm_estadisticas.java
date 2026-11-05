package com.example.maquetagestia.vista;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cDashboard;
import com.example.maquetagestia.controller.cDashboard.EstadisticasDTO;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class frm_estadisticas extends AppCompatActivity {

    private Spinner spPeriodo;
    private CircularProgressIndicator chartDonut;
    private TextView lblPorcentajeDonut, lblCompletadasCount, lblPendientesCount, lblVencidasCount;
    private cDashboard dashboardControlador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_estadisticas);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spPeriodo = findViewById(R.id.spPeriodo);
        chartDonut = findViewById(R.id.chartDonut);
        lblPorcentajeDonut = findViewById(R.id.lblPorcentajeDonut);
        lblCompletadasCount = findViewById(R.id.lblCompletadasCount);
        lblPendientesCount = findViewById(R.id.lblPendientesCount);
        lblVencidasCount = findViewById(R.id.lblVencidasCount);

        dashboardControlador = new cDashboard();

        String[] opcionesPeriodo = {"Este mes", "Mes anterior", "Últimos 3 meses", "Este año"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opcionesPeriodo);
        if (spPeriodo != null) {
            spPeriodo.setAdapter(adapter);
        }

        cargarEstadisticas();
    }

    private void cargarEstadisticas() {
        String idUsuario = Sesion.getIdUsuario(this);
        if (idUsuario == null) return;

        new Thread(() -> {
            EstadisticasDTO dto = dashboardControlador.obtenerEstadisticasDetalladas(idUsuario);

            runOnUiThread(() -> {
                if (chartDonut != null) chartDonut.setProgress(dto.porcentajeCumplimiento);
                if (lblPorcentajeDonut != null) lblPorcentajeDonut.setText(dto.porcentajeCumplimiento + "%");
                if (lblCompletadasCount != null) lblCompletadasCount.setText(String.valueOf(dto.completadas));
                if (lblPendientesCount != null) lblPendientesCount.setText(String.valueOf(dto.pendientes));
                if (lblVencidasCount != null) lblVencidasCount.setText(String.valueOf(dto.vencidas));
            });
        }).start();
    }

    public void volverAlHome(View view) {
        finish();
    }
}