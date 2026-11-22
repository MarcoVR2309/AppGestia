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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.cEquipo;
import com.example.maquetagestia.controller.cEquipo.MiembroEquipoItem;
import com.example.maquetagestia.controller.cFase;
import com.example.maquetagestia.controller.cTarea;
import com.example.maquetagestia.model.Fase;
import com.example.maquetagestia.model.Tarea;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.TareaViewHolder> {

    private final Context context;
    private final List<Tarea> listaTareas;
    private final cTarea tareaControlador;
    private final cFase faseControlador;
    private final cEquipo equipoControlador;

    public TareaAdapter(Context context, List<Tarea> listaTareas) {
        this.context = context;
        this.listaTareas = listaTareas;
        this.tareaControlador = new cTarea();
        this.faseControlador = new cFase();
        this.equipoControlador = new cEquipo();
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

        String fecha = (t.getFechaLimite() != null && !t.getFechaLimite().trim().isEmpty())
                ? "Límite: " + t.getFechaLimite().trim()
                : "Sin fecha";
        holder.lblFechaLimite.setText(fecha);

        String responsable = (t.getResponsableNombre() != null && !t.getResponsableNombre().trim().isEmpty())
                ? "Responsable: " + t.getResponsableNombre().trim()
                : "Responsable: Sin asignar";
        holder.lblResponsable.setText(responsable);

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
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            String nuevoEstado = "Finalizada".equalsIgnoreCase(t.getEstado()) ? "Pendiente" : "Finalizada";
            new Thread(() -> {
                boolean exito = tareaControlador.actualizarEstadoTarea(t.getId(), nuevoEstado);
                if (exito) {
                    ejecutarEnUi(() -> {
                        t.setEstado(nuevoEstado);
                        if (context instanceof frm_tareas) {
                            ((frm_tareas) context).cargarTareas();
                        } else {
                            notifyItemChanged(adapterPos);
                        }
                    });
                }
            }).start();
        });

        // Abrir modal Editar / Asignar Tarea al pulsar la tarjeta
        holder.itemView.setOnClickListener(v -> abrirDialogoEditarTarea(t));
    }

    private void abrirDialogoEditarTarea(@NonNull Tarea tarea) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Editar / Asignar Tarea");

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_nueva_tarea, null, false);
        EditText txtTitulo = dialogView.findViewById(R.id.txtTituloTareaDialog);
        EditText txtInstrucciones = dialogView.findViewById(R.id.txtInstruccionesDialog);
        EditText txtFechaLimite = dialogView.findViewById(R.id.txtFechaLimiteDialog);
        Spinner spPrioridad = dialogView.findViewById(R.id.spPrioridadDialog);
        TextView lblEstado = dialogView.findViewById(R.id.lblEstadoTareaDialog);
        Spinner spEstado = dialogView.findViewById(R.id.spEstadoDialog);
        Spinner spFase = dialogView.findViewById(R.id.spFaseDialog);
        Spinner spAsignado = dialogView.findViewById(R.id.spAsignadoDialog);

        // Mostrar selector de estado en modo edición
        lblEstado.setVisibility(View.VISIBLE);
        spEstado.setVisibility(View.VISIBLE);

        final String fechaOriginal = tarea.getFechaLimite() != null ? tarea.getFechaLimite().trim() : "";

        txtTitulo.setText(tarea.getTitulo() != null ? tarea.getTitulo() : "");
        txtInstrucciones.setText(tarea.getInstrucciones() != null ? tarea.getInstrucciones() : "");
        txtFechaLimite.setText(fechaOriginal);

        txtFechaLimite.setOnClickListener(v -> {
            Calendar minCal = obtenerInicioDeHoy();
            Calendar cal = (Calendar) minCal.clone();
            String actual = txtFechaLimite.getText() != null ? txtFechaLimite.getText().toString().trim() : "";
            if (actual.length() >= 10 && actual.charAt(4) == '-' && actual.charAt(7) == '-') {
                try {
                    int y = Integer.parseInt(actual.substring(0, 4));
                    int m = Integer.parseInt(actual.substring(5, 7)) - 1;
                    int d = Integer.parseInt(actual.substring(8, 10));
                    cal.set(y, m, d, 0, 0, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    if (cal.before(minCal)) {
                        cal.setTimeInMillis(minCal.getTimeInMillis());
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            DatePickerDialog datePicker = new DatePickerDialog(
                    context,
                    (picker, year, month, dayOfMonth) -> {
                        String fechaFormateada = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        txtFechaLimite.setText(fechaFormateada);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.getDatePicker().setMinDate(minCal.getTimeInMillis());
            datePicker.show();
        });

        String[] prioridades = {"Baja", "Media", "Alta", "Urgente"};
        ArrayAdapter<String> adapterPrio = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, prioridades);
        spPrioridad.setAdapter(adapterPrio);
        for (int i = 0; i < prioridades.length; i++) {
            if (prioridades[i].equalsIgnoreCase(tarea.getPrioridad())) {
                spPrioridad.setSelection(i);
                break;
            }
        }

        String[] estados = {"Pendiente", "EnProgreso", "Finalizada"};
        ArrayAdapter<String> adapterEstado = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, estados);
        spEstado.setAdapter(adapterEstado);
        for (int i = 0; i < estados.length; i++) {
            if (estados[i].equalsIgnoreCase(tarea.getEstado())) {
                spEstado.setSelection(i);
                break;
            }
        }

        final List<Fase> fasesProyecto = new ArrayList<>();
        final List<MiembroEquipoItem> equipoProyecto = new ArrayList<>();

        List<String> cargando = new ArrayList<>();
        cargando.add("Cargando...");
        spFase.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, cargando));
        spAsignado.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, cargando));

        builder.setView(dialogView);
        builder.setPositiveButton("Guardar cambios", null);
        builder.setNeutralButton("Eliminar", null);
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Deshabilitar guardado mientras se consultan fases y equipo del proyecto en SQL Server
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Eliminar Tarea")
                    .setMessage("¿Seguro que deseas eliminar la tarea \"" + tarea.getTitulo() + "\"?")
                    .setPositiveButton("Eliminar", (confirmDialog, w) -> {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
                        new Thread(() -> {
                            boolean exito = tareaControlador.eliminarTarea(tarea.getId());
                            ejecutarEnUi(() -> {
                                if (exito) {
                                    dialog.dismiss();
                                    listaTareas.remove(tarea);
                                    Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show();
                                    if (context instanceof frm_tareas) {
                                        ((frm_tareas) context).cargarTareas();
                                    } else {
                                        notifyDataSetChanged();
                                    }
                                } else {
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
                                    Toast.makeText(context, "Error al eliminar la tarea en SQL Server", Toast.LENGTH_LONG).show();
                                }
                            });
                        }).start();
                    })
                    .setNegativeButton("Cancelar", (confirmDialog, w) -> confirmDialog.dismiss())
                    .show();
        });

        new Thread(() -> {
            List<Fase> fases = faseControlador.listarPorProyecto(tarea.getProyectoId());
            List<MiembroEquipoItem> equipo = equipoControlador.listarEquipoPorProyecto(tarea.getProyectoId());

            ejecutarEnUi(() -> {
                if (!dialog.isShowing()) return;

                fasesProyecto.clear();
                if (fases != null) fasesProyecto.addAll(fases);

                List<String> nombresFases = new ArrayList<>();
                int faseSelIndex = 0;
                if (fasesProyecto.isEmpty()) {
                    nombresFases.add("Sin fase");
                } else {
                    for (int i = 0; i < fasesProyecto.size(); i++) {
                        Fase f = fasesProyecto.get(i);
                        nombresFases.add(f.getNombre());
                        if (tarea.getFaseId() != null && tarea.getFaseId().equals(f.getId())) {
                            faseSelIndex = i;
                        }
                    }
                }
                spFase.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, nombresFases));
                spFase.setSelection(faseSelIndex);

                equipoProyecto.clear();
                if (equipo != null) equipoProyecto.addAll(equipo);

                List<String> nombresMiembros = new ArrayList<>();
                nombresMiembros.add("Sin asignar");
                int respSelIndex = 0;
                String respActualId = tarea.getResponsableId() != null ? tarea.getResponsableId() : tarea.getAsignadoA();
                for (int i = 0; i < equipoProyecto.size(); i++) {
                    MiembroEquipoItem item = equipoProyecto.get(i);
                    nombresMiembros.add(item.usuario.getNombreCompleto());
                    if (respActualId != null && respActualId.equals(item.usuario.getId())) {
                        respSelIndex = i + 1;
                    }
                }
                spAsignado.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, nombresMiembros));
                spAsignado.setSelection(respSelIndex);

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            });
        }).start();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nuevoTitulo = txtTitulo.getText() != null ? txtTitulo.getText().toString().trim() : "";
            String nuevasInstr = txtInstrucciones.getText() != null ? txtInstrucciones.getText().toString().trim() : "";
            String nuevaFecha = txtFechaLimite.getText() != null ? txtFechaLimite.getText().toString().trim() : "";
            String nuevaPrioridad = spPrioridad.getSelectedItem().toString();
            String nuevoEstado = spEstado.getSelectedItem().toString();

            if (TextUtils.isEmpty(nuevoTitulo)) {
                Toast.makeText(context, "El título de la tarea es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!nuevaFecha.equals(fechaOriginal) && !nuevaFecha.isEmpty() && esFechaAnteriorAHoy(nuevaFecha)) {
                Toast.makeText(context, "La fecha límite debe ser hoy o una fecha posterior", Toast.LENGTH_SHORT).show();
                return;
            }

            int faseIndex = spFase.getSelectedItemPosition();
            String nuevaFaseId = (!fasesProyecto.isEmpty() && faseIndex >= 0 && faseIndex < fasesProyecto.size())
                    ? fasesProyecto.get(faseIndex).getId()
                    : null;

            int asignadoIndex = spAsignado.getSelectedItemPosition();
            String nuevoResponsableId = (asignadoIndex > 0 && (asignadoIndex - 1) < equipoProyecto.size())
                    ? equipoProyecto.get(asignadoIndex - 1).usuario.getId()
                    : null;
            String nuevoResponsableNombre = (asignadoIndex > 0 && (asignadoIndex - 1) < equipoProyecto.size())
                    ? equipoProyecto.get(asignadoIndex - 1).usuario.getNombreCompleto()
                    : null;

            Tarea tareaEditada = new Tarea();
            tareaEditada.setId(tarea.getId());
            tareaEditada.setProyectoId(tarea.getProyectoId());
            tareaEditada.setFaseId(nuevaFaseId);
            tareaEditada.setTitulo(nuevoTitulo);
            tareaEditada.setInstrucciones(nuevasInstr);
            tareaEditada.setPrioridad(nuevaPrioridad);
            tareaEditada.setEstado(nuevoEstado);
            tareaEditada.setFechaLimite(nuevaFecha.isEmpty() ? null : nuevaFecha);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            new Thread(() -> {
                boolean exito = tareaControlador.actualizarTareaYResponsable(tareaEditada, nuevoResponsableId);
                ejecutarEnUi(() -> {
                    if (exito) {
                        tarea.setFaseId(nuevaFaseId);
                        tarea.setTitulo(nuevoTitulo);
                        tarea.setInstrucciones(nuevasInstr);
                        tarea.setPrioridad(nuevaPrioridad);
                        tarea.setEstado(nuevoEstado);
                        tarea.setFechaLimite(nuevaFecha.isEmpty() ? null : nuevaFecha);
                        tarea.setResponsableId(nuevoResponsableId);
                        tarea.setAsignadoA(nuevoResponsableId);
                        tarea.setResponsableNombre(nuevoResponsableNombre);

                        dialog.dismiss();
                        Toast.makeText(context, "¡Tarea actualizada exitosamente!", Toast.LENGTH_SHORT).show();
                        if (context instanceof frm_tareas) {
                            ((frm_tareas) context).cargarTareas();
                        } else {
                            notifyDataSetChanged();
                        }
                    } else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(context, "Error al actualizar la tarea en SQL Server", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });
    }

    private Calendar obtenerInicioDeHoy() {
        Calendar minCal = Calendar.getInstance();
        minCal.set(Calendar.HOUR_OF_DAY, 0);
        minCal.set(Calendar.MINUTE, 0);
        minCal.set(Calendar.SECOND, 0);
        minCal.set(Calendar.MILLISECOND, 0);
        return minCal;
    }

    private boolean esFechaAnteriorAHoy(String fechaStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            Date fecha = sdf.parse(fechaStr.trim());
            Date hoy = obtenerInicioDeHoy().getTime();
            return fecha == null || fecha.before(hoy);
        } catch (Exception e) {
            return true;
        }
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
        return listaTareas.size();
    }

    public static class TareaViewHolder extends RecyclerView.ViewHolder {
        TextView lblTitulo, lblInstrucciones, lblPrioridad, lblFechaLimite, lblResponsable;
        ImageButton btnEstado;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            lblTitulo = itemView.findViewById(R.id.lblTituloTarea);
            lblInstrucciones = itemView.findViewById(R.id.lblInstruccionesTarea);
            lblPrioridad = itemView.findViewById(R.id.lblPrioridadTarea);
            lblFechaLimite = itemView.findViewById(R.id.lblFechaLimiteTarea);
            lblResponsable = itemView.findViewById(R.id.lblResponsableTarea);
            btnEstado = itemView.findViewById(R.id.btnCambiarEstado);
        }
    }
}
