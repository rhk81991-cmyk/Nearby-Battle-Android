package com.nox.nearbybattle;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class NumberRushGame {

    private final MainActivity activity;
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private LinearLayout root;
    private TextView title, info, question, score, timerText;
    private Button b1, b2, b3, action, back;
    private EditText answerInput;
    private GridLayout huntGrid;
    private CountDownTimer timer;

    private int mode = 0;
    private int player = 1;
    private int round = 1;

    private int p1Score = 0;
    private int p2Score = 0;

    private String memoryNumber = "";
    private int correctAnswer;
    private int huntTarget = 1;

    private long huntStartTime;
    private long p1HuntTime;
    private long p2HuntTime;

    private boolean[] huntDone = new boolean[100];
    private final List<Integer> huntNumbers = new ArrayList<>();

    private boolean waiting = false;
    private boolean gameOver = false;
    private boolean huntRunning = false;

    private Runnable huntTicker;
    private Runnable pendingTransition;

    private final int backgroundColor = Color.rgb(18, 22, 38);
    private final int normalBubbleColor = Color.rgb(65, 100, 170);
    private final int correctBubbleColor = Color.rgb(35, 160, 95);
    private final int wrongBubbleColor = Color.rgb(220, 55, 65);

    public NumberRushGame(MainActivity activity) {
        this.activity = activity;
    }

    public void start() {
        showMenu();
    }

    private int dp(float value) {
        return (int) (value * activity.getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void cancelHuntTicker() {
        huntRunning = false;

        if (huntTicker != null) {
            handler.removeCallbacks(huntTicker);
            huntTicker = null;
        }
    }

    private void cancelPendingTransition() {
        if (pendingTransition != null) {
            handler.removeCallbacks(pendingTransition);
            pendingTransition = null;
        }
    }

    private void stopAllTimers() {
        cancelTimer();
        cancelHuntTicker();
        cancelPendingTransition();
    }

    private void setup(String heading) {
        stopAllTimers();

        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(12), dp(6), dp(12), dp(6));
        root.setBackgroundColor(backgroundColor);

        title = makeText(heading, 25);
        info = makeText("", 15);
        score = makeText("", 15);
        question = makeText("", 21);
        timerText = makeText("", 19);

        root.addView(title);
        root.addView(info);
        root.addView(score);
        root.addView(question);
        root.addView(timerText);

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
            stopAllTimers();
            activity.showGamesScreen();
        });

        activity.setContentView(root);
        updateScore();
    }

    private TextView makeText(String value, int size) {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(Color.WHITE);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(3), dp(3), dp(3), dp(3));
        return text;
    }

    private Button makeButton(String value) {
        Button button = new Button(activity);
        button.setText(value);
        button.setTextSize(14);
        button.setAllCaps(false);
        return button;
    }

    private void showMenu() {
        waiting = false;
        gameOver = false;
        mode = 0;

        setup("NUMBER RUSH");

        info.setText("Choose a game mode");
        question.setText("2 PLAYERS • SAME PHONE");
        timerText.setVisibility(View.GONE);

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

        stopAllTimers();

        player = 1;
        round = 1;
        p1Score = 0;
        p2Score = 0;
        p1HuntTime = 0;
        p2HuntTime = 0;

        waiting = false;
        gameOver = false;

        showRound();
    }

    private void showRound() {
        waiting = false;

        showRoundScreen();
    }

    private void showRoundScreen() {
        setup("NUMBER RUSH");

        info.setText("PLAYER " + player
                + (mode == 3 ? " • NUMBER HUNT" :
                " • ROUND " + round + "/5"));

        updateScore();

        if (mode == 1) {
            playMemory();
        } else if (mode == 2) {
            playMath();
        } else if (mode == 3) {
            playHunt();
        }
    }

    // =====================================
    // MODE 1: NUMBER MEMORY
    // =====================================

    private void playMemory() {
        memoryNumber = String.format(
                Locale.US, "%04d", random.nextInt(10000)
        );

        question.setText(
                "MEMORIZE THIS NUMBER\n\n" + memoryNumber
        );

        info.setText("Remember all 4 digits!");
        timerText.setVisibility(View.GONE);

        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        b3.setVisibility(View.GONE);
        action.setVisibility(View.GONE);

        final LinearLayout thisRoot = root;

        timer = new CountDownTimer(900, 900) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (root != thisRoot || gameOver) return;

                question.setText("Enter the 4-digit number");

                answerInput = new EditText(activity);
                answerInput.setSingleLine(true);
                answerInput.setTextColor(Color.WHITE);
                answerInput.setHintTextColor(Color.LTGRAY);
                answerInput.setHint("0000");
                answerInput.setTextSize(20);
                answerInput.setGravity(Gravity.CENTER);
                answerInput.setInputType(
                        InputType.TYPE_CLASS_NUMBER
                );

                answerInput.setFilters(
                        new InputFilter[]{
                                new InputFilter.LengthFilter(4)
                        }
                );

                root.addView(
                        answerInput,
                        root.indexOfChild(action)
                );

                action.setVisibility(View.VISIBLE);
                action.setText("SUBMIT");

                action.setOnClickListener(v -> {
                    if (waiting || gameOver) return;

                    String value = answerInput.getText()
                            .toString().trim();

                    if (value.length() != 4) {
                        answerInput.setError(
                                "Enter exactly 4 digits"
                        );
                        return;
                    }

                    award(value.equals(memoryNumber));
                });

                answerInput.requestFocus();
            }
        }.start();
    }

    // =====================================
    // MODE 2: FAST CALCULATION
    // =====================================

    private void playMath() {
        timerText.setVisibility(View.VISIBLE);

        int operation = random.nextInt(4);
        int a;
        int b;
        String expression;

        if (operation == 0) {
            // Addition
            a = 25 + random.nextInt(76);
            b = 15 + random.nextInt(66);

            correctAnswer = a + b;
            expression = a + " + " + b + " = ?";
        } else if (operation == 1) {
            // Subtraction
            a = 40 + random.nextInt(61);
            b = 10 + random.nextInt(30);

            if (b > a) {
                int temp = a;
                a = b;
                b = temp;
            }

            correctAnswer = a - b;
            expression = a + " - " + b + " = ?";
        } else if (operation == 2) {
            // Multiplication
            a = 6 + random.nextInt(10);
            b = 4 + random.nextInt(9);

            correctAnswer = a * b;
            expression = a + " × " + b + " = ?";
        } else {
            // Exact division
            b = 2 + random.nextInt(11);
            int quotient = 3 + random.nextInt(13);

            a = b * quotient;

            correctAnswer = quotient;
            expression = a + " ÷ " + b + " = ?";
        }

        question.setText(expression);

        List<Integer> options = new ArrayList<>();
        options.add(correctAnswer);

        while (options.size() < 3) {
            int difference = random.nextInt(21) - 10;

            if (difference == 0) {
                difference = 11;
            }

            int option = correctAnswer + difference;

            if (option >= 0 && !options.contains(option)) {
                options.add(option);
            }
        }

        Collections.shuffle(options);

        b1.setText(String.valueOf(options.get(0)));
        b2.setText(String.valueOf(options.get(1)));
        b3.setText(String.valueOf(options.get(2)));

        b1.setEnabled(true);
        b2.setEnabled(true);
        b3.setEnabled(true);

        b1.setOnClickListener(v ->
                submitMathAnswer(b1));

        b2.setOnClickListener(v ->
                submitMathAnswer(b2));

        b3.setOnClickListener(v ->
                submitMathAnswer(b3));

        info.setText("Solve before time runs out!");

        final LinearLayout thisRoot = root;

        timer = new CountDownTimer(3000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (root != thisRoot || waiting) return;

                timerText.setText(
                        String.format(
                                Locale.US,
                                "TIME: %.1f s",
                                millisUntilFinished / 1000.0
                        )
                );
            }

            @Override
            public void onFinish() {
                if (root != thisRoot || waiting) return;

                timerText.setText("TIME UP!");
                award(false);
            }
        }.start();
    }

    private void submitMathAnswer(Button button) {
        if (waiting || gameOver) return;

        int answer = Integer.parseInt(
                button.getText().toString()
        );

        award(answer == correctAnswer);
    }

    // =====================================
    // MODE 3: NUMBER HUNT
    // =====================================

    private void playHunt() {
        huntTarget = 1;
        huntDone = new boolean[100];

        huntNumbers.clear();

        for (int i = 1; i <= 99; i++) {
            huntNumbers.add(i);
        }

        Collections.shuffle(huntNumbers);

        question.setText("TAP IN ORDER: 1 → 99");
        info.setText("Find number 1 to begin");

        timerText.setVisibility(View.VISIBLE);
        timerText.setText("TIME: 00:00.0");

        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        b3.setVisibility(View.GONE);
        action.setVisibility(View.GONE);

        huntGrid = new GridLayout(activity);
        huntGrid.setColumnCount(9);
        huntGrid.setUseDefaultMargins(false);
        huntGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        root.addView(
                huntGrid,
                root.indexOfChild(back)
        );

        renderHuntGrid();

        huntStartTime = SystemClock.elapsedRealtime();
        huntRunning = true;

        huntTicker = new Runnable() {
            @Override
            public void run() {
                if (!huntRunning || gameOver) return;

                long elapsed =
                        SystemClock.elapsedRealtime()
                                - huntStartTime;

                timerText.setText(
                        "TIME: " + formatTime(elapsed)
                );

                handler.postDelayed(this, 100);
            }
        };

        handler.post(huntTicker);
    }

    private String formatTime(long millis) {
        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;
        long tenths = (millis % 1000) / 100;

        return String.format(
                Locale.US,
                "%02d:%02d.%d",
                minutes,
                seconds,
                tenths
        );
    }

    private GradientDrawable bubbleBackground(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        shape.setStroke(dp(1), Color.rgb(220, 225, 240));

        return shape;
    }

    private void renderHuntGrid() {
        if (huntGrid == null) return;

        huntGrid.removeAllViews();

        for (int number : huntNumbers) {
            Button bubble = new Button(activity);

            bubble.setText(String.valueOf(number));
            bubble.setTextSize(11);
            bubble.setTextColor(Color.WHITE);
            bubble.setAllCaps(false);
            bubble.setPadding(0, 0, 0, 0);
            bubble.setMinWidth(0);
            bubble.setMinimumWidth(0);
            bubble.setMinHeight(0);
            bubble.setMinimumHeight(0);
            bubble.setIncludeFontPadding(false);

            int color = huntDone[number]
                    ? correctBubbleColor
                    : normalBubbleColor;

            bubble.setBackground(bubbleBackground(color));
            bubble.setTag(number);

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width = 0;
            params.height = dp(32);
            params.columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED, 1f
            );

            params.setMargins(
                    dp(1), dp(1), dp(1), dp(1)
            );

            huntGrid.addView(bubble, params);

            bubble.setOnClickListener(v ->
                    huntTap(bubble));
        }
    }

    private void huntTap(Button bubble) {
        if (waiting || gameOver || !huntRunning) return;

        int tapped = (int) bubble.getTag();

        if (tapped == huntTarget) {
            huntDone[tapped] = true;
            huntTarget++;

            bubble.setBackground(
                    bubbleBackground(correctBubbleColor)
            );

            info.setText(
                    "PLAYER " + player
                            + " • NEXT: "
                            + (huntTarget <= 99
                            ? huntTarget : "FINISHED")
            );

            if (huntTarget > 99) {
                finishHuntPlayer();
                return;
            }

            // Shuffle after every 10 correct taps.
            // Progress and timer remain unchanged.
            if (tapped % 10 == 0) {
                Collections.shuffle(huntNumbers);
                renderHuntGrid();
            }

        } else {
            // Wrong bubble flashes red for one second.
            bubble.setBackground(
                    bubbleBackground(wrongBubbleColor)
            );

            final Button wrongBubble = bubble;
            final int number = tapped;

            handler.postDelayed(() -> {
                if (huntGrid == null
                        || wrongBubble.getParent() != huntGrid) {
                    return;
                }

                int restoreColor = huntDone[number]
                        ? correctBubbleColor
                        : normalBubbleColor;

                wrongBubble.setBackground(
                        bubbleBackground(restoreColor)
                );
            }, 1000);
        }
    }

    private void finishHuntPlayer() {
        if (!huntRunning || gameOver) return;

        long elapsed =
                SystemClock.elapsedRealtime() - huntStartTime;

        cancelHuntTicker();

        if (player == 1) {
            p1HuntTime = elapsed;
        } else {
            p2HuntTime = elapsed;
        }

        timerText.setText("YOUR TIME: " + formatTime(elapsed));

        question.setText(
                "PLAYER " + player + " FINISHED!"
        );

        info.setText("Completed all numbers from 1 to 99");

        if (player == 1) {
            waiting = true;

            pendingTransition = () -> {
                pendingTransition = null;

                if (gameOver) return;

                player = 2;
                waiting = false;
                showRound();
            };

            handler.postDelayed(pendingTransition, 1500);

        } else {
            finishHuntMatch();
        }
    }

    private void finishHuntMatch() {
        gameOver = true;
        waiting = false;

        setup("NUMBER HUNT RESULTS");

        String p1Time = formatTime(p1HuntTime);
        String p2Time = formatTime(p2HuntTime);

        info.setText(
                "PLAYER 1: " + p1Time
                        + "\nPLAYER 2: " + p2Time
        );

        if (p1HuntTime < p2HuntTime) {
            question.setText("PLAYER 1 WINS!");
        } else if (p2HuntTime < p1HuntTime) {
            question.setText("PLAYER 2 WINS!");
        } else {
            question.setText("IT'S A DRAW!");
        }

        timerText.setVisibility(View.GONE);

        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        b3.setVisibility(View.GONE);

        action.setVisibility(View.VISIBLE);
        action.setText("PLAY AGAIN");
        action.setOnClickListener(v -> showMenu());

        updateScore();
    }

    // =====================================
    // SCORING: MEMORY AND CALCULATION
    // =====================================

    private void award(boolean correct) {
        if (waiting || gameOver) return;

        waiting = true;
        cancelTimer();

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
            question.setText("WRONG OR TIME UP! +0 POINTS");
        }

        updateScore();

        final LinearLayout thisRoot = root;

        timer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (root != thisRoot || gameOver) return;

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
        timerText.setVisibility(View.GONE);

        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        b3.setVisibility(View.GONE);

        action.setVisibility(View.VISIBLE);
        action.setText("PLAY AGAIN");

        action.setOnClickListener(v -> showMenu());

        updateScore();
    }

    private void updateScore() {
        if (score != null) {
            score.setText(
                    "P1: " + p1Score
                            + "       P2: " + p2Score
            );
        }
    }
}
