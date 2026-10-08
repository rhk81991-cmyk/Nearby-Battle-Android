package com.nox.nearbybattle;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private CountDownTimer tapWarTimer;

    private int player1Score = 0;
    private int player2Score = 0;

    private TextView tapScore;
    private TextView tapTimer;

    private Button player1Button;
    private Button player2Button;

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHomeScreen();
    }

    private void showHomeScreen() {
        stopTapWarTimer();

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
        stopTapWarTimer();

        setContentView(R.layout.activity_games);

        Button tapWar = findViewById(R.id.btnTapWar);
        Button reaction = findViewById(R.id.btnReaction);
        Button rps = findViewById(R.id.btnRPS);
        Button numberRush = findViewById(R.id.btnNumberRush);
        Button back = findViewById(R.id.btnGamesBack);

        tapWar.setOnClickListener(v ->
                showTapWarScreen()
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

    private void showTapWarScreen() {
        setContentView(R.layout.activity_tap_war);

        tapScore = findViewById(R.id.txtTapScore);
        tapTimer = findViewById(R.id.txtTapTimer);

        player1Button = findViewById(R.id.btnPlayer1);
        player2Button = findViewById(R.id.btnPlayer2);
        Button back = findViewById(R.id.btnTapBack);

        player1Score = 0;
        player2Score = 0;

        updateScore();

        player1Button.setEnabled(true);
        player2Button.setEnabled(true);

        player1Button.setOnClickListener(v -> {
            player1Score++;
            updateScore();
        });

        player2Button.setOnClickListener(v -> {
            player2Score++;
            updateScore();
        });

        back.setOnClickListener(v ->
                showGamesScreen()
        );

        startTapWarTimer();
    }

    private void updateScore() {
        if (tapScore != null) {
            tapScore.setText(
                    "P1: " + player1Score + "     P2: " + player2Score
            );
        }
    }

    private void startTapWarTimer() {
        stopTapWarTimer();

        tapTimer.setText("10");

        tapWarTimer = new CountDownTimer(10000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tapTimer.setText(String.valueOf(seconds));
            }

            @Override
            public void onFinish() {
                tapTimer.setText("0");

                player1Button.setEnabled(false);
                player2Button.setEnabled(false);

                showWinner();
            }
        };

        tapWarTimer.start();
    }

    private void showWinner() {
        String result;

        if (player1Score > player2Score) {
            result = "🏆 Player 1 Wins!\n\n" +
                    player1Score + " - " + player2Score;
        } else if (player2Score > player1Score) {
            result = "🏆 Player 2 Wins!\n\n" +
                    player2Score + " - " + player1Score;
        } else {
            result = "🤝 DRAW!\n\n" +
                    player1Score + " - " + player2Score;
        }

        showMessage(result);
    }

    private void stopTapWarTimer() {
        if (tapWarTimer != null) {
            tapWarTimer.cancel();
            tapWarTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopTapWarTimer();
        super.onDestroy();
    }
}
