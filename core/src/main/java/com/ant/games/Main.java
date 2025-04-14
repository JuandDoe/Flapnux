package com.ant.games;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture background;
    private Texture topRackServer;
    private Rectangle topRackServerHitBox;

    private Texture bottomRackServer;
    private Rectangle bottomRackServerHitBox;
    private Rectangle birdHitBox;
    private Texture[] birds;
    private BitmapFont font;
    private int score = 0;
    private int flapState = 0;
    private float birdY = 0;
    private float velocity = 0;
    private int gamestate = 0;
    private float gap = 500;
    private float maximumOffSet;
    private float tubeOffSet;
    private float tubeX;
    private float tubeVelocity = 4;
    private boolean tubeIsRed = false;
    private Random randomGenerator;

    private List<Crotte> crottes;
    private final String[] commandes = {"cd", "ls", "ip a", "sudo apt update", "sudo apt upgrade"};

    @Override
    public void create() {
        batch = new SpriteBatch();
        background = new Texture("background.png");
        topRackServer = new Texture("rackserver_up.png");
        bottomRackServer = new Texture("rackserver_down.png");
        birds = new Texture[2];
        birds[0] = new Texture("pinguin_up.png");
        birds[1] = new Texture("pinguin_down.png");

        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(4);

        birdY = (float) Gdx.graphics.getHeight() / 2 - (float) birds[0].getHeight() / 2;
        tubeX = Gdx.graphics.getWidth();
        maximumOffSet = ((float) Gdx.graphics.getHeight() / 2 - gap / 2) - 100;
        randomGenerator = new Random();
        topRackServerHitBox = new Rectangle();
        bottomRackServerHitBox = new Rectangle();
        birdHitBox = new Rectangle();

        crottes = new ArrayList<>();

        float width = birds[1].getWidth();
        float height = birds[1].getHeight();

    }

    @Override
    public void render() {

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        if (gamestate == 1) {
            if (Gdx.input.justTouched()) {
                velocity = -30;
                crottes.add(new Crotte(screenWidth / 2 - 20, birdY, commandes[randomGenerator.nextInt(commandes.length)]));
            }
            velocity += 2;
            birdY -= velocity;
            tubeX -= tubeVelocity;

            if (tubeX < -bottomRackServer.getWidth()) {
                tubeX = screenWidth;
                tubeOffSet = (randomGenerator.nextFloat() * maximumOffSet * 2) - maximumOffSet;
                tubeIsRed = randomGenerator.nextFloat() < 0.2f;
                score += tubeIsRed ? 2 : 1;
            }

            birdHitBox.set(screenWidth / 2 - (float) birds[flapState].getWidth() / 2, birdY, birds[flapState].getWidth(), birds[flapState].getHeight());
            topRackServerHitBox.set(tubeX, (screenHeight / 2 + gap / 2) + tubeOffSet, topRackServer.getWidth(), screenHeight - (screenHeight / 2 + gap / 2) - tubeOffSet);
            bottomRackServerHitBox.set(tubeX, 0, bottomRackServer.getWidth(), (screenHeight / 2 - gap / 2) + tubeOffSet);

            if (birdY <= 0 || birdY >= screenHeight ||  birdHitBox.overlaps(topRackServerHitBox) || birdHitBox.overlaps(bottomRackServerHitBox)) {
                gamestate = 2;
            }
        } else if (gamestate == 0 && Gdx.input.justTouched()) {
            gamestate = 1;
            velocity = -30;
        } else if (gamestate == 2 && Gdx.input.justTouched()) {
            resetGame();
        }

        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);
        if (tubeIsRed) batch.setColor(1, 0, 0, 0.8f);
        batch.draw(bottomRackServer, tubeX, 0, bottomRackServer.getWidth(), (screenHeight / 2 - gap / 2) + tubeOffSet);
        batch.draw(topRackServer, tubeX, (screenHeight / 2 + gap / 2) + tubeOffSet, topRackServer.getWidth(), screenHeight - (screenHeight / 2 + gap / 2) - tubeOffSet);
        batch.setColor(1, 1, 1, 1);
        batch.draw(birds[flapState], screenWidth / 2 - (float) birds[flapState].getWidth() / 2, birdY);

        font.setColor(Color.WHITE);
        font.draw(batch, "Score: " + score, 100, screenHeight - 50);

        for (int i = crottes.size() - 1; i >= 0; i--) {
            Crotte c = crottes.get(i);
            c.update();
            c.draw(batch, font);
            if (c.y < -50) crottes.remove(i);
        }

        if (gamestate == 2) {
            font.getData().setScale(6);
            font.setColor(Color.WHITE);
            font.draw(batch, "GAME OVER!", screenWidth / 2 - 200, screenHeight / 2 + 100);
            font.getData().setScale(4);
            font.draw(batch, "SCORE " + score, screenWidth / 2 - 100, screenHeight / 2);
            font.draw(batch, "Tap to Restart", screenWidth / 2 - 150, screenHeight / 2 - 100);
        }
        batch.end();
    }

    private void resetGame() {
        gamestate = 0;
        score = 0;
        velocity = 0;
        birdY = (float) Gdx.graphics.getHeight() / 2 - (float) birds[0].getHeight() / 2;
        tubeX = Gdx.graphics.getWidth();
        crottes.clear();
    }

    private static class Crotte {
        float x, y, speedX = -3, speedY = -2;
        String text;
        Color textColor;

        Crotte(float x, float y, String text) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.textColor = new Color(0.4f, 0.2f, 0.0f, 1);
        }

        void update() {
            x += speedX;
            y += speedY;
        }

        void draw(SpriteBatch batch, BitmapFont font) {
            font.setColor(textColor);
            font.draw(batch, text, x, y);
        }
    }
}
