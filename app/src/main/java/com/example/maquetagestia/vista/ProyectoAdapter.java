package com.example.maquetagestia.vista;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.model.Proyecto;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ProyectoAdapter extends RecyclerView.Adapter<ProyectoAdapter.ProyectoViewHolder> {

    private final Context context;
    private final List<Proyecto> listaProyectos;

    public ProyectoAdapter(Context context, List<Proyecto> listaProyectos) {
        this.context = context;
        this.listaProyectos = listaProyectos;
    }

    @NonNull
    @Override
    public ProyectoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_proyecto, parent, false);
        return new ProyectoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProyectoViewHolder holder, int position) {
        Proyecto p = listaProyectos.get(position);

        holder.lblTipo.setText(p.getTipo() != null ? p.getTipo().toUpperCase() : "PROYECTO");
        holder.lblNombre.setText(p.getNombre());
        holder.lblDescripcion.setText(p.getDescripcion() != null ? p.getDescripcion() : "Sin descripción");
        holder.lblEstado.setText(p.getEstado() != null ? p.getEstado() : "Planificación");

        String fechas = (p.getFechaInicio() != null ? p.getFechaInicio() : "Inicio") +
                        " - " +
                        (p.getFechaFinEstimada() != null ? p.getFechaFinEstimada() : "Fin");
        holder.lblFechas.setText(fechas);

        // Avance real según fases completadas del proyecto
        int progreso = Math.max(0, Math.min(100, p.getProgresoPorcentaje()));
        holder.progressAvance.setProgress(progreso);
        if (holder.lblPorcentaje != null) {
            holder.lblPorcentaje.setText(progreso + "%");
        }

        // Clic para abrir Detalle de Proyecto pasando su ID
        holder.cardProyecto.setOnClickListener(v -> {
            Intent intent = new Intent(context, frm_detalle_proyecto.class);
            intent.putExtra("PROYECTO_ID", p.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listaProyectos.size();
    }

    public static class ProyectoViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardProyecto;
        TextView lblTipo, lblNombre, lblDescripcion, lblEstado, lblFechas, lblPorcentaje;
        ProgressBar progressAvance;

        public ProyectoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardProyecto = itemView.findViewById(R.id.cardProyecto);
            lblTipo = itemView.findViewById(R.id.lblTipoProyecto);
            lblNombre = itemView.findViewById(R.id.lblNombreProyecto);
            lblDescripcion = itemView.findViewById(R.id.lblDescripcionProyecto);
            lblEstado = itemView.findViewById(R.id.lblEstadoProyecto);
            lblFechas = itemView.findViewById(R.id.lblFechasProyecto);
            lblPorcentaje = itemView.findViewById(R.id.lblPorcentajeProyecto);
            progressAvance = itemView.findViewById(R.id.progressAvance);
        }
    }
}
