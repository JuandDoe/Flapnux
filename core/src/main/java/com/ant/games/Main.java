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
    private ShapeRenderer shapeRenderer;

    // TEXTURES
    private Texture bottomRackServer;
    private Texture background;
    private Texture topRackServer;
    private Texture[] birds;

    // TEXTES
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

    // CROTTE
    private List<Crotte> crottes;
    private final String[] commandes = {"cd", "ls", "ip a", "sudo apt update", "sudo apt upgrade"};

    // HITBOXES
    private Rectangle topRackServerHitBox;
    private Rectangle bottomRackServerHitBox;
    private Rectangle birdHitBox;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
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
        crottes = new ArrayList<>();

        // HITBOXES
        topRackServerHitBox = new Rectangle();
        bottomRackServerHitBox = new Rectangle();
        birdHitBox = new Rectangle();
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

            // GAME OVER: si l'oiseau sort de l'écran (haut ou bas)
            if (birdY <= 0 || birdY + birds[flapState].getHeight() >= screenHeight) {
                gamestate = 2;
            }

            // HITBOXES
            float serverWidth = topRackServer.getWidth() * 0.5f;  // Réduction de 50 % de la largeur
            float serverHeight = screenHeight / 2 - gap / 2 + 50; // Hauteur conservée

            // Top Server Hitbox
            topRackServerHitBox.set(
                tubeX + (topRackServer.getWidth() - serverWidth) / 2, // Centrer horizontalement
                (screenHeight / 2 + gap / 2) + tubeOffSet,
                serverWidth,
                serverHeight
            );

            // Bottom Server Hitbox
            bottomRackServerHitBox.set(
                tubeX + (bottomRackServer.getWidth() - serverWidth) / 2, // Centrer horizontalement
                0,
                serverWidth,
                (screenHeight / 2 - gap / 2) + tubeOffSet // Hauteur conservée
            );

            // Oiseau Hitbox
            birdHitBox.set(
                screenWidth / 2 - (float) birds[flapState].getWidth() / 2,
                birdY,
                birds[flapState].getWidth(),
                birds[flapState].getHeight()
            );

            // Vérification des collisions
            if (birdHitBox.overlaps(topRackServerHitBox) || birdHitBox.overlaps(bottomRackServerHitBox)) {
                gamestate = 2; // Game Over si l'oiseau touche un serveur
            }
        } else if (gamestate == 0 && Gdx.input.justTouched()) {
            gamestate = 1;
            velocity = -30;
        } else if (gamestate == 2 && Gdx.input.justTouched()) {
            resetGame();
        }

        // AFFICHAGE
        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);
        if (tubeIsRed) batch.setColor(1, 0, 0, 0.8f);
        batch.draw(bottomRackServer, tubeX, 0, bottomRackServer.getWidth(), (screenHeight / 2 - gap / 2) + tubeOffSet);
        batch.draw(topRackServer, tubeX, (screenHeight / 2 + gap / 2) + tubeOffSet, topRackServer.getWidth(), screenHeight - (screenHeight / 2 + gap / 2) - tubeOffSet);
        batch.setColor(1, 1, 1, 1);
        batch.draw(birds[flapState], screenWidth / 2 - birds[flapState].getWidth() / 2, birdY);

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
            font.draw(batch, "GAME OVER!", screenWidth / 2 - 200, screenHeight / 2 + 100);
            font.getData().setScale(4);
            font.draw(batch, "SCORE " + score, screenWidth / 2 - 100, screenHeight / 2);
            font.draw(batch, "Tap to Restart", screenWidth / 2 - 150, screenHeight / 2 - 100);
        }

        batch.end();

        // Dessiner les hitboxes
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(
            topRackServerHitBox.x,
            topRackServerHitBox.y,
            topRackServerHitBox.width,
            topRackServerHitBox.height
        );
        shapeRenderer.rect(
            bottomRackServerHitBox.x,
            bottomRackServerHitBox.y,
            bottomRackServerHitBox.width,
            bottomRackServerHitBox.height
        );

        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.rect(
            birdHitBox.x,
            birdHitBox.y,
            birdHitBox.width,
            birdHitBox.height
        );
        shapeRenderer.end();
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
