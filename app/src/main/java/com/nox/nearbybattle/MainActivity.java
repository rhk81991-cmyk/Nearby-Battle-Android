package com.nox.nearbybattle;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private CountDownTimer tapWarTimer;

    private int player1Score = 0;
    private int player2Score = 0;

    private TextView tapScore;
    private TextView tapTimer;
    private TextView tapStatus;

    private Button player1Button;
    private Button player2Button;
    private Button startButton;

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
        stopTapWarTimer();

        setContentView(R.layout.activity_tap_war);

        tapScore = findViewById(R.id.txtTapScore);
        tapTimer = findViewById(R.id.txtTapTimer);
        tapStatus = findViewById(R.id.txtTapStatus);

        player1Button = findViewById(R.id.btnPlayer1);
        player2Button = findViewById(R.id.btnPlayer2);
        startButton = findViewById(R.id.btnStartTapWar);

        Button back = findViewById(R.id.btnTapBack);

        player1Score = 0;
        player2Score = 0;

        updateScore();

        tapTimer.setText("10");
        tapStatus.setText("READY FOR BATTLE?");

        player1Button.setEnabled(false);
        player2Button.setEnabled(false);
        startButton.setEnabled(true);

        player1Button.setOnClickListener(v -> {
            player1Score++;
            updateScore();
        });

        player2Button.setOnClickListener(v -> {
            player2Score++;
            updateScore();
        });

        startButton.setOnClickListener(v ->
                startTapWar()
        );

        back.setOnClickListener(v ->
                showGamesScreen()
        );
    }

    private void startTapWar() {
        player1Score = 0;
        player2Score = 0;

        updateScore();

        tapStatus.setText("🔥 BATTLE!");

        player1Button.setEnabled(true);
        player2Button.setEnabled(true);
        startButton.setEnabled(false);

        tapWarTimer = new CountDownTimer(10000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = (millisUntilFinished + 999) / 1000;
                tapTimer.setText(String.valueOf(seconds));
            }

            @Override
            public void onFinish() {
                tapTimer.setText("0");

                player1Button.setEnabled(false);
                player2Button.setEnabled(false);

                tapStatus.setText("🏆 BATTLE OVER!");

                showWinner();

                startButton.setText("🔄 PLAY AGAIN");
                startButton.setEnabled(true);

                startButton.setOnClickListener(v ->
                        startTapWar()
                );
            }
        };

        tapWarTimer.start();
    }

    private void updateScore() {
        if (tapScore != null) {
            tapScore.setText(
                    "P1: " + player1Score + "     P2: " + player2Score
            );
        }
    }

    private void showWinner() {
        String result;

        if (player1Score > player2Score) {
            result = "🏆 Player 1 Wins!\n\n"
                    + player1Score + " - " + player2Score;
        } else if (player2Score > player1Score) {
            result = "🏆 Player 2 Wins!\n\n"
                    + player2Score + " - " + player1Score;
        } else {
            result = "🤝 DRAW!\n\n"
                    + player1Score + " - " + player2Score;
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
