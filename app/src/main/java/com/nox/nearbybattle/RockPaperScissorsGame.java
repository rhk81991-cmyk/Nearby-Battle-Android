package com.nox.nearbybattle;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class RockPaperScissorsGame {

    private final MainActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private TextView txtRound, txtStatus, txtScore;
    private Button btnRock, btnPaper, btnScissors;
    private Button btnNext, btnBack;

    private String player1Move = "";
    private String player2Move = "";

    private int round = 1;
    private int player1Score = 0;
    private int player2Score = 0;
    private int draws = 0;

    private boolean player1Picking = true;
    private boolean gameFinished = false;

    public RockPaperScissorsGame(MainActivity activity) {
        this.activity = activity;
    }

    public void start() {
        activity.setContentView(R.layout.activity_rps);

        txtRound = activity.findViewById(R.id.txtRpsRound);
        txtStatus = activity.findViewById(R.id.txtRpsStatus);
        txtScore = activity.findViewById(R.id.txtRpsScore);

        btnRock = activity.findViewById(R.id.btnRpsRock);
        btnPaper = activity.findViewById(R.id.btnRpsPaper);
        btnScissors = activity.findViewById(R.id.btnRpsScissors);
        btnNext = activity.findViewById(R.id.btnRpsNext);
        btnBack = activity.findViewById(R.id.btnRpsBack);

        btnRock.setOnClickListener(v -> chooseMove("Rock"));
        btnPaper.setOnClickListener(v -> chooseMove("Paper"));
        btnScissors.setOnClickListener(v -> chooseMove("Scissors"));

        btnNext.setOnClickListener(v -> startNextRound());
        btnBack.setOnClickListener(v -> activity.showGamesScreen());

        updateScore();
        prepareRound();
    }

    private void prepareRound() {
        gameFinished = false;
        player1Picking = true;
        player1Move = "";
        player2Move = "";

        txtRound.setText("ROUND " + round + " / 5");
        txtStatus.setText("PLAYER 1: CHOOSE YOUR MOVE");
        btnNext.setVisibility(View.GONE);
        setChoicesEnabled(true);
        updateScore();
    }

    private void chooseMove(String move) {
        if (gameFinished) return;

        if (player1Picking) {
            player1Move = move;
            player1Picking = false;

            txtStatus.setText(
                    "PLAYER 1 LOCKED 🔒\n\n" +
                    "Pass the phone to PLAYER 2.\n" +
                    "Player 2, choose your move!"
            );

        } else {
            player2Move = move;
            setChoicesEnabled(false);

            txtStatus.setText("PLAYER 2 LOCKED 🔒\n\nGet ready...");
            beginReveal();
        }
    }

    private void beginReveal() {
        new CountDownTimer(3000, 1000) {
            int count = 3;

            @Override
            public void onTick(long millisUntilFinished) {
                txtStatus.setText(String.valueOf(count));
                count--;
            }

            @Override
            public void onFinish() {
                revealResult();
            }
        }.start();
    }

    private void revealResult() {
        String result;

        if (player1Move.equals(player2Move)) {
            draws++;
            result = "🤝 DRAW!";
        } else if (
                (player1Move.equals("Rock") && player2Move.equals("Scissors")) ||
                (player1Move.equals("Paper") && player2Move.equals("Rock")) ||
                (player1Move.equals("Scissors") && player2Move.equals("Paper"))
        ) {
            player1Score++;
            result = "🔴 PLAYER 1 WINS ROUND!";
        } else {
            player2Score++;
            result = "🔵 PLAYER 2 WINS ROUND!";
        }

        txtStatus.setText(
                "3... 2... 1... REVEAL!\n\n" +
                "🔴 P1: " + emoji(player1Move) + " " + player1Move + "\n" +
                "🔵 P2: " + emoji(player2Move) + " " + player2Move + "\n\n" +
                result
        );

        updateScore();
        gameFinished = true;
        btnNext.setVisibility(View.VISIBLE);

        if (round >= 5) {
            showFinalResult();
        } else {
            btnNext.setText("NEXT ROUND ➜");
        }
    }

    private void showFinalResult() {
        if (player1Score > player2Score) {
            txtStatus.append("\n\n🏆 PLAYER 1 WINS THE MATCH!");
        } else if (player2Score > player1Score) {
            txtStatus.append("\n\n🏆 PLAYER 2 WINS THE MATCH!");
        } else {
            txtStatus.append("\n\n🤝 MATCH DRAW!");
        }

        btnNext.setText("PLAY AGAIN 🔄");
    }

    private void startNextRound() {
        if (round >= 5) {
            round = 1;
            player1Score = 0;
            player2Score = 0;
            draws = 0;
            prepareRound();
            return;
        }

        round++;
        prepareRound();
    }

    private void updateScore() {
        txtScore.setText(
                "🔴 P1: " + player1Score +
                "     🔵 P2: " + player2Score +
                "     Draws: " + draws
        );
    }

    private void setChoicesEnabled(boolean enabled) {
        btnRock.setEnabled(enabled);
        btnPaper.setEnabled(enabled);
        btnScissors.setEnabled(enabled);
    }

    private String emoji(String move) {
        switch (move) {
            case "Rock": return "✊";
            case "Paper": return "✋";
            default: return "✌️";
        }
    }
                                        }
