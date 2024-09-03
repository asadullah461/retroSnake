package com.asadullah.retrosnake.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.asadullah.retrosnake.R;
import com.asadullah.retrosnake.classes.SnakePoints;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class GameActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private Bitmap snakeHeadBitmap; // Bitmap for the snake's head

    // List of snake points/length of snake
    private List<SnakePoints> snakePointsList = new ArrayList<>();

    // Surface holder to draw snake on surface's canvas
    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView;
    private MediaPlayer btnClickSound, eatPointSound,gameOverSound;
    private TextView scoreTv;

    private ImageButton top, right, left, bottom;

    // Snake moving position.. value must be "right", "left", "top", "bottom"
    // By default, it will be "right"
    private String movingPosition = "right";

    // Score
    private int score = 0;

    // Snake size / point size
    private static final int pointSize = 28;

    // Default snake tail length
    private static final int defaultTailPoints = 3;

    // Snake color
    private static final int snakeColor = Color.YELLOW;

    // Snake moving speed, value must lie between 1..1000
    private static final int snakeMovingSpeed = 800;

    // Random point position coordinates on the surfaceview
    private int positionY, positionX;

    // Timer to move snake / change snake position after specific time (snakeMovingSpeed)
    private Timer timer;

    // Canvas to draw snake and show on surface view
    private Canvas canvas = null;

    // Point color / single point color of the snake
    private Paint pointColor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Load the snake head image bitmap
        snakeHeadBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.snakess);


        // Hide system UI for full-screen mode
        hideSystemUI();

        top = findViewById(R.id.topBtn);
        right = findViewById(R.id.rightBtn);
        left = findViewById(R.id.leftBtn);
        bottom = findViewById(R.id.bottomBtn);

        scoreTv = findViewById(R.id.scoreTv);
        btnClickSound = MediaPlayer.create(this, R.raw.btn_click); // Sound for button click
        eatPointSound = MediaPlayer.create(this, R.raw.eat_point); // Sound for eating point
        gameOverSound = MediaPlayer.create(this, R.raw.game_over); // game over sound
        surfaceView = findViewById(R.id.surface_view);

        // Adding callback to surfaceview
        surfaceView.getHolder().addCallback(this);
        surfaceView.setFocusable(true);

        // Setting up button click listeners to change snake direction
        top.setOnClickListener(v -> {
            if (!movingPosition.equals("bottom")) {
                movingPosition = "top";
                playButtonClickSound();
            }
        });

        right.setOnClickListener(v -> {
            if (!movingPosition.equals("left")) {
                movingPosition = "right";
                playButtonClickSound();
            }
        });

        left.setOnClickListener(v -> {
            if (!movingPosition.equals("right")) {
                movingPosition = "left";
                playButtonClickSound();
            }
        });

        bottom.setOnClickListener(v -> {
            if (!movingPosition.equals("top")) {
                movingPosition = "bottom";
                playButtonClickSound();
            }
        });
    }

    private void hideSystemUI() {
        // Enables sticky immersive mode to hide the navigation and status bar
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        // When surface is created, then get surfaceHolder from it and assign to surfaceHolder
        this.surfaceHolder = holder;

        // Initialize data for snake / surfaceview
        init();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        // Not used
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        // Not used
    }

    private void init() {
        // Clear snake points / snake length
        snakePointsList.clear();

        // Set default score to 0
        scoreTv.setText("0");

        // Make score = 0
        score = 0;

        // Setting default moving position
        movingPosition = "right";

        // Default snake starting position on the screen
        int startPositionX = pointSize * defaultTailPoints;

        // Making snake's default length / points
        for (int i = 0; i < defaultTailPoints; i++) {
            // Adding points to snake's tail
            SnakePoints snakePoints = new SnakePoints(startPositionX, pointSize);
            snakePointsList.add(snakePoints);

            // Increasing value for next point as snake's tail
            startPositionX = startPositionX - (pointSize * 2);
        }

        // Add random point on the screen to be eaten by the snake
        addPoint();

        // Start moving snake / start game
        moveSnake();
    }

    private void addPoint() {
        // Getting surface view width and height to add point on the screen to be eaten by snake
        int surfaceWidth = surfaceView.getWidth() - (pointSize * 2);
        int surfaceHeight = surfaceView.getHeight() - (pointSize * 2);

        int randomXPosition = new Random().nextInt(surfaceWidth / pointSize);
        int randomYPosition = new Random().nextInt(surfaceHeight / pointSize);

        // Check if randomXPosition is even or odd. We need only even numbers.
        if ((randomXPosition % 2) != 0) {
            randomXPosition = randomXPosition + 1;
        }
        if ((randomYPosition % 2) != 0) {
            randomYPosition = randomYPosition + 1;
        }

        positionX = (pointSize * randomXPosition) + pointSize;
        positionY = (pointSize * randomYPosition) + pointSize;
    }

    private void moveSnake() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Getting head position
                int headPositionX = snakePointsList.get(0).getPositionX();
                int headPositionY = snakePointsList.get(0).getPositionY();

                // Check if snake ate a point
                if (headPositionX == positionX && headPositionY == positionY) {
                    // Play sound when snake eats a point
                    playEatPointSound();

                    // Grow snake after eating a point
                    growSnake();

                    // Add another random point on the screen
                    addPoint();
                }

                // Checking of snake's direction
                switch (movingPosition) {
                    case "right":
                        snakePointsList.get(0).setPositionX(headPositionX + (pointSize * 2));
                        break;
                    case "left":
                        snakePointsList.get(0).setPositionX(headPositionX - (pointSize * 2));
                        break;
                    case "top":
                        snakePointsList.get(0).setPositionY(headPositionY - (pointSize * 2));
                        break;
                    case "bottom":
                        snakePointsList.get(0).setPositionY(headPositionY + (pointSize * 2));
                        break;
                }

                // Check if game is over, whether snake touches the edges or itself
                if (checkGameOver(snakePointsList.get(0).getPositionX(), snakePointsList.get(0).getPositionY())) {
                    // Stop timer / stop moving snake
                    timer.purge();
                    timer.cancel();

                    // Show game over dialog
                    runOnUiThread(() -> showGameOverDialog());
//                                    new AlertDialog.Builder(GameActivity.this)
//                            .setMessage("Your score = " + score)
//                            .setTitle("Game Over")
//                            .setCancelable(false)
//                            .setPositiveButton("Start Again", (dialog, which) -> init())
//                            .show());
                } else {
                    // Lock canvas on surfaceHolder to draw on it
                    canvas = surfaceHolder.lockCanvas();

                    // Clear canvas with white color
                    canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);

                    // Draw snake head
                    canvas.drawCircle(snakePointsList.get(0).getPositionX(), snakePointsList.get(0).getPositionY(),
                            pointSize, createPointColor());

                    canvas.drawBitmap(snakeHeadBitmap, headPositionX - pointSize, headPositionY - pointSize, null);

                    // Draw random point circle on the surface to be eaten by the snake
                    canvas.drawCircle(positionX, positionY, pointSize, createPointColor());

                    // Draw snake body
                    int prevX = headPositionX;
                    int prevY = headPositionY;
                    for (int i = 1; i < snakePointsList.size(); i++) {
                        int tempX = snakePointsList.get(i).getPositionX();
                        int tempY = snakePointsList.get(i).getPositionY();
                        snakePointsList.get(i).setPositionX(prevX);
                        snakePointsList.get(i).setPositionY(prevY);
                        canvas.drawCircle(snakePointsList.get(i).getPositionX(), snakePointsList.get(i).getPositionY(),
                                pointSize, createPointColor());
                        prevX = tempX;
                        prevY = tempY;
                    }

                    // Unlock canvas to draw on surfaceView
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }, 1000 - snakeMovingSpeed, 1000 - snakeMovingSpeed);
    }

    private void growSnake() {
        // Create new snake point
        SnakePoints snakePoints = new SnakePoints(-pointSize, -pointSize); // Initialize off-screen
        snakePointsList.add(snakePoints);

        // Update score
        score += 1;
        runOnUiThread(() -> scoreTv.setText(String.valueOf(score)));
    }

    private boolean checkGameOver(int headX, int headY) {
        // Check if snake hits the wall
        if (headX < 0 || headY < 0 || headX >= surfaceView.getWidth() || headY >= surfaceView.getHeight()) {
            return true;
        }

        // Check if snake hits itself
        for (int i = 1; i < snakePointsList.size(); i++) {
            if (snakePointsList.get(i).getPositionX() == headX && snakePointsList.get(i).getPositionY() == headY) {
                return true;
            }
        }

        return false;
    }

    private void showGameOverDialog() {
        // Inflate the custom layout for the game over dialog
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogue_game_over, null);

        // Set up the score text in the dialog
        TextView gameOverMessage = dialogView.findViewById(R.id.gameOverMessage);
        gameOverSound.start();
        gameOverMessage.setText("Your score = " + score);

        // Create and show the game over dialog
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("Game Over")
                .setCancelable(false)
                .setPositiveButton("Start Again", (dialog, which) -> init())
                .show();
    }

    private void playButtonClickSound() {
        if (btnClickSound != null) {
            btnClickSound.start();
        }
    }

    private void playEatPointSound() {
        if (eatPointSound != null) {
            eatPointSound.start();
        }
    }

    private Paint createPointColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(snakeColor);
        }
        return pointColor;
    }
}
