package com.example.maquetagestia.vista;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.cEquipo;
import com.example.maquetagestia.controller.cEquipo.MiembroEquipoItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class frm_equipo extends AppCompatActivity {

    private String proyectoId;
    private RecyclerView rcvEquipo;
    private cEquipo equipoControlador;
    private EquipoAdapter equipoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_equipo);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        proyectoId = getIntent().getStringExtra("PROYECTO_ID");

        rcvEquipo = findViewById(R.id.rcvEquipo);
        if (rcvEquipo != null) {
            rcvEquipo.setLayoutManager(new LinearLayoutManager(this));
        }

        equipoControlador = new cEquipo();

        cargarEquipo();
    }

    public void cargarEquipo() {
        if (proyectoId == null || proyectoId.isEmpty()) return;

        new Thread(() -> {
            List<MiembroEquipoItem> lista = equipoControlador.listarEquipoPorProyecto(proyectoId);

            runOnUiThread(() -> {
                if (rcvEquipo != null) {
                    equipoAdapter = new EquipoAdapter(frm_equipo.this, lista);
                    rcvEquipo.setAdapter(equipoAdapter);
                }
            });
        }).start();
    }

    public void volverAlDetalle(View view) {
        finish();
    }

    public void asignarMiembros(View view) {
        if (proyectoId == null || proyectoId.isEmpty()) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Invitar Miembro al Equipo");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_invitar_miembro, null, false);
        EditText txtEmail = dialogView.findViewById(R.id.txtEmailInvitarDialog);
        Spinner spRol = dialogView.findViewById(R.id.spRolInvitarDialog);

        String[] roles = {"Colaborador", "Líder", "Observador"};
        ArrayAdapter<String> adapterRol = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spRol.setAdapter(adapterRol);

        builder.setView(dialogView);
        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String correo = txtEmail.getText() != null ? txtEmail.getText().toString().trim() : "";
            String rolSel = spRol.getSelectedItem().toString();

            if (TextUtils.isEmpty(correo)) {
                Toast.makeText(this, "Ingrese el correo electrónico del usuario", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                boolean exito = equipoControlador.agregarMiembroEquipo(proyectoId, correo, rolSel);
                runOnUiThread(() -> {
                    if (exito) {
                        Toast.makeText(frm_equipo.this, "¡Miembro agregado al equipo!", Toast.LENGTH_SHORT).show();
                        cargarEquipo();
                    } else {
                        Toast.makeText(frm_equipo.this, "El correo no existe o ya pertenece al proyecto", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}