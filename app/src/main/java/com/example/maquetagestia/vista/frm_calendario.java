package com.example.maquetagestia.vista;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cTarea;
import com.example.maquetagestia.model.Tarea;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class frm_calendario extends AppCompatActivity {

    private static final int ESTADO_NINGUNO = 0;
    private static final int ESTADO_ROJO = 1;
    private static final int ESTADO_AMBAR = 2;
    private static final int ESTADO_PRIMARIO = 3;
    private static final int ESTADO_EQUIPO = 4;

    private MaterialButton btnMesAnterior, btnMesSiguiente;
    private TextView lblMesAnioCalendario, lblTituloCalendario, lblFechaSeleccionada, tvSinTareasCalendario;
    private LinearLayout containerChipsFechas;
    private RecyclerView rcvCuadriculaCalendario, rcvTareasCalendario;

    private cTarea tareaControlador;
    private String proyectoIdFiltro;
    private String fechaSeleccionada;
    private String fechaHoy;
    private String idUsuarioSesion;

    private final Calendar mesMostradoCal = Calendar.getInstance();
    private final TreeMap<String, List<Tarea>> tareasPorFecha = new TreeMap<>();
    private final List<DiaCalendario> celdasMes = new ArrayList<>();
    private final List<Tarea> tareasDelDia = new ArrayList<>();

    private CalendarioGridAdapter gridAdapter;
    private TareaAdapter tareasAdapter;

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
        idUsuarioSesion = Sesion.getIdUsuario(this);
        SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        fechaHoy = sdfIso.format(new Date());
        fechaSeleccionada = fechaHoy;

        mesMostradoCal.set(Calendar.DAY_OF_MONTH, 1);
        limpiarHorasCalendar(mesMostradoCal);

        btnMesAnterior = findViewById(R.id.btnMesAnterior);
        btnMesSiguiente = findViewById(R.id.btnMesSiguiente);
        lblMesAnioCalendario = findViewById(R.id.lblMesAnioCalendario);
        containerChipsFechas = findViewById(R.id.containerChipsFechas);
        lblTituloCalendario = findViewById(R.id.lblTituloCalendario);
        lblFechaSeleccionada = findViewById(R.id.lblFechaSeleccionadaCalendario);
        tvSinTareasCalendario = findViewById(R.id.tvSinTareasCalendario);
        rcvCuadriculaCalendario = findViewById(R.id.rcvCuadriculaCalendario);
        rcvTareasCalendario = findViewById(R.id.rcvTareasCalendario);

        if (!TextUtils.isEmpty(proyectoIdFiltro) && lblTituloCalendario != null) {
            lblTituloCalendario.setText("Calendario del Proyecto");
        }

        tareaControlador = new cTarea();

        if (rcvCuadriculaCalendario != null) {
            rcvCuadriculaCalendario.setLayoutManager(new GridLayoutManager(this, 7));
            gridAdapter = new CalendarioGridAdapter();
            rcvCuadriculaCalendario.setAdapter(gridAdapter);
        }

        if (rcvTareasCalendario != null) {
            rcvTareasCalendario.setLayoutManager(new LinearLayoutManager(this));
            tareasAdapter = new TareaAdapter(this, tareasDelDia, this::cargarTareasCalendario);
            rcvTareasCalendario.setAdapter(tareasAdapter);
        }

        if (btnMesAnterior != null) {
            btnMesAnterior.setOnClickListener(v -> {
                mesMostradoCal.add(Calendar.MONTH, -1);
                actualizarCuadriculaMes();
            });
        }
        if (btnMesSiguiente != null) {
            btnMesSiguiente.setOnClickListener(v -> {
                mesMostradoCal.add(Calendar.MONTH, 1);
                actualizarCuadriculaMes();
            });
        }

        actualizarCuadriculaMes();
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
        idUsuarioSesion = idUsuario;

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
                actualizarCuadriculaMes();
                renderizarChipsFechas();
                mostrarTareasParaFecha(fechaSeleccionada);
            });
        }).start();
    }

    private void actualizarCuadriculaMes() {
        if (lblMesAnioCalendario != null) {
            SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE"));
            String nombreMes = sdfMes.format(mesMostradoCal.getTime());
            if (!nombreMes.isEmpty()) {
                nombreMes = nombreMes.substring(0, 1).toUpperCase(Locale.ROOT) + nombreMes.substring(1);
            }
            lblMesAnioCalendario.setText(nombreMes);
        }

        celdasMes.clear();

        Calendar iter = (Calendar) mesMostradoCal.clone();
        iter.set(Calendar.DAY_OF_MONTH, 1);
        limpiarHorasCalendar(iter);

        int dow = iter.get(Calendar.DAY_OF_WEEK);
        int offset = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        for (int i = 0; i < offset; i++) {
            celdasMes.add(new DiaCalendario(0, null, ESTADO_NINGUNO));
        }

        Calendar hoyCal = Calendar.getInstance();
        limpiarHorasCalendar(hoyCal);

        int maxDias = iter.getActualMaximum(Calendar.DAY_OF_MONTH);
        int anio = iter.get(Calendar.YEAR);
        int mes = iter.get(Calendar.MONTH);

        for (int d = 1; d <= maxDias; d++) {
            Calendar diaCal = Calendar.getInstance();
            diaCal.set(anio, mes, d);
            limpiarHorasCalendar(diaCal);

            String fechaIso = String.format(Locale.US, "%04d-%02d-%02d", anio, mes + 1, d);
            int estado = calcularEstadoDia(fechaIso, diaCal, hoyCal);
            celdasMes.add(new DiaCalendario(d, fechaIso, estado));
        }

        if (gridAdapter != null) {
            gridAdapter.notifyDataSetChanged();
        }
    }

    private int calcularEstadoDia(String fechaIso, Calendar diaCal, Calendar hoyCal) {
        List<Tarea> lista = tareasPorFecha.get(fechaIso);
        if (lista == null || lista.isEmpty()) {
            return ESTADO_NINGUNO;
        }

        long diffMs = diaCal.getTimeInMillis() - hoyCal.getTimeInMillis();
        long diasRestantes = Math.round((double) diffMs / 86400000.0);

        boolean tienePropia = false;
        boolean propiaRoja = false;
        boolean propiaAmbar = false;

        for (Tarea t : lista) {
            String respId = t.getResponsableId() != null ? t.getResponsableId() : t.getAsignadoA();
            boolean esMia = !TextUtils.isEmpty(idUsuarioSesion) && idUsuarioSesion.equals(respId);
            if (!esMia) continue;

            tienePropia = true;
            boolean finalizada = "Finalizada".equalsIgnoreCase(t.getEstado());
            if (!finalizada && diasRestantes <= 0) {
                propiaRoja = true;
            } else if (finalizada && diasRestantes == 0) {
                // Tarea propia vence hoy
                propiaRoja = true;
            } else if (!finalizada && diasRestantes >= 1 && diasRestantes <= 3) {
                propiaAmbar = true;
            }
        }

        if (tienePropia) {
            if (propiaRoja) return ESTADO_ROJO;
            if (propiaAmbar) return ESTADO_AMBAR;
            return ESTADO_PRIMARIO;
        }
        return ESTADO_EQUIPO;
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

        int marginEnd = dpToPx(8);
        int strokePx = dpToPx(1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (Map.Entry<String, List<Tarea>> entry : tareasPorFecha.entrySet()) {
            String fecha = entry.getKey();
            List<Tarea> lista = entry.getValue();
            int count = lista != null ? lista.size() : 0;
            boolean activa = fecha.equals(fechaSeleccionada);

            MaterialButton chip = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(marginEnd);
            chip.setLayoutParams(lp);
            chip.setMinHeight(dpToPx(40));
            chip.setMaxLines(1);
            chip.setText("• " + fecha + " (" + count + ")");
            chip.setAllCaps(false);
            chip.setTextSize(12);
            chip.setCornerRadius(dpToPx(18));
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
                    if (d != null) {
                        mesMostradoCal.setTime(d);
                        mesMostradoCal.set(Calendar.DAY_OF_MONTH, 1);
                        limpiarHorasCalendar(mesMostradoCal);
                    }
                } catch (Exception ignored) {
                }
                actualizarCuadriculaMes();
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
        if (tareasAdapter != null) {
            tareasAdapter.notifyDataSetChanged();
        }
        if (tvSinTareasCalendario != null) {
            tvSinTareasCalendario.setVisibility(tareasDelDia.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void limpiarHorasCalendar(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public void volverDesdeCalendario(View view) {
        finish();
    }

    private static class DiaCalendario {
        final int numeroDia;
        final String fechaIso;
        final int estado;

        DiaCalendario(int numeroDia, String fechaIso, int estado) {
            this.numeroDia = numeroDia;
            this.fechaIso = fechaIso;
            this.estado = estado;
        }
    }

    private class CalendarioGridAdapter extends RecyclerView.Adapter<CalendarioGridAdapter.DiaViewHolder> {

        @NonNull
        @Override
        public DiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout cell = new LinearLayout(parent.getContext());
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = dpToPx(2);
            lp.setMargins(margin, margin, margin, margin);
            cell.setLayoutParams(lp);
            cell.setMinimumHeight(dpToPx(42));
            cell.setPadding(dpToPx(2), dpToPx(6), dpToPx(2), dpToPx(6));

            TextView tvDia = new TextView(parent.getContext());
            tvDia.setTextSize(13);
            tvDia.setGravity(Gravity.CENTER);
            cell.addView(tvDia);

            View dot = new View(parent.getContext());
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dpToPx(6), dpToPx(6));
            dotLp.topMargin = dpToPx(2);
            dot.setLayoutParams(dotLp);
            cell.addView(dot);

            return new DiaViewHolder(cell, tvDia, dot);
        }

        @Override
        public void onBindViewHolder(@NonNull DiaViewHolder holder, int position) {
            DiaCalendario item = celdasMes.get(position);
            if (item.numeroDia <= 0 || item.fechaIso == null) {
                holder.itemView.setVisibility(View.INVISIBLE);
                holder.itemView.setOnClickListener(null);
                return;
            }

            holder.itemView.setVisibility(View.VISIBLE);
            holder.tvDia.setText(String.valueOf(item.numeroDia));

            boolean seleccionado = item.fechaIso.equals(fechaSeleccionada);
            boolean esHoy = item.fechaIso.equals(fechaHoy);

            int bgColor = Color.TRANSPARENT;
            int textColor = ContextCompat.getColor(frm_calendario.this, R.color.on_surface);
            int dotColor = Color.TRANSPARENT;
            boolean bold = seleccionado || esHoy || item.estado != ESTADO_NINGUNO;

            if (item.estado == ESTADO_ROJO) {
                bgColor = ContextCompat.getColor(frm_calendario.this, R.color.error_container);
                textColor = ContextCompat.getColor(frm_calendario.this, R.color.error);
                dotColor = ContextCompat.getColor(frm_calendario.this, R.color.error);
            } else if (item.estado == ESTADO_AMBAR) {
                bgColor = ContextCompat.getColor(frm_calendario.this, R.color.warning_amber_container);
                textColor = ContextCompat.getColor(frm_calendario.this, R.color.warning_amber);
                dotColor = ContextCompat.getColor(frm_calendario.this, R.color.warning_amber);
            } else if (item.estado == ESTADO_PRIMARIO) {
                bgColor = ContextCompat.getColor(frm_calendario.this, R.color.primary_container);
                textColor = ContextCompat.getColor(frm_calendario.this, R.color.on_primary);
                dotColor = ContextCompat.getColor(frm_calendario.this, R.color.on_primary);
            } else if (item.estado == ESTADO_EQUIPO) {
                bgColor = ContextCompat.getColor(frm_calendario.this, R.color.surface_container_high);
                textColor = ContextCompat.getColor(frm_calendario.this, R.color.on_surface);
                dotColor = ContextCompat.getColor(frm_calendario.this, R.color.secondary);
            }

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(10));
            bg.setColor(bgColor);
            if (seleccionado) {
                bg.setStroke(dpToPx(2), ContextCompat.getColor(frm_calendario.this, R.color.primary));
            } else if (esHoy) {
                bg.setStroke(dpToPx(1), ContextCompat.getColor(frm_calendario.this, R.color.outline));
            }
            holder.itemView.setBackground(bg);

            holder.tvDia.setTextColor(textColor);
            holder.tvDia.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);

            if (item.estado != ESTADO_NINGUNO) {
                holder.dot.setVisibility(View.VISIBLE);
                GradientDrawable dotBg = new GradientDrawable();
                dotBg.setShape(GradientDrawable.OVAL);
                dotBg.setColor(dotColor);
                holder.dot.setBackground(dotBg);
            } else {
                holder.dot.setVisibility(View.INVISIBLE);
            }

            holder.itemView.setOnClickListener(v -> {
                fechaSeleccionada = item.fechaIso;
                notifyDataSetChanged();
                renderizarChipsFechas();
                mostrarTareasParaFecha(item.fechaIso);
            });
        }

        @Override
        public int getItemCount() {
            return celdasMes.size();
        }

        class DiaViewHolder extends RecyclerView.ViewHolder {
            final TextView tvDia;
            final View dot;

            DiaViewHolder(@NonNull View itemView, TextView tvDia, View dot) {
                super(itemView);
                this.tvDia = tvDia;
                this.dot = dot;
            }
        }
    }
}
