package com.example.maquetagestia.vista;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cEquipo;
import com.example.maquetagestia.controller.cEquipo.MiembroEquipoItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class EquipoAdapter extends RecyclerView.Adapter<EquipoAdapter.EquipoViewHolder> {

    private final Context context;
    private final List<MiembroEquipoItem> listaMiembros;
    private final cEquipo equipoControlador;
    private final boolean esUsuarioLider;

    public EquipoAdapter(Context context, List<MiembroEquipoItem> listaMiembros) {
        this(context, listaMiembros, false);
    }

    public EquipoAdapter(Context context, List<MiembroEquipoItem> listaMiembros, boolean esUsuarioLider) {
        this.context = context;
        this.listaMiembros = listaMiembros;
        this.equipoControlador = new cEquipo();
        this.esUsuarioLider = esUsuarioLider;
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

        if (cEquipo.esRolLider(rol)) {
            holder.lblRol.setTextColor(ContextCompat.getColor(context, R.color.on_primary));
            holder.lblRol.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_container));
        } else if ("Colaborador".equalsIgnoreCase(rol)) {
            holder.lblRol.setTextColor(ContextCompat.getColor(context, R.color.secondary));
            holder.lblRol.setBackgroundColor(ContextCompat.getColor(context, R.color.secondary_container));
        } else {
            holder.lblRol.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
            holder.lblRol.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_low));
        }

        String idUsuarioSesion = Sesion.getIdUsuario(context);
        boolean esMismoUsuario = idUsuarioSesion != null && item.usuario != null
                && idUsuarioSesion.equals(item.usuario.getId());
        holder.btnOpcionesEquipo.setVisibility((esUsuarioLider && !esMismoUsuario) ? View.VISIBLE : View.GONE);

        holder.btnOpcionesEquipo.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnOpcionesEquipo);
            popup.getMenu().add(0, 1, 0, "Editar Rol");
            popup.getMenu().add(0, 2, 1, "Eliminar");
            popup.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == 1) {
                    mostrarDialogoEditarRol(item);
                    return true;
                } else if (menuItem.getItemId() == 2) {
                    mostrarDialogoEliminar(item);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void mostrarDialogoEditarRol(MiembroEquipoItem item) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Editar Rol");

        int paddingPx = Math.round(20 * context.getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(paddingPx, paddingPx / 2, paddingPx, 0);

        Spinner spRol = new Spinner(context);
        String[] roles = {"Lider", "Colaborador", "Observador"};
        ArrayAdapter<String> adapterRol = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_dropdown_item, roles);
        spRol.setAdapter(adapterRol);

        if (cEquipo.esRolLider(item.rol)) {
            spRol.setSelection(0);
        } else if ("Observador".equalsIgnoreCase(item.rol)) {
            spRol.setSelection(2);
        } else {
            spRol.setSelection(1);
        }

        container.addView(spRol);
        builder.setView(container);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoRol = spRol.getSelectedItem().toString();
            String idSolicitante = Sesion.getIdUsuario(context);
            new Thread(() -> {
                boolean exito = equipoControlador.actualizarRol(item.idRelacion, nuevoRol, idSolicitante);
                ejecutarEnUi(() -> {
                    if (exito) {
                        item.rol = nuevoRol;
                        notifyDataSetChanged();
                        Toast.makeText(context, "Rol actualizado correctamente", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "No tienes permiso o hubo un error al actualizar", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void mostrarDialogoEliminar(MiembroEquipoItem item) {
        String nombre = item.usuario != null && item.usuario.getNombreCompleto() != null
                ? item.usuario.getNombreCompleto()
                : "este miembro";
        String idSolicitante = Sesion.getIdUsuario(context);

        new MaterialAlertDialogBuilder(context)
                .setTitle("Eliminar Miembro")
                .setMessage("¿Desea eliminar a " + nombre + " del proyecto?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    new Thread(() -> {
                        boolean exito = equipoControlador.eliminarMiembro(item.idRelacion, idSolicitante);
                        ejecutarEnUi(() -> {
                            if (exito) {
                                listaMiembros.remove(item);
                                notifyDataSetChanged();
                                Toast.makeText(context, "Miembro eliminado del proyecto", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "No tienes permiso o hubo un error al eliminar", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void ejecutarEnUi(Runnable accion) {
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).runOnUiThread(accion);
        } else {
            new Handler(Looper.getMainLooper()).post(accion);
        }
    }

    @Override
    public int getItemCount() {
        return listaMiembros.size();
    }

    public static class EquipoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        ImageButton btnOpcionesEquipo;
        TextView lblNombre, lblCorreo, lblRol;

        public EquipoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatarMiembro);
            btnOpcionesEquipo = itemView.findViewById(R.id.btnOpcionesEquipo);
            lblNombre = itemView.findViewById(R.id.lblNombreMiembro);
            lblCorreo = itemView.findViewById(R.id.lblCorreoMiembro);
            lblRol = itemView.findViewById(R.id.lblRolMiembro);
        }
    }
}
