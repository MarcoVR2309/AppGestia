package com.example.maquetagestia;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class frm_splash extends AppCompatActivity {
    private ImageView imgLogo;
    private TextView tvBrand;
    private TextView tvSlogan;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frm_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_splash), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        imgLogo = findViewById(R.id.imgLogo);
        tvBrand = findViewById(R.id.tvBrand);
        tvSlogan = findViewById(R.id.tvSlogan);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(frm_splash.this, frm_login.class);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}