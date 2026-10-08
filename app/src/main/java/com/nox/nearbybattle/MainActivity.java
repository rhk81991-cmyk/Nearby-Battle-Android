package com.nox.nearbybattle;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends Activity {

    // TAP WAR
    private CountDownTimer tapWarTimer;

    private int player1Score = 0;
    private int player2Score = 0;

    private TextView tapScore;
    private TextView tapTimer;
    private TextView tapStatus;

    private Button player1Button;
    private Button player2Button;
    private Button startButton;

    // REACTION BATTLE
    private final Handler reactionHandler =
            new Handler(Looper.getMainLooper());

    private final Random random = new Random();

    private Runnable reactionSignalRunnable;

    private TextView reactionRoundText;
    private TextView reactionStatusText;
    private TextView reactionTimesText;

    private Button reactionP1Button;
    private Button reactionP2Button;
    private Button reactionStartButton;

    private int reactionRound = 1;

    private int p1RoundWins = 0;
    private int p2RoundWins = 0;

    private int p1ValidRounds = 0;
    private int p2ValidRounds = 0;

    private long p1TotalReaction = 0;
    private long p2TotalReaction = 0;

    private long signalTime = 0;

    private boolean reactionWaiting = false;
    private boolean reactionSignalShown = false;
    private boolean p1Responded = false;
    private boolean p2Responded = false;
    private boolean reactionGameFinished = false;

    private long p1Reaction = -1;
    private long p2Reaction = -1;

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHomeScreen();
    }

    // HOME SCREEN

    public void showHomeScreen() {
        stopTapWarTimer();
        stopReactionDelay();

        setContentView(R.layout.activity_main);

        Button bluetoothButton = findViewById(R.id.btnBluetooth);
        Button offlineButton = findViewById(R.id.btnOffline);
        Button gamesButton = findViewById(R.id.btnGames);

        bluetoothButton.setOnClickListener(v ->
        new BluetoothBattle(this).start()

        );

        offlineButton.setOnClickListener(v ->
                showMessage("Offline Local — coming next!")
        );

        gamesButton.setOnClickListener(v ->
                showGamesScreen()
        );
    }

    // GAME HUB

    public void showGamesScreen() {
        stopTapWarTimer();
        stopReactionDelay();

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
                showReactionScreen()
        );

        rps.setOnClickListener(v ->
        new RockPaperScissorsGame(this).start()

        );

        numberRush.setOnClickListener(v ->
        new NumberRushGame(this).start()
  
        );

        back.setOnClickListener(v ->
                showHomeScreen()
        );
    }

    // TAP WAR

    private void showTapWarScreen() {
        stopTapWarTimer();
        stopReactionDelay();

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
        startButton.setText("⚔️ START BATTLE");

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
        stopTapWarTimer();

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

    // REACTION BATTLE SCREEN

    private void showReactionScreen() {
        stopTapWarTimer();
        stopReactionDelay();

        setContentView(R.layout.activity_reaction);

        reactionRoundText = findViewById(R.id.txtReactionRound);
        reactionStatusText = findViewById(R.id.txtReactionStatus);
        reactionTimesText = findViewById(R.id.txtReactionTimes);

        reactionP1Button = findViewById(R.id.btnReactionP1);
        reactionP2Button = findViewById(R.id.btnReactionP2);
        reactionStartButton = findViewById(R.id.btnReactionStart);

        Button back = findViewById(R.id.btnReactionBack);

        resetReactionGame();

        reactionP1Button.setOnClickListener(v ->
                onReactionTap(1)
        );

        reactionP2Button.setOnClickListener(v ->
                onReactionTap(2)
        );

        reactionStartButton.setOnClickListener(v ->
                startReactionRound()
        );

        back.setOnClickListener(v ->
                showGamesScreen()
        );
    }

    private void resetReactionGame() {
        reactionRound = 1;

        p1RoundWins = 0;
        p2RoundWins = 0;

        p1ValidRounds = 0;
        p2ValidRounds = 0;

        p1TotalReaction = 0;
        p2TotalReaction = 0;

        reactionWaiting = false;
        reactionSignalShown = false;
        p1Responded = false;
        p2Responded = false;
        reactionGameFinished = false;

        p1Reaction = -1;
        p2Reaction = -1;

        updateReactionDisplay();

        reactionStatusText.setText("READY FOR BATTLE?");

        reactionP1Button.setEnabled(false);
        reactionP2Button.setEnabled(false);

        reactionStartButton.setEnabled(true);
        reactionStartButton.setText("⚔️ START BATTLE");
    }

    private void startReactionRound() {
        if (reactionGameFinished) {
            resetReactionGame();
        }

        if (reactionWaiting || reactionSignalShown) {
            return;
        }

        p1Reaction = -1;
        p2Reaction = -1;

        p1Responded = false;
        p2Responded = false;

        reactionWaiting = true;
        reactionSignalShown = false;

        reactionRoundText.setText(
                "ROUND " + reactionRound + " / 3"
        );

        reactionTimesText.setText(
                "P1: --- ms     P2: --- ms"
        );

        reactionStatusText.setText(
                "👀 WAIT FOR THE SIGNAL..."
        );

        reactionP1Button.setEnabled(true);
        reactionP2Button.setEnabled(true);

        reactionStartButton.setEnabled(false);

        // Random delay: 2 to 5 seconds
        int delay = 2000 + random.nextInt(3001);

        reactionSignalRunnable = () -> {
            if (!reactionWaiting) {
                return;
            }

            reactionWaiting = false;
            reactionSignalShown = true;

            signalTime = SystemClock.elapsedRealtime();

            reactionStatusText.setText("⚡ TAP NOW!");
        };

        reactionHandler.postDelayed(
                reactionSignalRunnable,
                delay
        );
    }

    private void onReactionTap(int player) {
        if (reactionGameFinished) {
            return;
        }

        // Tapping before the signal causes a false start.
        if (reactionWaiting && !reactionSignalShown) {
            handleFalseStart(player);
            return;
        }

        if (!reactionSignalShown) {
            return;
        }

        long reactionTime =
                SystemClock.elapsedRealtime() - signalTime;

        if (player == 1 && !p1Responded) {
            p1Responded = true;
            p1Reaction = reactionTime;
            reactionP1Button.setEnabled(false);
        } else if (player == 2 && !p2Responded) {
            p2Responded = true;
            p2Reaction = reactionTime;
            reactionP2Button.setEnabled(false);
        }

        reactionTimesText.setText(
                "P1: " + formatReaction(p1Reaction)
                        + "     P2: " + formatReaction(p2Reaction)
        );

        if (p1Responded && p2Responded) {
            finishReactionRound(false, 0);
        } else {
            reactionStatusText.setText(
                    "⚡ " + (p1Responded
                            ? "P1 LOCKED IN! P2, TAP!"
                            : "P2 LOCKED IN! P1, TAP!")
            );
        }
    }

    private void handleFalseStart(int player) {
        stopReactionDelay();

        reactionWaiting = false;
        reactionSignalShown = false;

        reactionP1Button.setEnabled(false);
        reactionP2Button.setEnabled(false);

        if (player == 1) {
            p1Reaction = -1;
            p2Reaction = 200;
            p2RoundWins++;

            reactionStatusText.setText(
                    "🚫 P1 FALSE START!\nP2 WINS THIS ROUND!"
            );
        } else {
            p2Reaction = -1;
            p1Reaction = 200;
            p1RoundWins++;

            reactionStatusText.setText(
                    "🚫 P2 FALSE START!\nP1 WINS THIS ROUND!"
            );
        }

        reactionTimesText.setText(
                "P1: " + formatReaction(p1Reaction)
                        + "     P2: " + formatReaction(p2Reaction)
        );

        // Only the non-false-start player receives a valid time.
        if (player == 1) {
            p2TotalReaction += 200;
            p2ValidRounds++;
        } else {
            p1TotalReaction += 200;
            p1ValidRounds++;
        }

        prepareNextReactionRound();
    }

    private void finishReactionRound(
            boolean falseStart,
            int falseStartPlayer
    ) {
        reactionSignalShown = false;
        reactionWaiting = false;

        reactionP1Button.setEnabled(false);
        reactionP2Button.setEnabled(false);

        if (!falseStart) {
            p1TotalReaction += p1Reaction;
            p2TotalReaction += p2Reaction;

            p1ValidRounds++;
            p2ValidRounds++;

            if (p1Reaction < p2Reaction) {
                p1RoundWins++;
                reactionStatusText.setText(
                        "🏆 P1 WINS THIS ROUND!"
                );
            } else if (p2Reaction < p1Reaction) {
                p2RoundWins++;
                reactionStatusText.setText(
                        "🏆 P2 WINS THIS ROUND!"
                );
            } else {
                reactionStatusText.setText(
                        "🤝 ROUND DRAW!"
                );
            }
        }

        prepareNextReactionRound();
    }

    private void prepareNextReactionRound() {
        reactionStartButton.setEnabled(true);

        if (reactionRound < 3) {
            reactionRound++;

            reactionRoundText.setText(
                    "ROUND " + reactionRound + " / 3"
            );

            reactionStartButton.setText("➡️ NEXT ROUND");

        } else {
            finishReactionGame();
        }
    }

    private void finishReactionGame() {
        reactionGameFinished = true;

        reactionP1Button.setEnabled(false);
        reactionP2Button.setEnabled(false);

        double p1Average = p1ValidRounds > 0
                ? (double) p1TotalReaction / p1ValidRounds
                : Double.MAX_VALUE;

        double p2Average = p2ValidRounds > 0
                ? (double) p2TotalReaction / p2ValidRounds
                : Double.MAX_VALUE;

        String winner;

        if (p1RoundWins > p2RoundWins) {
            winner = "🏆 PLAYER 1 WINS!";
        } else if (p2RoundWins > p1RoundWins) {
            winner = "🏆 PLAYER 2 WINS!";
        } else if (p1Average < p2Average) {
            winner = "🏆 PLAYER 1 WINS ON AVERAGE!";
        } else if (p2Average < p1Average) {
            winner = "🏆 PLAYER 2 WINS ON AVERAGE!";
        } else {
            winner = "🤝 OVERALL DRAW!";
        }

        reactionStatusText.setText(winner);

        String p1AvgText = p1ValidRounds > 0
                ? String.format("%.1f", p1Average)
                : "N/A";

        String p2AvgText = p2ValidRounds > 0
                ? String.format("%.1f", p2Average)
                : "N/A";

        reactionTimesText.setText(
                "Round wins: P1 " + p1RoundWins
                        + " - " + p2RoundWins + " P2\n"
                        + "P1 average: " + p1AvgText + " ms\n"
                        + "P2 average: " + p2AvgText + " ms"
        );

        reactionStartButton.setText("🔄 PLAY AGAIN");
        reactionStartButton.setEnabled(true);

        showMessage(winner);
    }

    private String formatReaction(long time) {
        if (time < 0) {
            return "FALSE START";
        }

        return time + " ms";
    }

    private void updateReactionDisplay() {
        reactionRoundText.setText(
                "ROUND " + reactionRound + " / 3"
        );

        reactionTimesText.setText(
                "P1: --- ms     P2: --- ms"
        );
    }

    private void stopReactionDelay() {
        if (reactionSignalRunnable != null) {
            reactionHandler.removeCallbacks(
                    reactionSignalRunnable
            );

            reactionSignalRunnable = null;
        }

        reactionWaiting = false;
        reactionSignalShown = false;
    }

    @Override
    protected void onDestroy() {
        stopTapWarTimer();
        stopReactionDelay();

        super.onDestroy();
    }
}
