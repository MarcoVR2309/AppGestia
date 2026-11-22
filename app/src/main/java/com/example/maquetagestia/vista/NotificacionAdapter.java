package com.example.maquetagestia.vista;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.cEquipo;
import com.example.maquetagestia.controller.cNotificacion;
import com.example.maquetagestia.model.Notificacion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder> {

    private final Context context;
    private final List<Notificacion> listaNotificaciones;
    private final cNotificacion notificacionControlador;
    private final cEquipo equipoControlador;

    public NotificacionAdapter(Context context, List<Notificacion> listaNotificaciones) {
        this.context = context;
        this.listaNotificaciones = listaNotificaciones;
        this.notificacionControlador = new cNotificacion();
        this.equipoControlador = new cEquipo();
    }

    @NonNull
    @Override
    public NotificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notificacion, parent, false);
        return new NotificacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificacionViewHolder holder, int position) {
        Notificacion n = listaNotificaciones.get(position);

        holder.lblTitulo.setText(n.getTitulo() != null ? n.getTitulo() : "");
        holder.lblMensaje.setText(n.getMensaje() != null ? n.getMensaje() : "");
        holder.lblFecha.setText(n.getFechaGeneracion() != null ? n.getFechaGeneracion() : "");

        // Distinción visual entre notificación leída y no leída
        if (!n.isLeido()) {
            holder.lblTitulo.setTypeface(null, Typeface.BOLD);
            holder.cardNotificacion.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_high));
            holder.indicadorNoLeida.setVisibility(View.VISIBLE);
        } else {
            holder.lblTitulo.setTypeface(null, Typeface.NORMAL);
            holder.cardNotificacion.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_lowest));
            holder.indicadorNoLeida.setVisibility(View.GONE);
        }

        // Ícono y color según el tipo de notificación
        aplicarIconoPorTipo(holder.imgIcono, n.getTipo());

        // Mostrar botones de aceptación/rechazo solo para invitaciones pendientes (no leídas)
        boolean esInvitacionPendiente = "Invitacion".equals(n.getTipo()) && !n.isLeido();
        if (esInvitacionPendiente) {
            holder.btnAceptarInvitacion.setVisibility(View.VISIBLE);
            holder.btnRechazarInvitacion.setVisibility(View.VISIBLE);
        } else {
            holder.btnAceptarInvitacion.setVisibility(View.GONE);
            holder.btnRechazarInvitacion.setVisibility(View.GONE);
        }

        holder.btnAceptarInvitacion.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION || n.isLeido()) {
                return;
            }

            new Thread(() -> {
                boolean confirmado = equipoControlador.confirmarInvitacion(
                        n.getReferenciaId(), n.getUsuarioId(), n.getMensaje());
                boolean marcada = confirmado && notificacionControlador.marcarComoLeida(n.getId());

                ejecutarEnUi(() -> {
                    if (confirmado) {
                        if (marcada) {
                            n.setLeido(true);
                        }
                        notifyItemChanged(adapterPos);
                        Toast.makeText(context, "Invitación aceptada. ¡Ya eres parte del proyecto!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Error al aceptar la invitación", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        holder.btnRechazarInvitacion.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION || n.isLeido()) {
                return;
            }

            new Thread(() -> {
                boolean marcada = notificacionControlador.marcarComoLeida(n.getId());
                ejecutarEnUi(() -> {
                    if (marcada) {
                        n.setLeido(true);
                        notifyItemChanged(adapterPos);
                        Toast.makeText(context, "Invitación rechazada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Error al rechazar la invitación", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        // Marcar como leída al hacer clic en la tarjeta (salvo invitaciones pendientes que requieren Aceptar/Rechazar)
        holder.cardNotificacion.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION || n.isLeido() || esInvitacionPendiente) {
                return;
            }

            new Thread(() -> {
                boolean exito = notificacionControlador.marcarComoLeida(n.getId());
                if (exito) {
                    ejecutarEnUi(() -> {
                        n.setLeido(true);
                        notifyItemChanged(adapterPos);
                    });
                }
            }).start();
        });
    }

    private void ejecutarEnUi(Runnable accion) {
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).runOnUiThread(accion);
        } else {
            new Handler(Looper.getMainLooper()).post(accion);
        }
    }

    private void aplicarIconoPorTipo(ImageView imgIcono, String tipo) {
        if ("Invitacion".equalsIgnoreCase(tipo)) {
            imgIcono.setImageResource(R.drawable.ic_group_add);
            imgIcono.setColorFilter(ContextCompat.getColor(context, R.color.primary));
        } else if ("Asignacion".equalsIgnoreCase(tipo)) {
            imgIcono.setImageResource(R.drawable.ic_assignment);
            imgIcono.setColorFilter(ContextCompat.getColor(context, R.color.primary));
        } else if ("Vencimiento".equalsIgnoreCase(tipo)) {
            imgIcono.setImageResource(R.drawable.ic_schedule);
            imgIcono.setColorFilter(ContextCompat.getColor(context, R.color.warning_amber));
        } else if ("Completada".equalsIgnoreCase(tipo)) {
            imgIcono.setImageResource(R.drawable.ic_check_circle);
            imgIcono.setColorFilter(ContextCompat.getColor(context, R.color.success_green));
        } else {
            imgIcono.setImageResource(R.drawable.ic_notifications);
            imgIcono.setColorFilter(ContextCompat.getColor(context, R.color.secondary));
        }
    }

    @Override
    public int getItemCount() {
        return listaNotificaciones.size();
    }

    public static class NotificacionViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardNotificacion;
        ImageView imgIcono;
        TextView lblTitulo, lblMensaje, lblFecha;
        View indicadorNoLeida;
        MaterialButton btnAceptarInvitacion, btnRechazarInvitacion;

        public NotificacionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotificacion = itemView.findViewById(R.id.cardNotificacion);
            imgIcono = itemView.findViewById(R.id.imgIconoNotif);
            lblTitulo = itemView.findViewById(R.id.lblTituloNotif);
            lblMensaje = itemView.findViewById(R.id.lblMensajeNotif);
            lblFecha = itemView.findViewById(R.id.lblFechaNotif);
            indicadorNoLeida = itemView.findViewById(R.id.indicadorNoLeida);
            btnAceptarInvitacion = itemView.findViewById(R.id.btnAceptarInvitacion);
            btnRechazarInvitacion = itemView.findViewById(R.id.btnRechazarInvitacion);
        }
    }
}
