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
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main implements ApplicationListener {
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
    
    // Score variables
    private int currentScore = 0;
    private int highScore = 0;
    private BitmapFont font;
    private Preferences prefs;

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
        
        // Load high score
        prefs = Gdx.app.getPreferences("dropGame");
        highScore = prefs.getInteger("highScore", 0);
        
        // Only auto-start on desktop, wait for interaction on WebGL and mobile
        boolean isWebGL = Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.WebGL);
        boolean isMobile = Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.Android) || 
                          Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.iOS);
        if (!isWebGL && !isMobile) {
            gameStarted = true;
            if (music != null) music.play();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        input();
        logic();
        draw();
    }

    private void input(){
        // Start game on any input if not started in WebGL or mobile
        if (!gameStarted && (Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.WebGL) || 
            Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.Android) ||
            Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.iOS))) {
            if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Keys.ANY_KEY)) {
                gameStarted = true;
                
                // Mobile browsers need special handling for audio
                boolean isMobileBrowser = Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.WebGL) && 
                                         (Gdx.app.getVersion() == 0); // Default to assuming mobile for WebGL
                
                // Re-initialize audio for mobile browsers on user gesture
                if (isMobileBrowser) {
                    Gdx.app.log("Audio", "Android mobile browser detected - special handling");
                    // Try to reload and play music immediately on user interaction for Android
                    try {
                        if (music != null) {
                            music.dispose(); // Release any previous instance
                        }
                        
                        // Try different formats - for Android, OGG first
                        if (Gdx.files.internal("music.ogg").exists()) {
                            Gdx.app.log("Audio", "Trying OGG for Android mobile browser");
                            music = Gdx.audio.newMusic(Gdx.files.internal("music.ogg"));
                        } else if (Gdx.files.internal("music.mp3").exists()) {
                            Gdx.app.log("Audio", "Trying MP3 for Android mobile browser");
                            music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
                        }
                        
                        if (music != null) {
                            // Android-specific settings
                            music.setLooping(true);
                            music.setVolume(1.0f);
                            // Give Android a moment to prepare the audio
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        music.play();
                                        Gdx.app.log("Audio", "Android browser: Music play() called on separate thread");
                                    } catch (Exception e) {
                                        Gdx.app.log("Audio", "Android thread error: " + e.getMessage());
                                    }
                                }
                            });
                            Gdx.app.log("Audio", "Android browser: Music scheduled to play");
                        }
                    } catch (Exception e) {
                        Gdx.app.log("Audio", "Android browser: Error with music: " + e.getMessage());
                        silentMode = true;
                    }
                } else if (!silentMode && music != null) {
                    // Regular WebGL and other platforms
                    try {
                        Gdx.app.log("Audio", "Attempting to play music on user interaction");
                        music.setVolume(1.0f);
                        music.play();
                        Gdx.app.log("Audio", "Music play() called successfully");
                    } catch (Exception e) {
                        Gdx.app.log("Audio", "Error playing music: " + e.getMessage());
                        silentMode = true;
                    }
                } else {
                    Gdx.app.log("Audio", "Not playing music - Silent mode: " + silentMode + ", Music object null: " + (music == null));
                }
            }
            return; // Skip other input until game starts
        }

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
        
        // Check if game needs to show start screen in WebGL or mobile
        if (!gameStarted && (Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.WebGL) ||
            Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.Android) ||
            Gdx.app.getType().equals(com.badlogic.gdx.Application.ApplicationType.iOS))) {
            // Draw start instructions
            String instructions = "TAP OR PRESS ANY KEY TO START";
            float textWidth = font.draw(spriteBatch, instructions, 0, 0).width;
            font.draw(spriteBatch, instructions, 
                     (worldWidth - textWidth) / 2, worldHeight / 2);
        } else {
            // Draw game elements
            bucketSprite.draw(spriteBatch);
            
            for (Sprite dropSprite: dropSprites){
                dropSprite.draw(spriteBatch);
            }
            
            // Draw score and high score
            font.draw(spriteBatch, "Score: " + currentScore, 10, worldHeight - 10);
            font.draw(spriteBatch, "High Score: " + highScore, 10, worldHeight - 30);
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
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
        // Reload high score in case it was changed
        highScore = prefs.getInteger("highScore", 0);
    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        bucketTexture.dispose();
        dropTexture.dispose();
        dropSound.dispose();
        music.dispose();
        spriteBatch.dispose();
        font.dispose();
    }
}
