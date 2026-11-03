package com.example.maquetagestia.vista;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.cEquipo.MiembroEquipoItem;

import java.util.List;

public class EquipoAdapter extends RecyclerView.Adapter<EquipoAdapter.EquipoViewHolder> {

    private final Context context;
    private final List<MiembroEquipoItem> listaMiembros;

    public EquipoAdapter(Context context, List<MiembroEquipoItem> listaMiembros) {
        this.context = context;
        this.listaMiembros = listaMiembros;
    }

    @NonNull
    @Override
    public EquipoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_equipo, parent, false);
        return new EquipoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipoViewHolder holder, int position) {
        MiembroEquipoItem item = listaMiembros.get(position);

        holder.lblNombre.setText(item.usuario.getNombreCompleto());
        holder.lblCorreo.setText(item.usuario.getCorreo());

        String rol = item.rol != null ? item.rol : "Colaborador";
        holder.lblRol.setText(rol);

        if ("Lider".equalsIgnoreCase(rol) || "Líder".equalsIgnoreCase(rol)) {
            holder.lblRol.setTextColor(ContextCompat.getColor(context, R.color.on_primary));
            holder.lblRol.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_container));
        } else if ("Colaborador".equalsIgnoreCase(rol)) {
            holder.lblRol.setTextColor(ContextCompat.getColor(context, R.color.secondary));
            holder.lblRol.setBackgroundColor(ContextCompat.getColor(context, R.color.secondary_container));
        } else {
            holder.lblRol.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
            holder.lblRol.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_low));
        }
    }

    @Override
    public int getItemCount() {
        return listaMiembros.size();
    }

    public static class EquipoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView lblNombre, lblCorreo, lblRol;

        public EquipoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatarMiembro);
            lblNombre = itemView.findViewById(R.id.lblNombreMiembro);
            lblCorreo = itemView.findViewById(R.id.lblCorreoMiembro);
            lblRol = itemView.findViewById(R.id.lblRolMiembro);
        }
    }
}
