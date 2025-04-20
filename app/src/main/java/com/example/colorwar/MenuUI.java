package com.example.colorwar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class MenuUI extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_ui);

        // Sự kiện cho nút "How to Play"
        ImageButton howToPlayButton = findViewById(R.id.how_to_play_button);
        howToPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuUI.this, Rule.class);
                startActivity(intent);
            }
        });
        // Sự kiện cho nút "Play vs. Bot"
        ImageButton playVsBotButton = findViewById(R.id.play_vs_bot_button);
        playVsBotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuUI.this, PlayVsBot.class);
                startActivity(intent);
            }
        });

        // Sự kiện cho nút "Exit"
        ImageButton exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity(); // Thoát hoàn toàn ứng dụng
            }
        });
        ImageButton playVsFriend = findViewById(R.id.play_vs_friend_button);
        playVsFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuUI.this, GameActivity.class);
                startActivity(intent);
            }
        });

    }
}