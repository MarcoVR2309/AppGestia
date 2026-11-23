package com.example.maquetagestia.vista;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cColaboracionTarea;
import com.example.maquetagestia.controller.cEquipo;
import com.example.maquetagestia.controller.cEquipo.MiembroEquipoItem;
import com.example.maquetagestia.controller.cFase;
import com.example.maquetagestia.controller.cTarea;
import com.example.maquetagestia.model.ComentarioTarea;
import com.example.maquetagestia.model.EnlaceTarea;
import com.example.maquetagestia.model.Fase;
import com.example.maquetagestia.model.Subtarea;
import com.example.maquetagestia.model.Tarea;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.TareaViewHolder> {

    public interface OnTareaCambiadaListener {
        void onTareaCambiada();
    }

    private final Context context;
    private final List<Tarea> listaTareas;
    private final cTarea tareaControlador;
    private final cFase faseControlador;
    private final cEquipo equipoControlador;
    private final cColaboracionTarea colabControlador;
    private final OnTareaCambiadaListener listener;

    public TareaAdapter(Context context, List<Tarea> listaTareas) {
        this(context, listaTareas, null);
    }

    public TareaAdapter(Context context, List<Tarea> listaTareas, OnTareaCambiadaListener listener) {
        this.context = context;
        this.listaTareas = listaTareas;
        this.tareaControlador = new cTarea();
        this.faseControlador = new cFase();
        this.equipoControlador = new cEquipo();
        this.colabControlador = new cColaboracionTarea();
        this.listener = listener;
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

        // Mini barra de progreso de subtareas (solo si existen subtareas)
        if (t.getTotalSubtareas() > 0) {
            holder.layoutProgresoSubtareas.setVisibility(View.VISIBLE);
            int pctSub = (t.getSubtareasCompletadas() * 100) / t.getTotalSubtareas();
            holder.progressSubtareas.setProgress(pctSub);
            holder.lblSubtareasResumen.setText(t.getSubtareasCompletadas() + "/" + t.getTotalSubtareas());
        } else {
            holder.layoutProgresoSubtareas.setVisibility(View.GONE);
        }

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

        // Cambiar estado rápido (solo Líder o responsable asignado)
        holder.btnEstado.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            String idUsuarioActual = Sesion.getIdUsuario(context);
            String respActualId = t.getResponsableId() != null ? t.getResponsableId() : t.getAsignadoA();
            String nuevoEstado = "Finalizada".equalsIgnoreCase(t.getEstado()) ? "Pendiente" : "Finalizada";

            new Thread(() -> {
                boolean esResponsable = !TextUtils.isEmpty(idUsuarioActual) && idUsuarioActual.equals(respActualId);
                boolean esLider = !TextUtils.isEmpty(idUsuarioActual) && !TextUtils.isEmpty(t.getProyectoId())
                        && equipoControlador.esLiderDelProyecto(t.getProyectoId(), idUsuarioActual);
                if (!esResponsable && !esLider) {
                    ejecutarEnUi(() -> Toast.makeText(context,
                            "Solo el líder o el responsable asignado pueden cambiar el estado",
                            Toast.LENGTH_SHORT).show());
                    return;
                }

                boolean exito = tareaControlador.actualizarEstadoTarea(t.getId(), nuevoEstado);
                if (exito) {
                    ejecutarEnUi(() -> {
                        t.setEstado(nuevoEstado);
                        notificarCambioEnAdapter(adapterPos);
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

        LinearLayout layoutColaboracion = dialogView.findViewById(R.id.layoutColaboracionTarea);
        LinearLayout containerSubtareas = dialogView.findViewById(R.id.containerSubtareas);
        EditText txtNuevaSubtarea = dialogView.findViewById(R.id.txtNuevaSubtareaDialog);
        MaterialButton btnAgregarSubtarea = dialogView.findViewById(R.id.btnAgregarSubtareaDialog);
        LinearLayout containerEnlaces = dialogView.findViewById(R.id.containerEnlaces);
        EditText txtTituloEnlace = dialogView.findViewById(R.id.txtTituloEnlaceDialog);
        EditText txtUrlEnlace = dialogView.findViewById(R.id.txtUrlEnlaceDialog);
        MaterialButton btnAgregarEnlace = dialogView.findViewById(R.id.btnAgregarEnlaceDialog);
        LinearLayout containerComentarios = dialogView.findViewById(R.id.containerComentarios);
        EditText txtNuevoComentario = dialogView.findViewById(R.id.txtNuevoComentarioDialog);
        MaterialButton btnEnviarComentario = dialogView.findViewById(R.id.btnEnviarComentarioDialog);

        // Mostrar selector de estado y sección de colaboración en modo edición
        lblEstado.setVisibility(View.VISIBLE);
        spEstado.setVisibility(View.VISIBLE);
        layoutColaboracion.setVisibility(View.VISIBLE);

        final String idUsuarioActual = Sesion.getIdUsuario(context);
        final boolean[] esLiderRef = {false};
        final boolean[] esResponsableRef = {false};

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

        // Deshabilitar guardado y ocultar eliminar mientras se verifican roles y datos en SQL Server
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            if (!esLiderRef[0]) {
                Toast.makeText(context, "Solo el líder del proyecto puede eliminar tareas", Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Eliminar Tarea")
                    .setMessage("¿Seguro que deseas eliminar la tarea \"" + tarea.getTitulo() + "\"?")
                    .setPositiveButton("Eliminar", (confirmDialog, w) -> {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
                        new Thread(() -> {
                            boolean exito = tareaControlador.eliminarTarea(tarea.getId(), idUsuarioActual);
                            ejecutarEnUi(() -> {
                                if (exito) {
                                    dialog.dismiss();
                                    listaTareas.remove(tarea);
                                    Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show();
                                    notificarCambioEnAdapter(-1);
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

        final Runnable[] recargarSubtareasHolder = new Runnable[1];
        recargarSubtareasHolder[0] = () -> new Thread(() -> {
            List<Subtarea> subtareas = colabControlador.listarSubtareasPorTarea(tarea.getId());
            ejecutarEnUi(() -> {
                if (!dialog.isShowing()) return;
                actualizarContadorSubtareasEnTarjeta(tarea, subtareas);
                renderizarSubtareas(containerSubtareas, subtareas, tarea, esLiderRef[0], () -> {
                    int idx = listaTareas.indexOf(tarea);
                    if (idx >= 0) notifyItemChanged(idx);
                }, recargarSubtareasHolder[0]);
            });
        }).start();

        final Runnable[] recargarEnlacesHolder = new Runnable[1];
        recargarEnlacesHolder[0] = () -> new Thread(() -> {
            List<EnlaceTarea> enlaces = colabControlador.listarEnlacesPorTarea(tarea.getId());
            ejecutarEnUi(() -> {
                if (!dialog.isShowing()) return;
                renderizarEnlaces(containerEnlaces, enlaces, idUsuarioActual, esLiderRef[0],
                        tarea.getProyectoId(), recargarEnlacesHolder[0]);
            });
        }).start();

        final Runnable[] recargarComentariosHolder = new Runnable[1];
        recargarComentariosHolder[0] = () -> new Thread(() -> {
            List<ComentarioTarea> comentarios = colabControlador.listarComentariosPorTarea(tarea.getId());
            ejecutarEnUi(() -> {
                if (!dialog.isShowing()) return;
                renderizarComentarios(containerComentarios, comentarios);
            });
        }).start();

        btnAgregarSubtarea.setOnClickListener(v -> {
            String tituloSub = txtNuevaSubtarea.getText() != null ? txtNuevaSubtarea.getText().toString().trim() : "";
            if (TextUtils.isEmpty(tituloSub)) {
                Toast.makeText(context, "Escribe el título de la subtarea", Toast.LENGTH_SHORT).show();
                return;
            }
            btnAgregarSubtarea.setEnabled(false);
            new Thread(() -> {
                boolean ok = colabControlador.crearSubtarea(tarea.getId(), tituloSub);
                ejecutarEnUi(() -> {
                    btnAgregarSubtarea.setEnabled(true);
                    if (ok) {
                        txtNuevaSubtarea.setText("");
                        recargarSubtareasHolder[0].run();
                    } else {
                        Toast.makeText(context, "Error al crear subtarea", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        btnAgregarEnlace.setOnClickListener(v -> {
            String tituloEnl = txtTituloEnlace.getText() != null ? txtTituloEnlace.getText().toString().trim() : "";
            String urlEnl = txtUrlEnlace.getText() != null ? txtUrlEnlace.getText().toString().trim() : "";
            if (!urlEnl.startsWith("http://") && !urlEnl.startsWith("https://")) {
                Toast.makeText(context, "La URL debe comenzar con http:// o https://", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(idUsuarioActual)) return;
            btnAgregarEnlace.setEnabled(false);
            new Thread(() -> {
                boolean ok = colabControlador.crearEnlace(tarea.getId(), idUsuarioActual, tituloEnl, urlEnl);
                ejecutarEnUi(() -> {
                    btnAgregarEnlace.setEnabled(true);
                    if (ok) {
                        txtTituloEnlace.setText("");
                        txtUrlEnlace.setText("");
                        recargarEnlacesHolder[0].run();
                    } else {
                        Toast.makeText(context, "No se pudo adjuntar el enlace", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        btnEnviarComentario.setOnClickListener(v -> {
            String textoCom = txtNuevoComentario.getText() != null ? txtNuevoComentario.getText().toString().trim() : "";
            if (TextUtils.isEmpty(textoCom)) {
                Toast.makeText(context, "Escribe un comentario", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(idUsuarioActual)) return;
            btnEnviarComentario.setEnabled(false);
            new Thread(() -> {
                boolean ok = colabControlador.crearComentario(tarea.getId(), idUsuarioActual, textoCom);
                ejecutarEnUi(() -> {
                    btnEnviarComentario.setEnabled(true);
                    if (ok) {
                        txtNuevoComentario.setText("");
                        recargarComentariosHolder[0].run();
                    } else {
                        Toast.makeText(context, "No se pudo enviar el comentario", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        new Thread(() -> {
            List<Fase> fases = faseControlador.listarPorProyecto(tarea.getProyectoId());
            List<MiembroEquipoItem> equipo = equipoControlador.listarEquipoPorProyecto(tarea.getProyectoId());
            boolean esLider = !TextUtils.isEmpty(idUsuarioActual) && !TextUtils.isEmpty(tarea.getProyectoId())
                    && equipoControlador.esLiderDelProyecto(tarea.getProyectoId(), idUsuarioActual);
            List<Subtarea> subtareas = colabControlador.listarSubtareasPorTarea(tarea.getId());
            List<EnlaceTarea> enlaces = colabControlador.listarEnlacesPorTarea(tarea.getId());
            List<ComentarioTarea> comentarios = colabControlador.listarComentariosPorTarea(tarea.getId());

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

                boolean esResponsable = !TextUtils.isEmpty(idUsuarioActual) && idUsuarioActual.equals(respActualId);
                esLiderRef[0] = esLider;
                esResponsableRef[0] = esResponsable;

                if (!esLider) {
                    txtTitulo.setEnabled(false);
                    txtInstrucciones.setEnabled(false);
                    txtFechaLimite.setEnabled(false);
                    txtFechaLimite.setClickable(false);
                    spPrioridad.setEnabled(false);
                    spFase.setEnabled(false);
                    spAsignado.setEnabled(false);
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);
                    spEstado.setEnabled(esResponsable);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(esResponsable ? View.VISIBLE : View.GONE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(esResponsable);
                    if (!esResponsable) {
                        txtNuevaSubtarea.setEnabled(false);
                        btnAgregarSubtarea.setEnabled(false);
                    }
                } else {
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }

                actualizarContadorSubtareasEnTarjeta(tarea, subtareas);
                renderizarSubtareas(containerSubtareas, subtareas, tarea, esLider, () -> {
                    int idx = listaTareas.indexOf(tarea);
                    if (idx >= 0) notifyItemChanged(idx);
                }, recargarSubtareasHolder[0]);
                renderizarEnlaces(containerEnlaces, enlaces, idUsuarioActual, esLider,
                        tarea.getProyectoId(), recargarEnlacesHolder[0]);
                renderizarComentarios(containerComentarios, comentarios);
            });
        }).start();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nuevoEstado = spEstado.getSelectedItem().toString();

            // Colaborador responsable solo puede cambiar el estado de su tarea asignada
            if (!esLiderRef[0]) {
                if (!esResponsableRef[0]) {
                    Toast.makeText(context, "No tienes permiso para modificar esta tarea", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                new Thread(() -> {
                    boolean exito = tareaControlador.actualizarEstadoTarea(tarea.getId(), nuevoEstado);
                    ejecutarEnUi(() -> {
                        if (exito) {
                            tarea.setEstado(nuevoEstado);
                            dialog.dismiss();
                            Toast.makeText(context, "Estado actualizado", Toast.LENGTH_SHORT).show();
                            notificarCambioEnAdapter(-1);
                        } else {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            Toast.makeText(context, "Error al actualizar estado", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
                return;
            }

            String nuevoTitulo = txtTitulo.getText() != null ? txtTitulo.getText().toString().trim() : "";
            String nuevasInstr = txtInstrucciones.getText() != null ? txtInstrucciones.getText().toString().trim() : "";
            String nuevaFecha = txtFechaLimite.getText() != null ? txtFechaLimite.getText().toString().trim() : "";
            String nuevaPrioridad = spPrioridad.getSelectedItem().toString();

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
                        notificarCambioEnAdapter(-1);
                    } else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(context, "Error al actualizar la tarea en SQL Server", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });
    }

    private void actualizarContadorSubtareasEnTarjeta(Tarea tarea, List<Subtarea> subtareas) {
        int total = subtareas != null ? subtareas.size() : 0;
        int completadas = 0;
        if (subtareas != null) {
            for (Subtarea s : subtareas) {
                if (s.isCompletada()) completadas++;
            }
        }
        tarea.setTotalSubtareas(total);
        tarea.setSubtareasCompletadas(completadas);
        int idx = listaTareas.indexOf(tarea);
        if (idx >= 0) {
            notifyItemChanged(idx);
        }
    }

    private void renderizarSubtareas(LinearLayout container, List<Subtarea> subtareas, Tarea tarea,
                                    boolean puedeEliminar, Runnable onCountChanged, Runnable recargar) {
        container.removeAllViews();
        if (subtareas == null || subtareas.isEmpty()) {
            TextView tvVacio = new TextView(context);
            tvVacio.setText("Sin subtareas aún");
            tvVacio.setTextSize(12);
            tvVacio.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
            tvVacio.setPadding(0, dpToPx(2), 0, dpToPx(6));
            container.addView(tvVacio);
            return;
        }

        for (Subtarea s : subtareas) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dpToPx(4);
            row.setLayoutParams(rowLp);

            CheckBox cb = new CheckBox(context);
            LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cb.setLayoutParams(cbLp);
            cb.setText(s.getTitulo());
            cb.setTextSize(13);
            cb.setTextColor(ContextCompat.getColor(context,
                    s.isCompletada() ? R.color.on_surface_variant : R.color.on_surface));
            cb.setChecked(s.isCompletada());
            if (s.isCompletada()) {
                cb.setPaintFlags(cb.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            final boolean[] ignorarCambio = {false};
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (ignorarCambio[0]) return;
                cb.setEnabled(false);
                new Thread(() -> {
                    boolean ok = colabControlador.actualizarEstadoSubtarea(s.getId(), isChecked);
                    ejecutarEnUi(() -> {
                        cb.setEnabled(true);
                        if (ok) {
                            s.setCompletada(isChecked);
                            cb.setTextColor(ContextCompat.getColor(context,
                                    isChecked ? R.color.on_surface_variant : R.color.on_surface));
                            if (isChecked) {
                                cb.setPaintFlags(cb.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                            } else {
                                cb.setPaintFlags(cb.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                            }
                            int comp = 0;
                            for (Subtarea item : subtareas) {
                                if (item.isCompletada()) comp++;
                            }
                            tarea.setTotalSubtareas(subtareas.size());
                            tarea.setSubtareasCompletadas(comp);
                            if (onCountChanged != null) onCountChanged.run();
                        } else {
                            ignorarCambio[0] = true;
                            cb.setChecked(!isChecked);
                            ignorarCambio[0] = false;
                            Toast.makeText(context, "Error al actualizar subtarea", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            });
            row.addView(cb);

            if (puedeEliminar && recargar != null) {
                TextView btnDel = new TextView(context);
                btnDel.setText("✕");
                btnDel.setTextSize(14);
                btnDel.setTextColor(ContextCompat.getColor(context, R.color.error));
                btnDel.setPadding(dpToPx(8), dpToPx(4), dpToPx(4), dpToPx(4));
                btnDel.setOnClickListener(v -> new Thread(() -> {
                    boolean ok = colabControlador.eliminarSubtarea(s.getId());
                    ejecutarEnUi(() -> {
                        if (ok) {
                            recargar.run();
                        } else {
                            Toast.makeText(context, "Error al eliminar subtarea", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start());
                row.addView(btnDel);
            }
            container.addView(row);
        }
    }

    private void renderizarEnlaces(LinearLayout container, List<EnlaceTarea> enlaces,
                                 String idUsuarioActual, boolean esLider,
                                 String proyectoId, Runnable recargar) {
        container.removeAllViews();
        if (enlaces == null || enlaces.isEmpty()) {
            TextView tvVacio = new TextView(context);
            tvVacio.setText("Sin enlaces adjuntos");
            tvVacio.setTextSize(12);
            tvVacio.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
            tvVacio.setPadding(0, dpToPx(2), 0, dpToPx(6));
            container.addView(tvVacio);
            return;
        }

        for (EnlaceTarea e : enlaces) {
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_low));
            card.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dpToPx(6);
            card.setLayoutParams(cardLp);

            LinearLayout info = new LinearLayout(context);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvTitulo = new TextView(context);
            tvTitulo.setText(TextUtils.isEmpty(e.getTitulo()) ? "Enlace" : e.getTitulo());
            tvTitulo.setTextSize(13);
            tvTitulo.setTextColor(ContextCompat.getColor(context, R.color.on_surface));
            info.addView(tvTitulo);

            TextView tvUrl = new TextView(context);
            tvUrl.setText(e.getUrl());
            tvUrl.setTextSize(12);
            tvUrl.setTextColor(ContextCompat.getColor(context, R.color.primary));
            info.addView(tvUrl);

            if (!TextUtils.isEmpty(e.getAutorNombre())) {
                TextView tvAutor = new TextView(context);
                tvAutor.setText(e.getAutorNombre() + (TextUtils.isEmpty(e.getFechaCreacion()) ? "" : " · " + e.getFechaCreacion()));
                tvAutor.setTextSize(10);
                tvAutor.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
                info.addView(tvAutor);
            }

            info.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(e.getUrl()));
                    context.startActivity(intent);
                } catch (Exception ex) {
                    Toast.makeText(context, "No se pudo abrir el enlace en el navegador", Toast.LENGTH_SHORT).show();
                }
            });
            card.addView(info);

            boolean esAutor = !TextUtils.isEmpty(idUsuarioActual) && idUsuarioActual.equals(e.getUsuarioId());
            if ((esLider || esAutor) && recargar != null) {
                TextView btnDel = new TextView(context);
                btnDel.setText("✕");
                btnDel.setTextSize(14);
                btnDel.setTextColor(ContextCompat.getColor(context, R.color.error));
                btnDel.setPadding(dpToPx(8), dpToPx(4), dpToPx(4), dpToPx(4));
                btnDel.setOnClickListener(v -> new Thread(() -> {
                    boolean ok = colabControlador.eliminarEnlace(e.getId(), idUsuarioActual, proyectoId);
                    ejecutarEnUi(() -> {
                        if (ok) {
                            recargar.run();
                        } else {
                            Toast.makeText(context, "No autorizado para eliminar este enlace", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start());
                card.addView(btnDel);
            }
            container.addView(card);
        }
    }

    private void renderizarComentarios(LinearLayout container, List<ComentarioTarea> comentarios) {
        container.removeAllViews();
        if (comentarios == null || comentarios.isEmpty()) {
            TextView tvVacio = new TextView(context);
            tvVacio.setText("Sin comentarios aún");
            tvVacio.setTextSize(12);
            tvVacio.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
            tvVacio.setPadding(0, dpToPx(2), 0, dpToPx(6));
            container.addView(tvVacio);
            return;
        }

        for (ComentarioTarea c : comentarios) {
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_low));
            card.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dpToPx(6);
            card.setLayoutParams(lp);

            TextView tvMeta = new TextView(context);
            String autor = TextUtils.isEmpty(c.getAutorNombre()) ? "Usuario" : c.getAutorNombre();
            String fecha = TextUtils.isEmpty(c.getFechaCreacion()) ? "" : " · " + c.getFechaCreacion();
            tvMeta.setText(autor + fecha);
            tvMeta.setTextSize(11);
            tvMeta.setTextColor(ContextCompat.getColor(context, R.color.primary));
            card.addView(tvMeta);

            TextView tvBody = new TextView(context);
            tvBody.setText(c.getContenido());
            tvBody.setTextSize(13);
            tvBody.setTextColor(ContextCompat.getColor(context, R.color.on_surface));
            tvBody.setPadding(0, dpToPx(2), 0, 0);
            card.addView(tvBody);

            container.addView(card);
        }
    }

    private void notificarCambioEnAdapter(int adapterPos) {
        if (listener != null) {
            listener.onTareaCambiada();
        } else if (context instanceof frm_tareas) {
            ((frm_tareas) context).cargarTareas();
        } else if (adapterPos >= 0 && adapterPos < listaTareas.size()) {
            notifyItemChanged(adapterPos);
        } else {
            notifyDataSetChanged();
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
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
        TextView lblTitulo, lblInstrucciones, lblPrioridad, lblFechaLimite, lblResponsable, lblSubtareasResumen;
        LinearLayout layoutProgresoSubtareas;
        ProgressBar progressSubtareas;
        ImageButton btnEstado;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            lblTitulo = itemView.findViewById(R.id.lblTituloTarea);
            lblInstrucciones = itemView.findViewById(R.id.lblInstruccionesTarea);
            lblPrioridad = itemView.findViewById(R.id.lblPrioridadTarea);
            lblFechaLimite = itemView.findViewById(R.id.lblFechaLimiteTarea);
            lblResponsable = itemView.findViewById(R.id.lblResponsableTarea);
            btnEstado = itemView.findViewById(R.id.btnCambiarEstado);
            layoutProgresoSubtareas = itemView.findViewById(R.id.layoutProgresoSubtareas);
            progressSubtareas = itemView.findViewById(R.id.progressSubtareas);
            lblSubtareasResumen = itemView.findViewById(R.id.lblSubtareasResumen);
        }
    }
}
