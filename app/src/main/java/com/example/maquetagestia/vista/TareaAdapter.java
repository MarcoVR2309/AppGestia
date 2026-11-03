package com.example.maquetagestia.vista;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.cTarea;
import com.example.maquetagestia.model.Tarea;

import java.util.List;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.TareaViewHolder> {

    private final Context context;
    private final List<Tarea> listaTareas;
    private final cTarea tareaControlador;

    public TareaAdapter(Context context, List<Tarea> listaTareas) {
        this.context = context;
        this.listaTareas = listaTareas;
        this.tareaControlador = new cTarea();
    }

    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tarea, parent, false);
        return new TareaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int position) {
        Tarea t = listaTareas.get(position);

        holder.lblTitulo.setText(t.getTitulo());
        holder.lblInstrucciones.setText(t.getInstrucciones() != null && !t.getInstrucciones().isEmpty() ? t.getInstrucciones() : "Sin instrucciones");

        String fecha = t.getFechaLimite() != null ? "Límite: " + t.getFechaLimite() : "Sin fecha límite";
        holder.lblFechaLimite.setText(fecha);

        // Badge de Prioridad
        String prio = t.getPrioridad() != null ? t.getPrioridad() : "Media";
        holder.lblPrioridad.setText(prio.toUpperCase());

        if ("Urgente".equalsIgnoreCase(prio) || "Alta".equalsIgnoreCase(prio)) {
            holder.lblPrioridad.setTextColor(ContextCompat.getColor(context, R.color.error));
            holder.lblPrioridad.setBackgroundColor(ContextCompat.getColor(context, R.color.error_container));
        } else if ("Media".equalsIgnoreCase(prio)) {
            holder.lblPrioridad.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
            holder.lblPrioridad.setBackgroundColor(ContextCompat.getColor(context, R.color.warning_amber_container));
        } else {
            holder.lblPrioridad.setTextColor(ContextCompat.getColor(context, R.color.primary));
            holder.lblPrioridad.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_container));
        }

        // Estado e ícono de acción
        if ("Finalizada".equalsIgnoreCase(t.getEstado())) {
            holder.btnEstado.setImageResource(R.drawable.ic_check_circle);
            holder.btnEstado.setColorFilter(ContextCompat.getColor(context, R.color.success_green));
        } else {
            holder.btnEstado.setImageResource(R.drawable.ic_check_circle);
            holder.btnEstado.setColorFilter(ContextCompat.getColor(context, R.color.outline));
        }

        // Cambiar estado rápido
        holder.btnEstado.setOnClickListener(v -> {
            String nuevoEstado = "Finalizada".equalsIgnoreCase(t.getEstado()) ? "Pendiente" : "Finalizada";
            new Thread(() -> {
                boolean exito = tareaControlador.actualizarEstadoTarea(t.getId(), nuevoEstado);
                if (exito) {
                    t.setEstado(nuevoEstado);
                    if (context instanceof frm_tareas) {
                        ((frm_tareas) context).runOnUiThread(() -> notifyItemChanged(position));
                    }
                }
            }).start();
        });
    }

    @Override
    public int getItemCount() {
        return listaTareas.size();
    }

    public static class TareaViewHolder extends RecyclerView.ViewHolder {
        TextView lblTitulo, lblInstrucciones, lblPrioridad, lblFechaLimite;
        ImageButton btnEstado;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            lblTitulo = itemView.findViewById(R.id.lblTituloTarea);
            lblInstrucciones = itemView.findViewById(R.id.lblInstruccionesTarea);
            lblPrioridad = itemView.findViewById(R.id.lblPrioridadTarea);
            lblFechaLimite = itemView.findViewById(R.id.lblFechaLimiteTarea);
            btnEstado = itemView.findViewById(R.id.btnCambiarEstado);
        }
    }
}
