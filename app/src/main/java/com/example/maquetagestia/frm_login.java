package com.example.maquetagestia;

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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class frm_login extends AppCompatActivity {
    private TextInputEditText email, contrasena;
    private MaterialButton login, google;
    private TextView forgotPassword, registro;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    public void iniciarSesion(View view){
        String txtEmail = email.getText().toString();
        String txtPass = contrasena.getText().toString();

        if(!txtEmail.isEmpty() && !txtPass.isEmpty()){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }else {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_LONG).show();
        }
    }
    public void Registro(View view) {
        // Redirige a la pantalla de Registro
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