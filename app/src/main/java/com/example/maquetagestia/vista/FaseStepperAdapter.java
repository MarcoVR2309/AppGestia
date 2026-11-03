package com.example.maquetagestia.vista;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.model.Fase;

import java.util.List;

public class FaseStepperAdapter extends RecyclerView.Adapter<FaseStepperAdapter.FaseViewHolder> {

    private final Context context;
    private final List<Fase> listaFases;

    public FaseStepperAdapter(Context context, List<Fase> listaFases) {
        this.context = context;
        this.listaFases = listaFases;
    }

    @NonNull
    @Override
    public FaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fase_stepper, parent, false);
        return new FaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaseViewHolder holder, int position) {
        Fase f = listaFases.get(position);

        holder.lblNombre.setText(f.getOrden() + ". " + f.getNombre());
        holder.lblEstado.setText(f.getEstado() != null ? f.getEstado() : "Pendiente");

        String fecha = f.getFechaEstimada() != null && !f.getFechaEstimada().isEmpty()
                ? "Estimada: " + f.getFechaEstimada()
                : "Sin fecha asignada";
        holder.lblFecha.setText(fecha);

        // Ocultar línea conectora en el último ítem
        if (position == listaFases.size() - 1) {
            holder.lineConnector.setVisibility(View.GONE);
        } else {
            holder.lineConnector.setVisibility(View.VISIBLE);
        }

        // Estilos e íconos dinámicos según el estado
        if ("Completada".equalsIgnoreCase(f.getEstado())) {
            holder.imgIndicator.setImageResource(R.drawable.ic_check_circle);
            holder.imgIndicator.setColorFilter(ContextCompat.getColor(context, R.color.success_green));
            holder.lblEstado.setTextColor(ContextCompat.getColor(context, R.color.success_green));
            holder.lblEstado.setBackgroundColor(ContextCompat.getColor(context, R.color.success_green_container));
        } else if ("EnProgreso".equalsIgnoreCase(f.getEstado())) {
            holder.imgIndicator.setImageResource(R.drawable.ic_sync);
            holder.imgIndicator.setColorFilter(ContextCompat.getColor(context, R.color.primary));
            holder.lblEstado.setTextColor(ContextCompat.getColor(context, R.color.primary));
            holder.lblEstado.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_container));
        } else {
            holder.imgIndicator.setImageResource(R.drawable.ic_schedule);
            holder.imgIndicator.setColorFilter(ContextCompat.getColor(context, R.color.outline));
            holder.lblEstado.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
            holder.lblEstado.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_low));
        }
    }

    @Override
    public int getItemCount() {
        return listaFases.size();
    }

    public static class FaseViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIndicator;
        View lineConnector;
        TextView lblNombre, lblEstado, lblFecha;

        public FaseViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIndicator = itemView.findViewById(R.id.imgIndicatorFase);
            lineConnector = itemView.findViewById(R.id.lineConnector);
            lblNombre = itemView.findViewById(R.id.lblNombreFase);
            lblEstado = itemView.findViewById(R.id.lblEstadoFase);
            lblFecha = itemView.findViewById(R.id.lblFechaFase);
        }
    }
}
