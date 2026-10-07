package com.nox.nearbybattle;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHomeScreen();
    }

    private void showHomeScreen() {
        setContentView(R.layout.activity_main);

        Button bluetoothButton = findViewById(R.id.btnBluetooth);
        Button offlineButton = findViewById(R.id.btnOffline);
        Button gamesButton = findViewById(R.id.btnGames);

        bluetoothButton.setOnClickListener(v ->
                showMessage("Bluetooth Battle — coming next!")
        );

        offlineButton.setOnClickListener(v ->
                showMessage("Offline Local — coming next!")
        );

        gamesButton.setOnClickListener(v ->
                showGamesScreen()
        );
    }

    private void showGamesScreen() {
        setContentView(R.layout.activity_games);

        Button tapWar = findViewById(R.id.btnTapWar);
        Button reaction = findViewById(R.id.btnReaction);
        Button rps = findViewById(R.id.btnRPS);
        Button numberRush = findViewById(R.id.btnNumberRush);
        Button back = findViewById(R.id.btnGamesBack);

        tapWar.setOnClickListener(v ->
                showMessage("Tap War — game coming next!")
        );

        reaction.setOnClickListener(v ->
                showMessage("Reaction Battle — game coming next!")
        );

        rps.setOnClickListener(v ->
                showMessage("Rock Paper Scissors — game coming next!")
        );

        numberRush.setOnClickListener(v ->
                showMessage("Number Rush — game coming next!")
        );

        back.setOnClickListener(v ->
                showHomeScreen()
        );
    }
    }
