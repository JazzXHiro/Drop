package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Implementation of DatabaseManager using LibGDX Preferences for local storage
 */
public class LocalDatabaseManager implements DatabaseManager {
    private Preferences prefs;
    private ObjectMap<String, Integer> highScores = new ObjectMap<>();
    private Array<String> usernames = new Array<>();
    
    public LocalDatabaseManager() {
        prefs = Gdx.app.getPreferences("dropGame");
        loadData();
    }
    
    private void loadData() {
        // Load all users and their high scores
        String userListStr = prefs.getString("userList", "");
        if (!userListStr.isEmpty()) {
            String[] users = userListStr.split(",");
            for (String user : users) {
                usernames.add(user);
                int score = prefs.getInteger(user + "_score", 0);
                highScores.put(user, score);
            }
        }
    }
    
    @Override
    public void saveHighScore(String username, int score) {
        int existingScore = highScores.get(username, 0);
        if (score > existingScore) {
            highScores.put(username, score);
            prefs.putInteger(username + "_score", score);
            
            // Update user list if needed
            if (!usernames.contains(username, false)) {
                usernames.add(username);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < usernames.size; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(usernames.get(i));
                }
                prefs.putString("userList", sb.toString());
            }
        }
    }
    
    @Override
    public int getHighScore(String username) {
        return highScores.get(username, 0);
    }
    
    @Override
    public ObjectMap<String, Integer> getAllHighScores() {
        return highScores;
    }
    
    @Override
    public Array<String> getAllUsernames() {
        return usernames;
    }
    
    @Override
    public void flush() {
        prefs.flush();
    }
}
