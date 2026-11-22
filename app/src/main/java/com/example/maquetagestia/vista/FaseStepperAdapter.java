package com.example.maquetagestia.vista;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.cFase;
import com.example.maquetagestia.model.Fase;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FaseStepperAdapter extends RecyclerView.Adapter<FaseStepperAdapter.FaseViewHolder> {

    public interface OnFaseActualizadaListener {
        void onFaseActualizada();
    }

    private final Context context;
    private final List<Fase> listaFases;
    private final OnFaseActualizadaListener listener;

    public FaseStepperAdapter(Context context, List<Fase> listaFases) {
        this(context, listaFases, null);
    }

    public FaseStepperAdapter(Context context, List<Fase> listaFases, OnFaseActualizadaListener listener) {
        this.context = context;
        this.listaFases = listaFases;
        this.listener = listener;
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

        holder.btnEditarFase.setOnClickListener(v -> mostrarDialogoEditarFase(holder, f));
    }

    private void mostrarDialogoEditarFase(@NonNull FaseViewHolder holder, @NonNull Fase f) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Editar Fase");

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_nueva_fase, null, false);
        EditText txtNombreFase = dialogView.findViewById(R.id.txtNombreFaseDialog);
        EditText txtFechaEstimada = dialogView.findViewById(R.id.txtFechaEstimadaFaseDialog);
        TextView lblEstadoFase = dialogView.findViewById(R.id.lblEstadoFaseDialog);
        Spinner spEstadoFase = dialogView.findViewById(R.id.spEstadoFaseDialog);

        lblEstadoFase.setVisibility(View.VISIBLE);
        spEstadoFase.setVisibility(View.VISIBLE);

        String[] estadosFase = {"Pendiente", "EnProgreso", "Completada"};
        ArrayAdapter<String> adapterEstado = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_dropdown_item, estadosFase);
        spEstadoFase.setAdapter(adapterEstado);
        for (int i = 0; i < estadosFase.length; i++) {
            if (estadosFase[i].equalsIgnoreCase(f.getEstado())) {
                spEstadoFase.setSelection(i);
                break;
            }
        }

        txtNombreFase.setText(f.getNombre() != null ? f.getNombre() : "");
        txtFechaEstimada.setText(f.getFechaEstimada() != null ? f.getFechaEstimada() : "");

        txtFechaEstimada.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            String actual = txtFechaEstimada.getText() != null ? txtFechaEstimada.getText().toString().trim() : "";
            if (actual.length() >= 10 && actual.charAt(4) == '-' && actual.charAt(7) == '-') {
                try {
                    int y = Integer.parseInt(actual.substring(0, 4));
                    int m = Integer.parseInt(actual.substring(5, 7)) - 1;
                    int d = Integer.parseInt(actual.substring(8, 10));
                    cal.set(y, m, d);
                } catch (NumberFormatException ignored) {
                }
            }
            DatePickerDialog dp = new DatePickerDialog(
                    context,
                    (picker, year, month, dayOfMonth) -> txtFechaEstimada.setText(
                            String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            dp.show();
        });

        builder.setView(dialogView);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoNombre = txtNombreFase.getText() != null ? txtNombreFase.getText().toString().trim() : "";
            String nuevaFecha = txtFechaEstimada.getText() != null ? txtFechaEstimada.getText().toString().trim() : "";
            String nuevoEstado = spEstadoFase.getSelectedItem() != null
                    ? spEstadoFase.getSelectedItem().toString()
                    : "Pendiente";

            if (TextUtils.isEmpty(nuevoNombre)) {
                Toast.makeText(context, "El nombre de la fase es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            Fase faseActualizada = new Fase(
                    f.getId(),
                    f.getProyectoId(),
                    nuevoNombre,
                    f.getOrden(),
                    nuevaFecha.isEmpty() ? null : nuevaFecha,
                    nuevoEstado
            );

            cFase faseControlador = new cFase();
            new Thread(() -> {
                boolean exito = faseControlador.actualizarFase(faseActualizada);
                ejecutarEnUi(() -> {
                    if (exito) {
                        f.setNombre(faseActualizada.getNombre());
                        f.setFechaEstimada(faseActualizada.getFechaEstimada());
                        f.setEstado(faseActualizada.getEstado());
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            notifyItemChanged(pos);
                        }
                        Toast.makeText(context, "¡Fase actualizada exitosamente!", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onFaseActualizada();
                        }
                    } else {
                        Toast.makeText(context, "Error al actualizar la fase en SQL Server", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        builder.setNeutralButton("Eliminar", (dialog, which) -> confirmarEliminarFase(holder, f));
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void confirmarEliminarFase(@NonNull FaseViewHolder holder, @NonNull Fase f) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Eliminar Fase")
                .setMessage("¿Seguro que deseas eliminar la fase \"" + f.getNombre() + "\"? Las tareas vinculadas se conservarán sin fase asignada.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    cFase faseControlador = new cFase();
                    new Thread(() -> {
                        boolean exito = faseControlador.eliminarFase(f.getId());
                        ejecutarEnUi(() -> {
                            if (exito) {
                                int pos = holder.getBindingAdapterPosition();
                                if (pos != RecyclerView.NO_POSITION && pos < listaFases.size()) {
                                    listaFases.remove(pos);
                                } else {
                                    listaFases.remove(f);
                                }
                                notifyDataSetChanged();
                                Toast.makeText(context, "Fase eliminada", Toast.LENGTH_SHORT).show();
                                if (listener != null) {
                                    listener.onFaseActualizada();
                                }
                            } else {
                                Toast.makeText(context, "Error al eliminar la fase en SQL Server", Toast.LENGTH_LONG).show();
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
        return listaFases.size();
    }

    public static class FaseViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIndicator;
        ImageButton btnEditarFase;
        View lineConnector;
        TextView lblNombre, lblEstado, lblFecha;

        public FaseViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIndicator = itemView.findViewById(R.id.imgIndicatorFase);
            btnEditarFase = itemView.findViewById(R.id.btnEditarFase);
            lineConnector = itemView.findViewById(R.id.lineConnector);
            lblNombre = itemView.findViewById(R.id.lblNombreFase);
            lblEstado = itemView.findViewById(R.id.lblEstadoFase);
            lblFecha = itemView.findViewById(R.id.lblFechaFase);
        }
    }
}
