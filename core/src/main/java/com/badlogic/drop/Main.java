package com.badlogic.drop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main implements ApplicationListener {
    // Game states
    private enum GameState {
        LOGIN,       // Login screen
        ADMIN_VIEW,  // Admin high score view
        GAME_READY,  // Ready to start
        GAME_RUNNING // Game is running
    }
    
    // User roles
    private enum UserRole {
        USER,
        ADMIN
    }
    
    // Game assets
    Texture backgroundTexture;
    Texture bucketTexture;
    Texture dropTexture;
    Sound dropSound;
    Music music;

    SpriteBatch spriteBatch;
    FitViewport viewport;

    Sprite bucketSprite;

    Vector2 touchPos;

    Array<Sprite> dropSprites;

    float dropTimer;

    Rectangle bucketRectangle;
    Rectangle dropRectangle;
    
    // Button rectangles for login screen
    private Rectangle adminButton = new Rectangle();
    private Rectangle userButton = new Rectangle();
    
    // Score variables
    private int currentScore = 0;
    private int highScore = 0;
    private BitmapFont font;
    private Preferences prefs;
    
    // Login system variables
    private GameState gameState = GameState.LOGIN;
    private String currentUser = "";
    private UserRole currentRole = UserRole.USER;
    private String inputText = "";
    private boolean isSelectingRole = true;
    private ObjectMap<String, Integer> allHighScores = new ObjectMap<>();
    private Array<String> userNames = new Array<>();
    
    // Text input field for username (clickable area)
    private Rectangle usernameField = new Rectangle();
    private boolean waitingForTextInput = false;

    private boolean gameStarted = false;
    private boolean silentMode = false;

    @Override
    public void create() {
        backgroundTexture = new Texture("background.png");
        bucketTexture = new Texture("bucket.png");
        dropTexture = new Texture("drop.png");
        
        spriteBatch = new SpriteBatch();
        viewport = new FitViewport(800, 480);
        viewport.getCamera().position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);

        bucketSprite = new Sprite(bucketTexture);
        bucketSprite.setSize(64, 64);
        bucketSprite.setPosition(viewport.getWorldWidth() / 2 - 32, 20);
        
        touchPos = new Vector2();
        dropSprites = new Array<>();
        bucketRectangle = new Rectangle();
        dropRectangle = new Rectangle();
        
        // Initialize audio with mobile-friendly approach
        try {
            boolean isWebGL = Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.WebGL);
            boolean isMobile = Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.Android) || 
                             Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.iOS);
            boolean isMobileBrowser = isWebGL && (Gdx.app.getVersion() == 0);
            
            Gdx.app.log("Audio", "Platform - WebGL: " + isWebGL + ", Mobile: " + isMobile + ", Mobile Browser: " + isMobileBrowser);
            
            // Try to load audio files with platform-specific fallbacks
            if (isWebGL) {
                if (isMobileBrowser) {
                    // For mobile browsers on Android, OGG usually works better
                    Gdx.app.log("Audio", "Android mobile browser detected, prioritizing OGG");
                    if (Gdx.files.internal("drop.ogg").exists()) {
                        Gdx.app.log("Audio", "Loading OGG files for Android mobile browser");
                        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.ogg"));
                        music = Gdx.audio.newMusic(Gdx.files.internal("music.ogg"));
                    } else if (Gdx.files.internal("drop.mp3").exists()) {
                        Gdx.app.log("Audio", "OGG not found, trying MP3 for Android mobile browser");
                        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
                        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
                    }
                } else {
                    // For desktop browsers, try OGG first
                    if (Gdx.files.internal("drop.ogg").exists()) {
                        Gdx.app.log("Audio", "Loading OGG files for desktop browser");
                        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.ogg"));
                        music = Gdx.audio.newMusic(Gdx.files.internal("music.ogg"));
                    } else if (Gdx.files.internal("drop.mp3").exists()) {
                        Gdx.app.log("Audio", "OGG not found, loading MP3 files");
                        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
                        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
                    }
                }
            } else if (isMobile) {
                // For native mobile, try MP3 first
                if (Gdx.files.internal("drop.mp3").exists()) {
                    Gdx.app.log("Audio", "Loading MP3 files for native mobile");
                    dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
                    music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
                } else if (Gdx.files.internal("drop.ogg").exists()) {
                    Gdx.app.log("Audio", "MP3 not found, loading OGG files for native mobile");
                    dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.ogg"));
                    music = Gdx.audio.newMusic(Gdx.files.internal("music.ogg"));
                }
            } else {
                // For desktop, try MP3 first
                if (Gdx.files.internal("drop.mp3").exists()) {
                    Gdx.app.log("Audio", "Loading MP3 files for desktop");
                    dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
                    music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
                } else if (Gdx.files.internal("drop.ogg").exists()) {
                    Gdx.app.log("Audio", "MP3 not found, loading OGG files");
                    dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.ogg"));
                    music = Gdx.audio.newMusic(Gdx.files.internal("music.ogg"));
                }
            }
            
            if (music != null) {
                music.setLooping(true);
                music.setVolume(1.0f);
                Gdx.app.log("Audio", "Music configured successfully");
            }
            
            // Set silent mode if no audio files were found
            if (dropSound == null && music == null) {
                Gdx.app.log("Audio", "No audio files found, entering silent mode");
                silentMode = true;
            }
        } catch (Exception e) {
            Gdx.app.log("Audio", "Error loading audio: " + e.getMessage());
            silentMode = true;
        }
        
        // Initialize font
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        
        // Load user data and high scores
        loadUserData();
    }

    private void loadUserData() {
        prefs = Gdx.app.getPreferences("dropGame");
        
        // Load all users and their high scores
        String userListStr = prefs.getString("userList", "");
        if (!userListStr.isEmpty()) {
            String[] users = userListStr.split(",");
            for (String user : users) {
                userNames.add(user);
                int score = prefs.getInteger(user + "_score", 0);
                allHighScores.put(user, score);
            }
        }
    }
    
    private void saveUserData() {
        // Save high score for current user
        if (!currentUser.isEmpty()) {
            int existingScore = allHighScores.get(currentUser, 0);
            if (currentScore > existingScore) {
                allHighScores.put(currentUser, currentScore);
                prefs.putInteger(currentUser + "_score", currentScore);
            }
            
            // Update user list if needed
            if (!userNames.contains(currentUser, false)) {
                userNames.add(currentUser);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < userNames.size; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(userNames.get(i));
                }
                prefs.putString("userList", sb.toString());
            }
            
            prefs.flush();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        switch (gameState) {
            case LOGIN:
                inputLogin();
                drawLogin();
                break;
            case ADMIN_VIEW:
                inputAdminView();
                drawAdminView();
                break;
            case GAME_READY:
                inputReady();
                drawReady();
                break;
            case GAME_RUNNING:
                input();
                logic();
                draw();
                break;
        }
    }
    
    private void inputLogin() {
        // Handle input for the login screen
        if (isSelectingRole) {
            // First screen - select role with clickable buttons
            if (Gdx.input.justTouched()) {
                touchPos.set(Gdx.input.getX(), Gdx.input.getY());
                viewport.unproject(touchPos);
                
                if (adminButton.contains(touchPos.x, touchPos.y)) {
                    currentRole = UserRole.ADMIN;
                    isSelectingRole = false;
                    inputText = "";
                } else if (userButton.contains(touchPos.x, touchPos.y)) {
                    currentRole = UserRole.USER;
                    isSelectingRole = false;
                    inputText = "";
                }
            }
        } else {
            // Username input screen
            
            // Handle touch input for mobile/browser
            if (Gdx.input.justTouched() && !waitingForTextInput) {
                touchPos.set(Gdx.input.getX(), Gdx.input.getY());
                viewport.unproject(touchPos);
                
                // Check if username field was clicked
                if (usernameField.contains(touchPos.x, touchPos.y)) {
                    // Show input dialog for mobile
                    waitingForTextInput = true;
                    Gdx.input.getTextInput(new com.badlogic.gdx.Input.TextInputListener() {
                        @Override
                        public void input(String text) {
                            if (text != null && !text.isEmpty()) {
                                inputText = text;
                                submitUsername();
                            }
                            waitingForTextInput = false;
                        }
                        
                        @Override
                        public void canceled() {
                            waitingForTextInput = false;
                        }
                    }, "Enter Username", "", "Type your username here");
                }
            }
            
            // Also keep keyboard input for desktop
            if (!waitingForTextInput) {
                // Add numbers
                for (int i = 0; i < 10; i++) {
                    if (Gdx.input.isKeyJustPressed(Keys.NUM_0 + i)) {
                        inputText += i;
                    }
                }
                
                // Add letters
                for (int i = 0; i < 26; i++) {
                    if (Gdx.input.isKeyJustPressed(Keys.A + i)) {
                        inputText += (char)('a' + i);
                    }
                }
                
                // Handle backspace
                if (Gdx.input.isKeyJustPressed(Keys.BACKSPACE) && inputText.length() > 0) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
                
                // Handle enter/submit
                if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                    submitUsername();
                }
            }
        }
    }
    
    private void submitUsername() {
        // Process username submission
        if (!inputText.isEmpty()) {
            currentUser = inputText;
            
            // Set high score based on user
            highScore = allHighScores.get(currentUser, 0);
            
            // Move to appropriate screen based on role
            if (currentRole == UserRole.ADMIN) {
                gameState = GameState.ADMIN_VIEW;
            } else {
                gameState = GameState.GAME_READY;
            }
        }
    }
    
    private void drawLogin() {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();
        
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        
        // Draw background
        spriteBatch.draw(backgroundTexture, 0, 0, worldWidth, worldHeight);
        
        if (isSelectingRole) {
            // First screen - draw role selection buttons
            String title = "SELECT ROLE";
            float titleWidth = font.draw(spriteBatch, title, 0, 0).width;
            font.draw(spriteBatch, title, (worldWidth - titleWidth) / 2, worldHeight * 0.8f);
            
            // Draw Admin button
            float buttonWidth = 200;
            float buttonHeight = 60;
            float buttonSpacing = 40;
            float startY = worldHeight * 0.5f;
            
            // Admin button
            adminButton.set(worldWidth / 2 - buttonWidth / 2, startY, buttonWidth, buttonHeight);
            spriteBatch.setColor(0.2f, 0.2f, 0.8f, 1); // Blue for admin
            spriteBatch.draw(backgroundTexture, adminButton.x, adminButton.y, adminButton.width, adminButton.height);
            spriteBatch.setColor(Color.WHITE);
            
            String adminText = "ADMIN";
            float adminTextWidth = font.draw(spriteBatch, adminText, 0, 0).width;
            font.draw(spriteBatch, adminText, 
                     adminButton.x + (adminButton.width - adminTextWidth) / 2, 
                     adminButton.y + adminButton.height * 0.65f);
            
            // User button
            userButton.set(worldWidth / 2 - buttonWidth / 2, startY - buttonHeight - buttonSpacing, buttonWidth, buttonHeight);
            spriteBatch.setColor(0.2f, 0.8f, 0.2f, 1); // Green for user
            spriteBatch.draw(backgroundTexture, userButton.x, userButton.y, userButton.width, userButton.height);
            spriteBatch.setColor(Color.WHITE);
            
            String userText = "USER";
            float userTextWidth = font.draw(spriteBatch, userText, 0, 0).width;
            font.draw(spriteBatch, userText, 
                     userButton.x + (userButton.width - userTextWidth) / 2, 
                     userButton.y + userButton.height * 0.65f);
            
            // Draw instruction
            String tapInstr = "Tap a button to select your role";
            float instrWidth = font.draw(spriteBatch, tapInstr, 0, 0).width;
            font.draw(spriteBatch, tapInstr, (worldWidth - instrWidth) / 2, worldHeight * 0.27f);
        } else {
            // Second screen - username input
            String title = "ENTER USERNAME";
            float titleWidth = font.draw(spriteBatch, title, 0, 0).width;
            font.draw(spriteBatch, title, (worldWidth - titleWidth) / 2, worldHeight * 0.7f);
            
            // Draw input field as a button on mobile
            float fieldWidth = 300;
            float fieldHeight = 60;
            usernameField.set(worldWidth / 2 - fieldWidth / 2, worldHeight * 0.4f, fieldWidth, fieldHeight);
            
            // Draw input field background
            spriteBatch.setColor(0.1f, 0.1f, 0.1f, 1); // Dark background
            spriteBatch.draw(backgroundTexture, usernameField.x, usernameField.y, usernameField.width, usernameField.height);
            spriteBatch.setColor(Color.WHITE);
            
            // Draw text or placeholder
            String displayText = inputText.isEmpty() ? "TAP HERE TO ENTER USERNAME" : inputText;
            float textScale = inputText.isEmpty() ? 0.8f : 1.0f; // Smaller text for placeholder
            
            // Store original scale
            float originalScaleX = font.getScaleX();
            float originalScaleY = font.getScaleY();
            
            // Set scaled font for placeholder
            if (inputText.isEmpty()) {
                font.getData().setScale(textScale);
            }
            
            // Measure and draw text
            float textWidth = font.draw(spriteBatch, displayText, 0, 0).width;
            float textX = usernameField.x + (usernameField.width - textWidth) / 2;
            float textY = usernameField.y + usernameField.height * 0.65f;
            
            if (inputText.isEmpty()) {
                font.setColor(0.7f, 0.7f, 0.7f, 1); // Gray for placeholder
            }
            
            font.draw(spriteBatch, displayText, textX, textY);
            
            // Reset font settings
            font.setColor(Color.WHITE);
            if (inputText.isEmpty()) {
                font.getData().setScale(originalScaleX, originalScaleY);
            }
            
            // Instructions
            String instructions;
            if (Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.WebGL) || 
                Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.Android) || 
                Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.iOS)) {
                instructions = "Tap the field above to enter your username";
            } else {
                instructions = "Type your username and press Enter";
            }
            
            float promptWidth = font.draw(spriteBatch, instructions, 0, 0).width;
            font.draw(spriteBatch, instructions, (worldWidth - promptWidth) / 2, worldHeight * 0.3f);
        }
        
        spriteBatch.end();
    }
    
    private void inputAdminView() {
        // Handle input for the admin view
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.BACK)) {
            gameState = GameState.GAME_READY;
        }
    }
    
    private void drawAdminView() {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();
        
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        
        // Draw background
        spriteBatch.draw(backgroundTexture, 0, 0, worldWidth, worldHeight);
        
        // Draw title
        String title = "ADMIN VIEW - HIGH SCORES";
        float titleWidth = font.draw(spriteBatch, title, 0, 0).width;
        font.draw(spriteBatch, title, (worldWidth - titleWidth) / 2, worldHeight * 0.9f);
        
        // Draw instruction
        String backMsg = "Press ESC to return to game";
        float backWidth = font.draw(spriteBatch, backMsg, 0, 0).width;
        font.draw(spriteBatch, backMsg, (worldWidth - backWidth) / 2, worldHeight * 0.1f);
        
        // Draw all high scores
        float startY = worldHeight * 0.8f;
        float lineHeight = 30;
        
        if (userNames.size == 0) {
            String noUsers = "No players yet";
            float noUsersWidth = font.draw(spriteBatch, noUsers, 0, 0).width;
            font.draw(spriteBatch, noUsers, (worldWidth - noUsersWidth) / 2, startY);
        } else {
            for (int i = 0; i < userNames.size; i++) {
                String userName = userNames.get(i);
                int score = allHighScores.get(userName, 0);
                String scoreText = (i + 1) + ". " + userName + ": " + score;
                font.draw(spriteBatch, scoreText, worldWidth * 0.3f, startY - (i * lineHeight));
            }
        }
        
        spriteBatch.end();
    }
    
    private void inputReady() {
        // Handle input for the ready state
        boolean startInput = Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Keys.ANY_KEY);
        
        if (currentRole == UserRole.ADMIN && Gdx.input.isKeyJustPressed(Keys.H)) {
            // Admin can view high scores with H key
            gameState = GameState.ADMIN_VIEW;
        } else if (startInput) {
            gameStarted = true;
            gameState = GameState.GAME_RUNNING;
            
            // Mobile browsers need special handling for audio
            boolean isWebGL = Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.WebGL);
            boolean isMobileBrowser = isWebGL && (Gdx.app.getVersion() == 0);
            
            if (isMobileBrowser) {
                // Android-specific audio handling
                // ... existing mobile audio code ...
            } else if (!silentMode && music != null) {
                try {
                    music.setVolume(1.0f);
                    music.play();
                } catch (Exception e) {
                    Gdx.app.log("Audio", "Error playing music: " + e.getMessage());
                    silentMode = true;
                }
            }
        }
    }
    
    private void drawReady() {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();
        
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        
        // Draw background
        spriteBatch.draw(backgroundTexture, 0, 0, worldWidth, worldHeight);
        
        // Draw welcome message
        String welcome = "Welcome, " + currentUser + "!";
        float welcomeWidth = font.draw(spriteBatch, welcome, 0, 0).width;
        font.draw(spriteBatch, welcome, (worldWidth - welcomeWidth) / 2, worldHeight * 0.7f);
        
        // Draw your high score
        String yourScore = "Your High Score: " + highScore;
        float scoreWidth = font.draw(spriteBatch, yourScore, 0, 0).width;
        font.draw(spriteBatch, yourScore, (worldWidth - scoreWidth) / 2, worldHeight * 0.6f);
        
        // Draw start instructions
        String instructions = "TAP OR PRESS ANY KEY TO START";
        float instrWidth = font.draw(spriteBatch, instructions, 0, 0).width;
        font.draw(spriteBatch, instructions, (worldWidth - instrWidth) / 2, worldHeight * 0.5f);
        
        // Draw admin instructions if applicable
        if (currentRole == UserRole.ADMIN) {
            String adminInstr = "Press H to view all high scores";
            float adminWidth = font.draw(spriteBatch, adminInstr, 0, 0).width;
            font.draw(spriteBatch, adminInstr, (worldWidth - adminWidth) / 2, worldHeight * 0.4f);
        }
        
        spriteBatch.end();
    }

    private void input(){
        float speed = 250f;
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
            bucketSprite.translateX(speed * delta);
        } else if (Gdx.input.isKeyPressed(Keys.LEFT)) {
            bucketSprite.translateX(-speed * delta);
        }

        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(touchPos);
            bucketSprite.setCenterX(touchPos.x);
        }
        
        // Admin can switch to high score view with H
        if (currentRole == UserRole.ADMIN && Gdx.input.isKeyJustPressed(Keys.H)) {
            gameState = GameState.ADMIN_VIEW;
        }
    }

    private void logic(){
        // Skip game logic if not started in WebGL or mobile
        if (!gameStarted && (Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.WebGL) ||
            Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.Android) ||
            Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.iOS))) {
            return;
        }
        
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        float bucketWidth = bucketSprite.getWidth();
        float bucketHeight = bucketSprite.getHeight();

        bucketSprite.setX(MathUtils.clamp(bucketSprite.getX(), 0, worldWidth - bucketWidth));

        float delta = Gdx.graphics.getDeltaTime();

        bucketRectangle.set(bucketSprite.getX(), bucketSprite.getY(), bucketWidth, bucketHeight);

        for (int i = dropSprites.size - 1; i >= 0; i--) {
            Sprite dropSprite = dropSprites.get(i);
            float dropWidth = dropSprite.getWidth();
            float dropHeight = dropSprite.getHeight();

            dropSprite.translateY((-200f * delta));
            dropRectangle.set(dropSprite.getX(), dropSprite.getY(), dropWidth, dropHeight);

            if (dropSprite.getY() < -dropHeight) dropSprites.removeIndex(i);
            else if (bucketRectangle.overlaps(dropRectangle)){
                dropSprites.removeIndex(i);
                if (!silentMode && dropSound != null) {
                    try {
                        // Try to play the sound with maximum volume
                        Gdx.app.log("Audio", "Attempting to play drop sound");
                        
                        // For mobile browsers, may need to reload the sound each time
                        boolean isMobileBrowser = Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.WebGL) && 
                                                 (Gdx.app.getVersion() == 0);
                        
                        if (isMobileBrowser) {
                            // Android-specific sound handling
                            try {
                                // Dispose old sound if exists
                                if (dropSound != null) {
                                    // Store a reference to avoid NullPointerException
                                    Sound oldSound = dropSound;
                                    
                                    // Try to load new sound instance - prefer OGG for Android
                                    if (Gdx.files.internal("drop.ogg").exists()) {
                                        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.ogg"));
                                    } else if (Gdx.files.internal("drop.mp3").exists()) {
                                        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
                                    }
                                    
                                    // Play new sound with Android-specific approach
                                    if (dropSound != null) {
                                        // Use postRunnable for more reliable Android sound
                                        final Sound finalSound = dropSound;
                                        Gdx.app.postRunnable(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    finalSound.play(1.0f);
                                                } catch (Exception e) {
                                                    Gdx.app.log("Audio", "Android thread sound error: " + e.getMessage());
                                                }
                                            }
                                        });
                                        Gdx.app.log("Audio", "Android: Scheduled drop sound");
                                    }
                                    
                                    // Dispose old sound after playing new one
                                    oldSound.dispose();
                                }
                            } catch (Exception e) {
                                Gdx.app.log("Audio", "Error with Android sound: " + e.getMessage());
                            }
                        } else {
                            // Normal sound playback for non-mobile
                            dropSound.play(1.0f);
                        }
                    } catch (Exception e) {
                        Gdx.app.log("Audio", "Error playing drop sound: " + e.getMessage());
                        silentMode = true;
                    }
                }
                
                // Increment score when drop is caught
                currentScore++;
                
                // Update high score if needed
                if (currentScore > highScore) {
                    highScore = currentScore;
                    // Save high score
                    prefs.putInteger("highScore", highScore);
                    prefs.flush();
                }
            }
        }

        dropTimer += delta;
        if (dropTimer > 1f) {
            dropTimer = 0;
            createDroplet();
        }
    }

    private void draw(){
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();

        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        // Draw background
        spriteBatch.draw(backgroundTexture, 0, 0, worldWidth, worldHeight);
        
        // Draw game elements
        bucketSprite.draw(spriteBatch);
        
        for (Sprite dropSprite: dropSprites){
            dropSprite.draw(spriteBatch);
        }
        
        // Draw score and high score
        font.draw(spriteBatch, currentUser + "'s Score: " + currentScore, 10, worldHeight - 10);
        font.draw(spriteBatch, "High Score: " + highScore, 10, worldHeight - 30);
        
        // Draw admin hint if applicable
        if (currentRole == UserRole.ADMIN) {
            font.draw(spriteBatch, "Press H for high scores", worldWidth - 200, worldHeight - 10);
        }

        spriteBatch.end();
    }

    private void createDroplet(){
        float dropWidth = 1;
        float dropHeight = 1;
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        Sprite dropSprite = new Sprite(dropTexture);
        dropSprite.setSize(64, 64);
        dropSprite.setX(MathUtils.random(0f, worldWidth - dropWidth));
        dropSprite.setY(worldHeight);
        dropSprites.add(dropSprite);
    }
    @Override
    public void pause() {
        // Save user data when paused
        saveUserData();
    }

    @Override
    public void resume() {
        // Reload high score in case it was changed
        if (!currentUser.isEmpty()) {
            highScore = prefs.getInteger(currentUser + "_score", 0);
        }
    }

    @Override
    public void dispose() {
        // Save data before disposing
        saveUserData();
        
        // Dispose resources
        backgroundTexture.dispose();
        bucketTexture.dispose();
        dropTexture.dispose();
        if (dropSound != null) dropSound.dispose();
        if (music != null) music.dispose();
        spriteBatch.dispose();
        font.dispose();
    }
}
