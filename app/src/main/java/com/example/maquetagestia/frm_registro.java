package com.example.maquetagestia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class frm_registro extends AppCompatActivity {
    private EditText txtNombre, txtEmail, txtContrasena, txtConfirmarContrasena;
    private Spinner spTipoUsuario;
    private CheckBox cbTerminos;
    private MaterialButton btnCrearCuenta;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_registro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        txtNombre = findViewById(R.id.txtNombre);
        txtEmail = findViewById(R.id.txtEmail);
        txtContrasena = findViewById(R.id.txtContrasena);
        txtConfirmarContrasena = findViewById(R.id.txtConfirmarContrasena);
        spTipoUsuario = findViewById(R.id.spTipoUsuario);
        cbTerminos = findViewById(R.id.cbTerminos);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        String[] opcionesUsuario = {"Selecciona un tipo", "Estudiante", "Docente", "Profesional/Empresarial"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opcionesUsuario);
        spTipoUsuario.setAdapter(adapter);
    }
    public void registrarUsuario(View view) {
        String nombre = txtNombre.getText().toString();
        String email = txtEmail.getText().toString();
        String pass = txtContrasena.getText().toString();
        String confirmPass = txtConfirmarContrasena.getText().toString();

        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbTerminos.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void volverAlLogin(View view) {
        finish();
    }
}