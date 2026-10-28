package com.example.maquetagestia.vista;

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

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.cUsuario;
import com.example.maquetagestia.model.Usuario;
import com.google.android.material.button.MaterialButton;

public class frm_registro extends AppCompatActivity {

    private EditText txtNombre, txtEmail, txtContrasena, txtConfirmarContrasena;
    private Spinner spTipoUsuario;
    private CheckBox cbTerminos;
    private MaterialButton btnCrearCuenta;
    private cUsuario usuarioControlador;

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

        // Enlace de vistas
        txtNombre = findViewById(R.id.txtNombre);
        txtEmail = findViewById(R.id.txtEmail);
        txtContrasena = findViewById(R.id.txtContrasena);
        txtConfirmarContrasena = findViewById(R.id.txtConfirmarContrasena);
        spTipoUsuario = findViewById(R.id.spTipoUsuario);
        cbTerminos = findViewById(R.id.cbTerminos);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);

        // Inicialización del controlador de base de datos
        usuarioControlador = new cUsuario();

        // Configuración del Spinner
        String[] opcionesUsuario = {"Selecciona un tipo", "Estudiante", "Docente", "Profesional/Empresarial"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opcionesUsuario);
        spTipoUsuario.setAdapter(adapter);
    }

    public void registrarUsuario(View view) {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().toString().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().toString().trim() : "";
        String pass = txtContrasena.getText() != null ? txtContrasena.getText().toString().trim() : "";
        String confirmPass = txtConfirmarContrasena.getText() != null ? txtConfirmarContrasena.getText().toString().trim() : "";

        // 1. Validación de campos vacíos
        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Validación de selección de rol/tipo
        if (spTipoUsuario.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Por favor selecciona un tipo de usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Validación de coincidencia de contraseñas
        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Validación de longitud de contraseña
        if (pass.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. Validación de términos y condiciones
        if (!cbTerminos.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deshabilitar botón temporalmente para evitar doble clic
        btnCrearCuenta.setEnabled(false);

        // 6. Proceso de inserción en SQL Server mediante hilo secundario
        new Thread(() -> {
            // Verificar si el correo ya existe en la base de datos
            if (usuarioControlador.existeCorreo(email)) {
                runOnUiThread(() -> {
                    Toast.makeText(frm_registro.this, "El correo ya se encuentra registrado", Toast.LENGTH_LONG).show();
                    btnCrearCuenta.setEnabled(true);
                });
                return;
            }

            // Crear objeto de modelo e insertar
            Usuario nuevoUsuario = new Usuario(nombre, email, pass, "");
            boolean exito = usuarioControlador.registrar(nuevoUsuario);

            runOnUiThread(() -> {
                btnCrearCuenta.setEnabled(true);
                if (exito) {
                    Toast.makeText(frm_registro.this, "¡Cuenta creada exitosamente en SQL Server!", Toast.LENGTH_LONG).show();
                    // Redirige al login para iniciar sesión formalmente
                    Intent intent = new Intent(frm_registro.this, frm_login.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(frm_registro.this, "Error al registrar en la base de datos. Verifique conexión.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    public void volverAlLogin(View view) {
        finish();
    }
}