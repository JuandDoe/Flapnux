package com.ant.games;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.io.*;
import java.util.*;

public class Main extends ApplicationAdapter implements TextInputListener {

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
    private int gamestate = -1;  // -1 = menu initial, 0 = waiting, 1 = playing, 2 = game over, 3 = name prompt, 4 = stats
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

    // USER DATA
    private String userName = "Player1"; // Pseudo par défaut
    private int bestScore = 0;
    private boolean isFirstTimeUser = false;
    private float welcomeMessageTimer = 0;
    private boolean showWelcomeMessage = false;
    private boolean namePopupShown = false;

    // SCORES MAP
    private Map<String, List<Integer>> userScores;

    // STATS DISPLAY
    private int currentStatsPage = 0;
    private int maxStatsPerPage = 10;
    private List<String> statsSummaries;

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
        userScores = new HashMap<>();
        statsSummaries = new ArrayList<>();

        // HITBOXES
        topRackServerHitBox = new Rectangle();
        bottomRackServerHitBox = new Rectangle();
        birdHitBox = new Rectangle();

        // Charger les données utilisateur et scores
        loadUserData();

        // Commencer par le menu initial splitté
        gamestate = -1;

        // Si c'est un nouveau joueur, préparer à demander le nom quand il choisira "Jouer"
        // sinon il verra le menu splitté d'abord
        isFirstTimeUser = isFirstTimeUser;
    }

    @Override
    public void render() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Mise à jour du timer du message de bienvenue
        if (showWelcomeMessage) {
            welcomeMessageTimer -= Gdx.graphics.getDeltaTime();
            if (welcomeMessageTimer <= 0) {
                showWelcomeMessage = false;
            }
        }

        // MENU INITIAL SPLITTÉ
        if (gamestate == -1) {
            renderSplitMenu(screenWidth, screenHeight);
            return;
        }

        // STATISTIQUES
        if (gamestate == 4) {
            renderStatsScreen(screenWidth, screenHeight);
            return;
        }

        // Pour le mode demande de nom (affiche un message, puis attend que l'utilisateur clique)
        if (gamestate == 3) {
            batch.begin();
            batch.draw(background, 0, 0, screenWidth, screenHeight);

            font.getData().setScale(4);
            font.setColor(Color.WHITE);
            font.draw(batch, "Tap to enter your name", screenWidth / 2 - 250, screenHeight / 2);

            batch.end();

            // Attendre un clic pour afficher la boîte de dialogue de saisie de nom
            if (Gdx.input.justTouched() && !namePopupShown) {
                namePopupShown = true;
                Gdx.input.getTextInput(this, "Enter Your Name", "Player1", "");
            }

            return;
        }

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
                saveScore();
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

            // Oiseau Hitbox - réduite de 10% en aire (environ 5% en largeur et 5% en hauteur)
            // Pour réduire l'aire de 10%, on réduit chaque dimension d'environ 5% (car 0.95*0.95 ≈ 0.9)
            float reductionFactor = 0.95f;
            float birdWidth = birds[flapState].getWidth() * reductionFactor;
            float birdHeight = birds[flapState].getHeight() * reductionFactor;

            // Centrer la hitbox réduite
            float offsetX = (birds[flapState].getWidth() - birdWidth) / 2;
            float offsetY = (birds[flapState].getHeight() - birdHeight) / 2;

            birdHitBox.set(
                screenWidth / 2 - (float) birds[flapState].getWidth() / 2 + offsetX,
                birdY + offsetY,
                birdWidth,
                birdHeight
            );

            // Vérification des collisions
            if (birdHitBox.overlaps(topRackServerHitBox) || birdHitBox.overlaps(bottomRackServerHitBox)) {
                gamestate = 2; // Game Over si l'oiseau touche un serveur
                saveScore();
            }
        } else if (gamestate == 0 && Gdx.input.justTouched()) {
            // Démarrer le jeu quand l'utilisateur clique
            gamestate = 1;
            velocity = -30;
        } else if (gamestate == 2 && Gdx.input.justTouched()) {
            // Vérifier si le bouton RESET DATA a été cliqué
            float buttonWidth = 300;
            float buttonHeight = 80;
            float buttonX = screenWidth / 2 - buttonWidth / 2;
            float buttonY = 50; // Même position que pour l'affichage

            // Si l'utilisateur clique sur le bouton RESET DATA
            if (Gdx.input.getX() >= buttonX && Gdx.input.getX() <= buttonX + buttonWidth &&
                screenHeight - Gdx.input.getY() >= buttonY && screenHeight - Gdx.input.getY() <= buttonY + buttonHeight) {
                // Réinitialiser toutes les données
                resetAllData();
            } else {
                // Vérifier si le bouton STATS a été cliqué
                float statsButtonX = screenWidth / 2 - buttonWidth / 2;
                float statsButtonY = 150; // Position au-dessus du bouton RESET DATA
                if (Gdx.input.getX() >= statsButtonX && Gdx.input.getX() <= statsButtonX + buttonWidth &&
                    screenHeight - Gdx.input.getY() >= statsButtonY && screenHeight - Gdx.input.getY() <= statsButtonY + buttonHeight) {
                    // Aller à l'écran des statistiques
                    gamestate = 4;
                    prepareStatsData();
                } else {
                    // Sinon, juste redémarrer la partie
                    resetGame();
                }
            }
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
        font.draw(batch, "Best: " + bestScore, 100, screenHeight - 100);
        font.draw(batch, "Player: " + userName, 100, screenHeight - 150);

        for (int i = crottes.size() - 1; i >= 0; i--) {
            Crotte c = crottes.get(i);
            c.update();
            c.draw(batch, font);
            if (c.y < -50) crottes.remove(i);
        }

        if (gamestate == 2) {
            // S'assurer que le texte est en blanc
            font.setColor(Color.WHITE);
            font.getData().setScale(6);
            font.draw(batch, "GAME OVER!", screenWidth / 2 - 200, screenHeight / 2 + 100);
            font.getData().setScale(4);
            font.draw(batch, "SCORE " + score, screenWidth / 2 - 100, screenHeight / 2);
            font.draw(batch, "Tap to Restart", screenWidth / 2 - 150, screenHeight / 2 - 100);

            // Dessiner un bouton STATS
            float buttonWidth = 300;
            float buttonHeight = 80;
            float buttonX = screenWidth / 2 - buttonWidth / 2;
            float buttonY = 150; // Position au-dessus du bouton RESET DATA

            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.BLUE);
            shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight);
            shapeRenderer.end();
            batch.begin();

            // Dessiner le texte du bouton en blanc
            font.setColor(Color.WHITE);
            font.draw(batch, "STATS", buttonX + 100, buttonY + 50);

            // Dessiner un bouton rouge RESET DATA beaucoup plus bas sur l'écran
            buttonY = 50; // À 50 pixels du bas de l'écran

            // Dessiner le fond du bouton en rouge
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight);
            shapeRenderer.end();
            batch.begin();

            // Dessiner le texte du bouton en blanc
            font.setColor(Color.WHITE);
            font.draw(batch, "RESET DATA", buttonX + 30, buttonY + 50);
        } else if (gamestate == 0) {
            font.getData().setScale(4);
            font.draw(batch, "Tap to Start", screenWidth / 2 - 150, screenHeight / 2);

            // Affiche le message de bienvenue si nécessaire
            if (showWelcomeMessage) {
                font.setColor(Color.GREEN);
                font.draw(batch, "Welcome back, " + userName + "!",
                    screenWidth / 2 - 200, screenHeight / 2 + 100);
                font.setColor(Color.WHITE);
            }
        }

        batch.end();

        // Dessiner les hitboxes (optionnel - pour débugger)
        if (gamestate == 1) {
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
    }

    // Nouvelle méthode pour afficher l'écran scindé initial
    private void renderSplitMenu(float screenWidth, float screenHeight) {
        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);

        // Dessiner la ligne de séparation verticale
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(screenWidth / 2 - 2, 0, 4, screenHeight);
        shapeRenderer.end();
        batch.begin();

        // Dessiner les boutons
        float buttonWidth = 300;
        float buttonHeight = 150;

        // Bouton Jouer (côté gauche)
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(screenWidth / 4 - buttonWidth / 2, screenHeight / 2 - buttonHeight / 2, buttonWidth, buttonHeight);
        shapeRenderer.end();
        batch.begin();

        // Bouton Statistiques (côté droit)
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.rect(3 * screenWidth / 4 - buttonWidth / 2, screenHeight / 2 - buttonHeight / 2, buttonWidth, buttonHeight);
        shapeRenderer.end();
        batch.begin();

        // Texte des boutons
        font.getData().setScale(5);
        font.setColor(Color.WHITE);
        font.draw(batch, "JOUER", screenWidth / 4 - 100, screenHeight / 2 + 20);
        font.draw(batch, "STATS", 3 * screenWidth / 4 - 100, screenHeight / 2 + 20);

        // Titre du jeu en haut
        font.getData().setScale(6);
        font.draw(batch, "LINUX BIRD", screenWidth / 2 - 200, screenHeight - 100);

        batch.end();

        // Vérifier les clics
        if (Gdx.input.justTouched()) {
            float touchX = Gdx.input.getX();
            float touchY = screenHeight - Gdx.input.getY();

            // Clic sur le bouton Jouer (côté gauche)
            if (touchX < screenWidth / 2) {
                if (isFirstTimeUser) {
                    gamestate = 3; // Demander nom si premier démarrage
                    namePopupShown = false;
                } else {
                    gamestate = 0; // Mode attente
                    resetGame();
                }
            }
            // Clic sur le bouton Statistiques (côté droit)
            else {
                gamestate = 4; // Mode statistiques
                prepareStatsData();
            }
        }
    }

    // Méthode pour préparer les données de statistiques
    private void prepareStatsData() {
        statsSummaries.clear();
        currentStatsPage = 0;

        // Générer des résumés pour chaque joueur
        for (Map.Entry<String, List<Integer>> entry : userScores.entrySet()) {
            String name = entry.getKey();
            List<Integer> scores = entry.getValue();

            if (scores.isEmpty()) continue;

            // Calculer des statistiques
            int max = 0;
            int sum = 0;
            for (int s : scores) {
                max = Math.max(max, s);
                sum += s;
            }
            double avg = (double) sum / scores.size();

            String summary = name + ": Parties: " + scores.size() +
                ", Max: " + max +
                ", Moy: " + String.format("%.1f", avg);

            statsSummaries.add(summary);
        }

        // Si aucune donnée disponible
        if (statsSummaries.isEmpty()) {
            statsSummaries.add("Aucune donnée de jeu disponible");
        }
    }

    // Méthode pour afficher l'écran des statistiques
    private void renderStatsScreen(float screenWidth, float screenHeight) {
        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);

        // Titre
        font.getData().setScale(6);
        font.setColor(Color.WHITE);
        font.draw(batch, "STATISTIQUES", screenWidth / 2 - 250, screenHeight - 50);

        // Ligne de démarcation
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(50, screenHeight - 100, screenWidth - 100, 4);
        shapeRenderer.end();
        batch.begin();

        // Afficher les statistiques
        font.getData().setScale(3);
        int startIdx = currentStatsPage * maxStatsPerPage;
        int endIdx = Math.min(startIdx + maxStatsPerPage, statsSummaries.size());

        for (int i = startIdx; i < endIdx; i++) {
            font.draw(batch, statsSummaries.get(i), 100, screenHeight - 150 - (i - startIdx) * 50);
        }

        // Boutons de navigation
        float buttonWidth = 200;
        float buttonHeight = 80;
        float buttonY = 100;

        // Bouton précédent
        if (currentStatsPage > 0) {
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.BLUE);
            shapeRenderer.rect(100, buttonY, buttonWidth, buttonHeight);
            shapeRenderer.end();
            batch.begin();

            font.setColor(Color.WHITE);
            font.draw(batch, "< PREV", 150, buttonY + 50);
        }

        // Bouton suivant
        if ((currentStatsPage + 1) * maxStatsPerPage < statsSummaries.size()) {
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.BLUE);
            shapeRenderer.rect(screenWidth - 300, buttonY, buttonWidth, buttonHeight);
            shapeRenderer.end();
            batch.begin();

            font.setColor(Color.WHITE);
            font.draw(batch, "NEXT >", screenWidth - 250, buttonY + 50);
        }

        // Bouton Retour
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(screenWidth / 2 - buttonWidth / 2, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.end();
        batch.begin();

        font.setColor(Color.WHITE);
        font.draw(batch, "MENU", screenWidth / 2 - 50, buttonY + 50);

        batch.end();

        // Vérifier les clics
        if (Gdx.input.justTouched()) {
            float touchX = Gdx.input.getX();
            float touchY = screenHeight - Gdx.input.getY();

            // Clic sur bouton précédent
            if (currentStatsPage > 0 && touchX >= 100 && touchX <= 100 + buttonWidth &&
                touchY >= buttonY && touchY <= buttonY + buttonHeight) {
                currentStatsPage--;
            }

            // Clic sur bouton suivant
            if ((currentStatsPage + 1) * maxStatsPerPage < statsSummaries.size() &&
                touchX >= screenWidth - 300 && touchX <= screenWidth - 300 + buttonWidth &&
                touchY >= buttonY && touchY <= buttonY + buttonHeight) {
                currentStatsPage++;
            }

            // Clic sur bouton retour
            if (touchX >= screenWidth / 2 - buttonWidth / 2 && touchX <= screenWidth / 2 + buttonWidth / 2 &&
                touchY >= buttonY && touchY <= buttonY + buttonHeight) {
                gamestate = -1; // Retour au menu principal
            }
        }
    }

    // Méthode appelée quand le joueur a saisi son nom
    @Override
    public void input(String text) {
        // Vérifier que le texte n'est pas vide
        if (text != null && !text.trim().isEmpty()) {
            userName = text.trim();
        }

        // Sauvegarder le nom et passer au jeu
        saveUserData();
        gamestate = 0;
        showWelcomeMessage = true;
        welcomeMessageTimer = 3;
    }

    // Méthode appelée si le joueur annule la saisie
    @Override
    public void canceled() {
        // Si l'utilisateur annule, utiliser le nom par défaut
        saveUserData();
        gamestate = 0;
        showWelcomeMessage = true;
        welcomeMessageTimer = 3;
    }

    private void resetGame() {
        gamestate = 0;
        score = 0;
        velocity = 0;
        birdY = (float) Gdx.graphics.getHeight() / 2 - (float) birds[0].getHeight() / 2;
        tubeX = Gdx.graphics.getWidth();
        crottes.clear();
    }

    private void resetAllData() {
        // Effacer toutes les données de scores
        userScores.clear();
        bestScore = 0;

        // Sauvegarder l'état vide
        saveUserData();

        // Afficher un message de confirmation dans la console
        System.out.println("TOUTES LES DONNÉES ONT ÉTÉ RÉINITIALISÉES!");

        // Réinitialiser le jeu
        resetGame();
    }

    private void saveScore() {
        // Ajouter le score actuel à la liste des scores de l'utilisateur
        if (!userScores.containsKey(userName)) {
            userScores.put(userName, new ArrayList<>());
        }
        userScores.get(userName).add(score);

        // Mettre à jour le meilleur score
        if (score > bestScore) {
            bestScore = score;
        }

        saveUserData();
    }

    private void loadUserData() {
        File file = new File("userdata.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String name = reader.readLine();
                if (name != null && !name.trim().isEmpty()) {
                    userName = name;
                    isFirstTimeUser = false;
                } else {
                    isFirstTimeUser = true;
                }

                String scoreStr = reader.readLine();
                if (scoreStr != null && !scoreStr.trim().isEmpty()) {
                    try {
                        bestScore = Integer.parseInt(scoreStr);
                    } catch (NumberFormatException e) {
                        bestScore = 0;
                    }
                }

                // Lire les scores de tous les utilisateurs
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            String user = parts[0];
                            String[] scoreValues = parts[1].split(",");
                            List<Integer> scores = new ArrayList<>();

                            for (String s : scoreValues) {
                                try {
                                    if (!s.trim().isEmpty()) {
                                        scores.add(Integer.parseInt(s.trim()));
                                    }
                                } catch (NumberFormatException e) {
                                    // Ignorer les valeurs non numériques
                                }
                            }

                            userScores.put(user, scores);
                        }
                    }
                }

            } catch (IOException e) {
                isFirstTimeUser = true;
                System.out.println("Erreur lors de la lecture des données: " + e.getMessage());
            }
        } else {
            isFirstTimeUser = true;
        }

        System.out.println("Données chargées: " + userName + ", meilleur score: " + bestScore);
        System.out.println("Premier démarrage: " + isFirstTimeUser);
        System.out.println("Scores chargés: " + userScores.size() + " utilisateurs");
    }

    private void saveUserData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("userdata.txt"))) {
            writer.write(userName + "\n");
            writer.write(bestScore + "\n");

            // Écrire tous les scores de tous les utilisateurs
            for (Map.Entry<String, List<Integer>> entry : userScores.entrySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append(entry.getKey()).append(":");

                List<Integer> scores = entry.getValue();
                for (int i = 0; i < scores.size(); i++) {
                    sb.append(scores.get(i));
                    if (i < scores.size() - 1) {
                        sb.append(",");
                    }
                }

                writer.write(sb.toString() + "\n");
            }

            System.out.println("Données sauvegardées: " + userName + ", meilleur score: " + bestScore);
            System.out.println("Scores sauvegardés pour " + userScores.size() + " utilisateurs");
        } catch (IOException e) {
            System.out.println("Erreur lors de la sauvegarde des données: " + e.getMessage());
        }
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
