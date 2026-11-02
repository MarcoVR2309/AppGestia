package com.example.maquetagestia.vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cUsuario;
import com.example.maquetagestia.controller.cUsuario.ResultadoLogin;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class frm_login extends AppCompatActivity {

    private TextInputEditText email, contrasena;
    private MaterialButton login, google;
    private TextView forgotPassword, registro;
    private cUsuario usuarioControlador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Si la sesión ya está activa, ir directamente al Dashboard
        if (Sesion.estaActiva(this)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = findViewById(R.id.txtEmail);
        contrasena = findViewById(R.id.txtContrasena);
        login = findViewById(R.id.btnLogin);
        google = findViewById(R.id.btnGoogle);
        forgotPassword = findViewById(R.id.tvForgotPassword);
        registro = findViewById(R.id.tvRegister);

        usuarioControlador = new cUsuario();
    }

    public void iniciarSesion(View view) {
        String txtEmail = email.getText() != null ? email.getText().toString().trim() : "";
        String txtPass = contrasena.getText() != null ? contrasena.getText().toString().trim() : "";

        if (txtEmail.isEmpty() || txtPass.isEmpty()) {
            Toast.makeText(this, "Por favor complete correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        // Feedback visual
        login.setEnabled(false);
        login.setText("Verificando...");

        new Thread(() -> {
            ResultadoLogin res = usuarioControlador.iniciarSesionEx(txtEmail, txtPass);

            runOnUiThread(() -> {
                login.setEnabled(true);
                login.setText("Iniciar sesión");

                if (res.usuario != null) {
                    Sesion.guardar(frm_login.this, res.usuario);
                    Toast.makeText(frm_login.this, "¡Bienvenido, " + res.usuario.getNombreCompleto() + "!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(frm_login.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(frm_login.this, res.mensajeError, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    public void Registro(View view) {
        Intent intent = new Intent(this, frm_registro.class);
        startActivity(intent);
    }

    public void recuperarContrasena(View view) {
        Toast.makeText(this, "Opción de recuperación en desarrollo", Toast.LENGTH_SHORT).show();
    }

    public void iniciarConGoogle(View view) {
        Toast.makeText(this, "Inicio con Google en desarrollo", Toast.LENGTH_SHORT).show();
    }
}