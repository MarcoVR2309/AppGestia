package com.example.maquetagestia;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class frm_configuracion extends AppCompatActivity {
    private SwitchMaterial switchPush;
    private SwitchMaterial switchDark;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_configuracion);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        switchPush = findViewById(R.id.switchPush);
        switchDark = findViewById(R.id.switchDark);

        switchPush.setOnCheckedChangeListener((new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean b) {
                if (b){
                    Toast.makeText(frm_configuracion.this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(frm_configuracion.this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
                }
            }
        }));
        switchDark.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean b) {
                if (b){
                    Toast.makeText(frm_configuracion.this, "Modo oscuro en desarrollo", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void volverAlPerfil(View view){
        finish();
    }

}