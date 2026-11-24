package com.example.maquetagestia.vista;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
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
import com.example.maquetagestia.controller.cDashboard.ProyectoParticipacionItem;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class frm_estadisticas extends AppCompatActivity {

    private Spinner spPeriodo, spProyectoEstadisticas;
    private CircularProgressIndicator chartDonut, chartDonutProyecto;
    private TextView lblPorcentajeDonut, lblCompletadasCount, lblPendientesCount, lblVencidasCount;
    private TextView lblPorcentajeDonutProyecto, lblProyectoTotalCount, lblProyectoCompletadasCount,
            lblProyectoPendientesCount, lblProyectoVencidasCount;
    private cDashboard dashboardControlador;
    private final List<ProyectoParticipacionItem> listaProyectosUsuario = new ArrayList<>();

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

        spProyectoEstadisticas = findViewById(R.id.spProyectoEstadisticas);
        chartDonutProyecto = findViewById(R.id.chartDonutProyecto);
        lblPorcentajeDonutProyecto = findViewById(R.id.lblPorcentajeDonutProyecto);
        lblProyectoTotalCount = findViewById(R.id.lblProyectoTotalCount);
        lblProyectoCompletadasCount = findViewById(R.id.lblProyectoCompletadasCount);
        lblProyectoPendientesCount = findViewById(R.id.lblProyectoPendientesCount);
        lblProyectoVencidasCount = findViewById(R.id.lblProyectoVencidasCount);

        dashboardControlador = new cDashboard();

        String[] opcionesPeriodo = {"Este mes", "Mes anterior", "Últimos 3 meses", "Este año", "Todo"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opcionesPeriodo);
        if (spPeriodo != null) {
            spPeriodo.setAdapter(adapter);
            spPeriodo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    cargarEstadisticasPersonales(opcionesPeriodo[position]);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else {
            cargarEstadisticasPersonales("Este mes");
        }

        cargarProyectosDelUsuario();
    }

    private void cargarEstadisticasPersonales(String periodo) {
        String idUsuario = Sesion.getIdUsuario(this);
        if (TextUtils.isEmpty(idUsuario)) return;

        new Thread(() -> {
            EstadisticasDTO dto = dashboardControlador.obtenerEstadisticasDetalladas(idUsuario, periodo);

            runOnUiThread(() -> {
                if (chartDonut != null) chartDonut.setProgress(dto.porcentajeCumplimiento);
                if (lblPorcentajeDonut != null) lblPorcentajeDonut.setText(dto.porcentajeCumplimiento + "%");
                if (lblCompletadasCount != null) lblCompletadasCount.setText(String.valueOf(dto.completadas));
                if (lblPendientesCount != null) lblPendientesCount.setText(String.valueOf(dto.pendientes));
                if (lblVencidasCount != null) lblVencidasCount.setText(String.valueOf(dto.vencidas));
            });
        }).start();
    }

    private void cargarProyectosDelUsuario() {
        String idUsuario = Sesion.getIdUsuario(this);
        if (TextUtils.isEmpty(idUsuario) || spProyectoEstadisticas == null) return;

        new Thread(() -> {
            List<ProyectoParticipacionItem> proyectos = dashboardControlador.listarProyectosConRolPorUsuario(idUsuario);

            runOnUiThread(() -> {
                listaProyectosUsuario.clear();
                if (proyectos != null) {
                    listaProyectosUsuario.addAll(proyectos);
                }

                if (listaProyectosUsuario.isEmpty()) {
                    List<String> vacio = new ArrayList<>();
                    vacio.add("Sin proyectos");
                    spProyectoEstadisticas.setAdapter(new ArrayAdapter<>(
                            frm_estadisticas.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            vacio
                    ));
                    spProyectoEstadisticas.setEnabled(false);
                    actualizarUiEstadisticasProyecto(new EstadisticasDTO());
                    return;
                }

                spProyectoEstadisticas.setEnabled(true);
                ArrayAdapter<ProyectoParticipacionItem> adapterPry = new ArrayAdapter<>(
                        frm_estadisticas.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        listaProyectosUsuario
                );
                spProyectoEstadisticas.setAdapter(adapterPry);
                spProyectoEstadisticas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position >= 0 && position < listaProyectosUsuario.size()) {
                            cargarEstadisticasProyecto(listaProyectosUsuario.get(position).proyectoId);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            });
        }).start();
    }

    private void cargarEstadisticasProyecto(String proyectoId) {
        if (TextUtils.isEmpty(proyectoId)) {
            actualizarUiEstadisticasProyecto(new EstadisticasDTO());
            return;
        }

        new Thread(() -> {
            EstadisticasDTO dto = dashboardControlador.obtenerEstadisticasGeneralesProyecto(proyectoId);
            runOnUiThread(() -> actualizarUiEstadisticasProyecto(dto));
        }).start();
    }

    private void actualizarUiEstadisticasProyecto(EstadisticasDTO dto) {
        int pct = Math.max(0, Math.min(100, dto.porcentajeCumplimiento));
        if (chartDonutProyecto != null) chartDonutProyecto.setProgress(pct);
        if (lblPorcentajeDonutProyecto != null) lblPorcentajeDonutProyecto.setText(pct + "%");
        if (lblProyectoTotalCount != null) lblProyectoTotalCount.setText(String.valueOf(dto.totalAsignadas));
        if (lblProyectoCompletadasCount != null) lblProyectoCompletadasCount.setText(String.valueOf(dto.completadas));
        if (lblProyectoPendientesCount != null) lblProyectoPendientesCount.setText(String.valueOf(dto.pendientes));
        if (lblProyectoVencidasCount != null) lblProyectoVencidasCount.setText(String.valueOf(dto.vencidas));
    }

    public void volverAlHome(View view) {
        finish();
    }
}
