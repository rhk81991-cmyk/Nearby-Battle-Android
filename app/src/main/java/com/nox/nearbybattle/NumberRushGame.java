package com.nox.nearbybattle;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class NumberRushGame {

    private final MainActivity activity;
    private final Random random = new Random();

    private LinearLayout root;
    private TextView title, info, question, score;
    private Button b1, b2, b3, action, back;
    private EditText answerInput;
    private CountDownTimer timer;

    private int mode = 0;
    private int player = 1;
    private int round = 1;
    private int p1Score = 0;
    private int p2Score = 0;

    private int memoryNumber;
    private int correctAnswer;
    private int huntTarget;

    private boolean waiting = false;
    private boolean gameOver = false;

    public NumberRushGame(MainActivity activity) {
        this.activity = activity;
    }

    public void start() {
        showMenu();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void setup(String heading) {
        cancelTimer();

        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(20, 12, 20, 12);
        root.setBackgroundColor(Color.rgb(18, 22, 38));

        title = makeText(heading, 27);
        info = makeText("", 17);
        score = makeText("", 17);
        question = makeText("", 23);

        root.addView(title);
        root.addView(info);
        root.addView(score);
        root.addView(question);

        b1 = makeButton("");
        b2 = makeButton("");
        b3 = makeButton("");
        action = makeButton("");
        back = makeButton("BACK TO GAMES");

        root.addView(b1);
        root.addView(b2);
        root.addView(b3);
        root.addView(action);
        root.addView(back);

        back.setOnClickListener(v -> {
            cancelTimer();
            activity.showGamesScreen();
        });

        activity.setContentView(root);
        updateScore();
    }

    private TextView makeText(String value, int size) {
        TextView t = new TextView(activity);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(6, 8, 6, 8);
        return t;
    }

    private Button makeButton(String value) {
        Button b = new Button(activity);
        b.setText(value);
        b.setTextSize(15);
        return b;
    }

    private void showMenu() {
        waiting = false;
        gameOver = false;

        setup("NUMBER RUSH");

        info.setText("Choose a game mode");
        question.setText("2 PLAYERS • SAME PHONE");

        b1.setText("NUMBER MEMORY");
        b2.setText("FAST CALCULATION");
        b3.setText("NUMBER HUNT");
        action.setText("START");

        b1.setOnClickListener(v -> selectMode(1));
        b2.setOnClickListener(v -> selectMode(2));
        b3.setOnClickListener(v -> selectMode(3));
        action.setOnClickListener(v -> beginMode());
    }

    private void selectMode(int selected) {
        mode = selected;

        b1.setText((mode == 1 ? "✓ " : "") + "NUMBER MEMORY");
        b2.setText((mode == 2 ? "✓ " : "") + "FAST CALCULATION");
        b3.setText((mode == 3 ? "✓ " : "") + "NUMBER HUNT");

        info.setText("Selected: " + modeName());
    }

    private String modeName() {
        if (mode == 1) return "Number Memory";
        if (mode == 2) return "Fast Calculation";
        if (mode == 3) return "Number Hunt";
        return "Choose a mode";
    }

    private void beginMode() {
        if (mode == 0) {
            info.setText("Choose a mode first!");
            return;
        }

        player = 1;
        round = 1;
        p1Score = 0;
        p2Score = 0;
        waiting = false;
        gameOver = false;

        showRound();
    }

    private void showRound() {
        waiting = false;

        setup("NUMBER RUSH");

        info.setText("PLAYER " + player + " • ROUND " + round + "/5");
        updateScore();

        if (mode == 1) {
            playMemory();
        } else if (mode == 2) {
            playMath();
        } else {
            playHunt();
        }
    }

    // MODE 1: NUMBER MEMORY

    private void playMemory() {
        memoryNumber = 10 + random.nextInt(90);

        question.setText(
                "MEMORIZE THIS NUMBER\n\n" + memoryNumber
        );

        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        b3.setVisibility(View.GONE);
        action.setVisibility(View.GONE);

        final LinearLayout thisRoot = root;

        timer = new CountDownTimer(2500, 2500) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (root != thisRoot) return;

                question.setText("Enter the number you remember");

                answerInput = new EditText(activity);
                answerInput.setSingleLine(true);
                answerInput.setTextColor(Color.WHITE);
                answerInput.setHintTextColor(Color.LTGRAY);
                answerInput.setHint("Enter number");
                answerInput.setInputType(
                        InputType.TYPE_CLASS_NUMBER
                );

                root.addView(
                        answerInput,
                        root.indexOfChild(action)
                );

                action.setVisibility(View.VISIBLE);
                action.setText("SUBMIT");

                action.setOnClickListener(v -> {
                    if (waiting) return;

                    String value = answerInput.getText()
                            .toString().trim();

                    if (value.isEmpty()) {
                        answerInput.setError("Enter a number");
                        return;
                    }

                    try {
                        int answer = Integer.parseInt(value);
                        award(answer == memoryNumber);
                    } catch (NumberFormatException e) {
                        answerInput.setError("Invalid number");
                    }
                });
            }
        }.start();
    }

    // MODE 2: FAST CALCULATION

    private void playMath() {
        int a = 2 + random.nextInt(18);
        int b = 2 + random.nextInt(18);
        int operation = random.nextInt(3);
        String expression;

        if (operation == 0) {
            correctAnswer = a + b;
            expression = a + " + " + b + " = ?";
        } else if (operation == 1) {
            if (a < b) {
                int temp = a;
                a = b;
                b = temp;
            }

            correctAnswer = a - b;
            expression = a + " - " + b + " = ?";
        } else {
            a = 2 + random.nextInt(10);
            b = 2 + random.nextInt(10);
            correctAnswer = a * b;
            expression = a + " x " + b + " = ?";
        }

        question.setText(expression);

        List<Integer> options = new ArrayList<>();
        options.add(correctAnswer);

        while (options.size() < 3) {
            int option = Math.max(
                    0,
                    correctAnswer + random.nextInt(11) - 5
            );

            if (!options.contains(option)) {
                options.add(option);
            }
        }

        Collections.shuffle(options);

        b1.setText(String.valueOf(options.get(0)));
        b2.setText(String.valueOf(options.get(1)));
        b3.setText(String.valueOf(options.get(2)));

        b1.setOnClickListener(v ->
                award(Integer.parseInt(b1.getText().toString())
                        == correctAnswer));

        b2.setOnClickListener(v ->
                award(Integer.parseInt(b2.getText().toString())
                        == correctAnswer));

        b3.setOnClickListener(v ->
                award(Integer.parseInt(b3.getText().toString())
                        == correctAnswer));
    }

    // MODE 3: NUMBER HUNT

    private void playHunt() {
        huntTarget = 1;

        question.setText(
                "TAP THE NUMBERS IN ORDER\n\n1 → 2 → 3"
        );

        List<Integer> numbers = new ArrayList<>();
        numbers.add(1);
        numbers.add(2);
        numbers.add(3);

        Collections.shuffle(numbers);

        b1.setText(String.valueOf(numbers.get(0)));
        b2.setText(String.valueOf(numbers.get(1)));
        b3.setText(String.valueOf(numbers.get(2)));

        b1.setEnabled(true);
        b2.setEnabled(true);
        b3.setEnabled(true);

        b1.setOnClickListener(v -> huntTap(b1));
        b2.setOnClickListener(v -> huntTap(b2));
        b3.setOnClickListener(v -> huntTap(b3));

        action.setVisibility(View.GONE);
    }

    private void huntTap(Button button) {
        if (waiting || gameOver) return;

        int tapped = Integer.parseInt(
                button.getText().toString()
        );

        if (tapped == huntTarget) {
            button.setEnabled(false);
            button.setText("✓");
            huntTarget++;

            if (huntTarget > 3) {
                award(true);
            } else {
                info.setText(
                        "PLAYER " + player +
                        " • FIND NUMBER " + huntTarget
                );
            }
        } else {
            award(false);
        }
    }

    // SCORING AND TURN SYSTEM

    private void award(boolean correct) {
        if (waiting || gameOver) return;

        waiting = true;

        b1.setEnabled(false);
        b2.setEnabled(false);
        b3.setEnabled(false);

        if (correct) {
            if (player == 1) {
                p1Score += 10;
            } else {
                p2Score += 10;
            }

            question.setText("CORRECT! +10 POINTS");
        } else {
            question.setText("WRONG! +0 POINTS");
        }

        updateScore();

        final LinearLayout thisRoot = root;

        timer = new CountDownTimer(1200, 1200) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (root != thisRoot) return;

                waiting = false;
                nextTurn();
            }
        }.start();
    }

    private void nextTurn() {
        if (player == 1) {
            player = 2;
            showRound();
            return;
        }

        player = 1;
        round++;

        if (round > 5) {
            finishMatch();
        } else {
            showRound();
        }
    }

    private void finishMatch() {
        gameOver = true;
        waiting = false;

        setup("MATCH OVER");

        if (p1Score > p2Score) {
            question.setText("PLAYER 1 WINS!");
        } else if (p2Score > p1Score) {
            question.setText("PLAYER 2 WINS!");
        } else {
            question.setText("MATCH DRAW!");
        }

        info.setText("FINAL SCORE");
        updateScore();

        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        b3.setVisibility(View.GONE);

        action.setVisibility(View.VISIBLE);
        action.setText("PLAY AGAIN");

        action.setOnClickListener(v -> showMenu());
    }

    private void updateScore() {
        if (score != null) {
            score.setText(
                    "P1: " + p1Score +
                    "       P2: " + p2Score
            );
        }
    }
  }
