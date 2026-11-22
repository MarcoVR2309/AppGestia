package com.example.maquetagestia.vista;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.maquetagestia.R;
import com.example.maquetagestia.controller.Sesion;
import com.example.maquetagestia.controller.cUsuario;
import com.example.maquetagestia.controller.cUsuario.ResultadoLogin;
import com.example.maquetagestia.model.Usuario;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Recuperar contraseña");

        int paddingPx = Math.round(20 * getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2);

        EditText txtCorreoRec = new EditText(this);
        txtCorreoRec.setHint("Correo electrónico registrado");
        txtCorreoRec.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (email != null && email.getText() != null) {
            txtCorreoRec.setText(email.getText().toString().trim());
        }
        layout.addView(txtCorreoRec);

        EditText txtTelefonoRec = new EditText(this);
        txtTelefonoRec.setHint("Teléfono registrado");
        txtTelefonoRec.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(txtTelefonoRec);

        EditText txtNuevaPass = new EditText(this);
        txtNuevaPass.setHint("Nueva contraseña (mínimo 6 caracteres)");
        txtNuevaPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(txtNuevaPass);

        EditText txtConfirmarPass = new EditText(this);
        txtConfirmarPass.setHint("Confirmar nueva contraseña");
        txtConfirmarPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(txtConfirmarPass);

        builder.setView(layout);
        builder.setPositiveButton("Restablecer", (dialog, which) -> {
            String correo = txtCorreoRec.getText() != null ? txtCorreoRec.getText().toString().trim() : "";
            String telefono = txtTelefonoRec.getText() != null ? txtTelefonoRec.getText().toString().trim() : "";
            String nuevaPass = txtNuevaPass.getText() != null ? txtNuevaPass.getText().toString().trim() : "";
            String confirmarPass = txtConfirmarPass.getText() != null ? txtConfirmarPass.getText().toString().trim() : "";

            if (TextUtils.isEmpty(correo) || TextUtils.isEmpty(telefono)
                    || TextUtils.isEmpty(nuevaPass) || TextUtils.isEmpty(confirmarPass)) {
                Toast.makeText(this, "Complete todos los campos para verificar identidad", Toast.LENGTH_SHORT).show();
                return;
            }
            if (nuevaPass.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!nuevaPass.equals(confirmarPass)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                boolean exito = usuarioControlador.restablecerContrasena(correo, telefono, nuevaPass);
                runOnUiThread(() -> {
                    if (exito) {
                        Toast.makeText(frm_login.this, "Contraseña restablecida correctamente", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(frm_login.this, "Correo o teléfono no coinciden con una cuenta válida", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void iniciarConGoogle(View view) {
        String webClientId = getString(R.string.google_web_client_id);
        if (TextUtils.isEmpty(webClientId) || "PEGA_AQUI_TU_WEB_CLIENT_ID".equals(webClientId)) {
            Toast.makeText(this, "Configura tu Google Web Client ID en strings.xml (google_web_client_id)", Toast.LENGTH_LONG).show();
            return;
        }

        if (google != null) {
            google.setEnabled(false);
        }

        CredentialManager credentialManager = CredentialManager.create(this);
        GetSignInWithGoogleOption googleOption = new GetSignInWithGoogleOption.Builder(webClientId).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            try {
                                GoogleIdTokenCredential googleCred = GoogleIdTokenCredential.createFrom(credential.getData());
                                String correoGoogle = googleCred.getId();
                                String nombreGoogle = googleCred.getDisplayName();
                                if (TextUtils.isEmpty(nombreGoogle) && correoGoogle.contains("@")) {
                                    nombreGoogle = correoGoogle.substring(0, correoGoogle.indexOf('@'));
                                }
                                String avatarGoogle = googleCred.getProfilePictureUri() != null
                                        ? googleCred.getProfilePictureUri().toString()
                                        : null;
                                String idToken = googleCred.getIdToken();

                                final String nombreFinal = nombreGoogle;
                                new Thread(() -> {
                                    Usuario usuario = usuarioControlador.autenticarOCrearConGoogle(
                                            correoGoogle, nombreFinal, avatarGoogle, idToken);
                                    runOnUiThread(() -> {
                                        if (google != null) google.setEnabled(true);
                                        if (usuario != null) {
                                            Sesion.guardar(frm_login.this, usuario);
                                            Toast.makeText(frm_login.this, "¡Bienvenido, " + usuario.getNombreCompleto() + "!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(frm_login.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(frm_login.this, "No se pudo autenticar la cuenta de Google en SQL Server", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }).start();
                            } catch (Exception e) {
                                if (google != null) google.setEnabled(true);
                                Toast.makeText(frm_login.this, "No se pudo leer la credencial de Google", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            if (google != null) google.setEnabled(true);
                            Toast.makeText(frm_login.this, "Tipo de credencial inesperado de Google", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (google != null) google.setEnabled(true);
                        if (e instanceof GetCredentialCancellationException) {
                            Toast.makeText(frm_login.this, "Inicio con Google cancelado", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(frm_login.this, "Error con Google Sign-In: Verifique red, Play Services o Web Client ID", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }
}