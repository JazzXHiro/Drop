package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Implementation of DatabaseManager using browser localStorage for web platforms
 */
public class WebDatabaseManager implements DatabaseManager {
    private ObjectMap<String, Integer> highScores = new ObjectMap<>();
    private Array<String> usernames = new Array<>();
    private WebSyncManager syncManager;
    private boolean initialSyncDone = false;
    
    public WebDatabaseManager() {
        syncManager = new WebSyncManager();
        loadData();
        
        // Try to fetch remote data
        syncManager.fetchHighScores(new WebSyncManager.FetchCallback() {
            @Override
            public void onComplete(boolean success, ObjectMap<String, Integer> remoteHighScores, Array<String> remoteUsernames) {
                if (success && remoteHighScores != null && remoteUsernames != null) {
                    // Merge remote data with local data
                    mergeData(remoteHighScores, remoteUsernames);
                    saveToLocalStorage();
                }
                initialSyncDone = true;
            }
        });
    }
    
    private void mergeData(ObjectMap<String, Integer> remoteHighScores, Array<String> remoteUsernames) {
        // For each remote username
        for (String username : remoteUsernames) {
            int remoteScore = remoteHighScores.get(username, 0);
            int localScore = highScores.get(username, 0);
            
            // Keep the higher score
            if (remoteScore > localScore) {
                highScores.put(username, remoteScore);
                if (!usernames.contains(username, false)) {
                    usernames.add(username);
                }
            }
        }
    }
    
    private void loadData() {
        try {
            // Using GWT JavaScript to access localStorage
            String userListStr = getLocalStorageItem("userList");
            if (userListStr != null && !userListStr.isEmpty()) {
                String[] users = userListStr.split(",");
                for (String user : users) {
                    usernames.add(user);
                    String scoreStr = getLocalStorageItem(user + "_score");
                    int score = scoreStr != null ? Integer.parseInt(scoreStr) : 0;
                    highScores.put(user, score);
                }
            }
        } catch (Exception e) {
            Gdx.app.log("WebDatabaseManager", "Error loading data: " + e.getMessage());
        }
    }
    
    private void saveToLocalStorage() {
        // Save high scores
        StringBuilder scoresJson = new StringBuilder();
        scoresJson.append("{");
        boolean first = true;
        for (ObjectMap.Entry<String, Integer> entry : highScores) {
            if (!first) scoresJson.append(",");
            scoresJson.append("\"").append(entry.key).append("\":").append(entry.value);
            first = false;
        }
        scoresJson.append("}");
        setLocalStorageItem("highScores", scoresJson.toString());

        // Save usernames
        StringBuilder usernamesJson = new StringBuilder();
        usernamesJson.append("[");
        first = true;
        for (String username : usernames) {
            if (!first) usernamesJson.append(",");
            usernamesJson.append("\"").append(username).append("\"");
            first = false;
        }
        usernamesJson.append("]");
        setLocalStorageItem("usernames", usernamesJson.toString());
    }
    
    // Native methods to interact with browser localStorage
    private native String getLocalStorageItem(String key) /*-{
        try {
            return $wnd.localStorage.getItem(key);
        } catch(e) {
            console.log("LocalStorage error: " + e);
            return null;
        }
    }-*/;
    
    private native void setLocalStorageItem(String key, String value) /*-{
        try {
            $wnd.localStorage.setItem(key, value);
        } catch(e) {
            console.log("LocalStorage error: " + e);
        }
    }-*/;
    
    @Override
    public void saveHighScore(String username, int score) {
        int existingScore = highScores.get(username, 0);
        if (score > existingScore) {
            highScores.put(username, score);
            setLocalStorageItem(username + "_score", String.valueOf(score));
            
            // Update user list if needed
            if (!usernames.contains(username, false)) {
                usernames.add(username);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < usernames.size; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(usernames.get(i));
                }
                setLocalStorageItem("userList", sb.toString());
            }
            
            // Sync to remote if we've done the initial sync
            if (initialSyncDone) {
                syncManager.syncHighScores(highScores, usernames);
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
        // Sync with remote storage
        if (initialSyncDone) {
            syncManager.syncHighScores(highScores, usernames);
        }
    }
}
