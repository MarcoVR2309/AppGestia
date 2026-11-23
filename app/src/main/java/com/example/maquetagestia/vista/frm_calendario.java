package com.example.maquetagestia.vista;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.example.maquetagestia.controller.cTarea;
import com.example.maquetagestia.model.Tarea;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class frm_calendario extends AppCompatActivity {

    private CalendarView calendarViewEntregas;
    private LinearLayout containerChipsFechas;
    private TextView lblTituloCalendario, lblFechaSeleccionada, tvSinTareasCalendario;
    private RecyclerView rcvTareasCalendario;

    private cTarea tareaControlador;
    private String proyectoIdFiltro;
    private String fechaSeleccionada;

    private final TreeMap<String, List<Tarea>> tareasPorFecha = new TreeMap<>();
    private final List<Tarea> tareasDelDia = new ArrayList<>();
    private TareaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Sesion.estaActiva(this)) {
            startActivity(new Intent(this, frm_login.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_calendario);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        proyectoIdFiltro = getIntent().getStringExtra("PROYECTO_ID");
        fechaSeleccionada = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        calendarViewEntregas = findViewById(R.id.calendarViewEntregas);
        containerChipsFechas = findViewById(R.id.containerChipsFechas);
        lblTituloCalendario = findViewById(R.id.lblTituloCalendario);
        lblFechaSeleccionada = findViewById(R.id.lblFechaSeleccionadaCalendario);
        tvSinTareasCalendario = findViewById(R.id.tvSinTareasCalendario);
        rcvTareasCalendario = findViewById(R.id.rcvTareasCalendario);

        if (!TextUtils.isEmpty(proyectoIdFiltro) && lblTituloCalendario != null) {
            lblTituloCalendario.setText("Calendario del Proyecto");
        }

        tareaControlador = new cTarea();

        if (rcvTareasCalendario != null) {
            rcvTareasCalendario.setLayoutManager(new LinearLayoutManager(this));
            adapter = new TareaAdapter(this, tareasDelDia, this::cargarTareasCalendario);
            rcvTareasCalendario.setAdapter(adapter);
        }

        if (calendarViewEntregas != null) {
            calendarViewEntregas.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                fechaSeleccionada = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                mostrarTareasParaFecha(fechaSeleccionada);
                renderizarChipsFechas();
            });
        }

        cargarTareasCalendario();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarTareasCalendario();
    }

    private void cargarTareasCalendario() {
        String idUsuario = Sesion.getIdUsuario(this);
        if (TextUtils.isEmpty(idUsuario)) return;

        new Thread(() -> {
            List<Tarea> lista = tareaControlador.listarTareasConFechaPorUsuario(idUsuario, proyectoIdFiltro);
            TreeMap<String, List<Tarea>> agrupadas = new TreeMap<>();
            if (lista != null) {
                for (Tarea t : lista) {
                    String f = t.getFechaLimite();
                    if (f != null && f.trim().length() >= 10) {
                        String key = f.trim().substring(0, 10);
                        List<Tarea> grupo = agrupadas.get(key);
                        if (grupo == null) {
                            grupo = new ArrayList<>();
                            agrupadas.put(key, grupo);
                        }
                        grupo.add(t);
                    }
                }
            }

            runOnUiThread(() -> {
                tareasPorFecha.clear();
                tareasPorFecha.putAll(agrupadas);
                renderizarChipsFechas();
                mostrarTareasParaFecha(fechaSeleccionada);
            });
        }).start();
    }

    private void renderizarChipsFechas() {
        if (containerChipsFechas == null) return;
        containerChipsFechas.removeAllViews();

        if (tareasPorFecha.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Sin entregas con fecha límite registrada");
            tv.setTextSize(12);
            tv.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            containerChipsFechas.addView(tv);
            return;
        }

        int marginEnd = Math.round(8 * getResources().getDisplayMetrics().density);
        int strokePx = Math.round(1 * getResources().getDisplayMetrics().density);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (Map.Entry<String, List<Tarea>> entry : tareasPorFecha.entrySet()) {
            String fecha = entry.getKey();
            List<Tarea> lista = entry.getValue();
            int count = lista != null ? lista.size() : 0;
            boolean activa = fecha.equals(fechaSeleccionada);

            MaterialButton chip = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    Math.round(36 * getResources().getDisplayMetrics().density)
            );
            lp.setMarginEnd(marginEnd);
            chip.setLayoutParams(lp);
            chip.setText("• " + fecha + " (" + count + ")");
            chip.setAllCaps(false);
            chip.setTextSize(12);
            chip.setCornerRadius(Math.round(18 * getResources().getDisplayMetrics().density));
            chip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    this, activa ? R.color.primary_container : android.R.color.transparent)));
            chip.setTextColor(ContextCompat.getColor(
                    this, activa ? R.color.on_primary : R.color.on_surface));
            chip.setStrokeWidth(activa ? 0 : strokePx);
            chip.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.outline_variant)));

            chip.setOnClickListener(v -> {
                fechaSeleccionada = fecha;
                try {
                    Date d = sdf.parse(fecha);
                    if (d != null && calendarViewEntregas != null) {
                        calendarViewEntregas.setDate(d.getTime(), true, true);
                    }
                } catch (Exception ignored) {
                }
                mostrarTareasParaFecha(fecha);
                renderizarChipsFechas();
            });
            containerChipsFechas.addView(chip);
        }
    }

    private void mostrarTareasParaFecha(String fecha) {
        if (lblFechaSeleccionada != null) {
            lblFechaSeleccionada.setText("Entregas para " + fecha);
        }
        tareasDelDia.clear();
        List<Tarea> encontradas = tareasPorFecha.get(fecha);
        if (encontradas != null) {
            tareasDelDia.addAll(encontradas);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (tvSinTareasCalendario != null) {
            tvSinTareasCalendario.setVisibility(tareasDelDia.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    public void volverDesdeCalendario(View view) {
        finish();
    }
}
