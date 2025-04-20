package com.example.colorwar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class PlayVsBot extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_vs_bot);

        // Các nút
        ImageButton easyButton = findViewById(R.id.choose_easy_button);
        ImageButton mediumButton = findViewById(R.id.choose_medium_button);
        ImageButton hardButton = findViewById(R.id.choose_hard_button);
        ImageButton backButton = findViewById(R.id.back_button);

        // Xử lý nút Easy
        easyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlayVsBot.this, GameVsBotActivity.class);
                intent.putExtra("DIFFICULTY", "Easy");
                startActivity(intent);
            }
        });

        // Xử lý nút Medium (Normal)
        mediumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlayVsBot.this, GameVsBotActivity.class);
                intent.putExtra("DIFFICULTY", "Normal");
                startActivity(intent);
            }
        });

        // Xử lý nút Hard
        hardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlayVsBot.this, GameVsBotActivity.class);
                intent.putExtra("DIFFICULTY", "Hard");
                startActivity(intent);
            }
        });

        // Xử lý nút Back
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Quay lại màn hình trước (MenuUI)
            }
        });

        // Đặt trạng thái ban đầu: Tất cả nút sáng (vì giờ đều có chức năng)
        easyButton.setAlpha(1.0f);
        mediumButton.setAlpha(1.0f);
        hardButton.setAlpha(1.0f);
    }
}