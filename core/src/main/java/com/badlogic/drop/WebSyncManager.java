package com.badlogic.drop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Json;

/**
 * Manages synchronization with remote services for web deployment
 */
public class WebSyncManager {
    // Use a simple JSON storage service like JSONBin.io
    private static final String API_URL = "https://api.jsonbin.io/v3/b/";
    private static final String BIN_ID = "YOUR_BIN_ID"; // Replace with your actual bin ID
    private static final String API_KEY = "YOUR_API_KEY"; // Replace with your actual API key
    
    private boolean isSyncing = false;
    private long lastSyncTime = 0;
    private static final long SYNC_INTERVAL = 60000; // 1 minute in milliseconds
    
    // Sync high scores to remote storage
    public void syncHighScores(final ObjectMap<String, Integer> highScores, final Array<String> usernames) {
        if (isSyncing) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSyncTime < SYNC_INTERVAL) {
            return; // Don't sync too frequently
        }
        
        isSyncing = true;
        lastSyncTime = currentTime;
        
        try {
            // Create JSON data
            Json json = new Json();
            json.setOutputType(OutputType.json);
            
            // Create a data structure to hold our high scores
            ObjectMap<String, Object> data = new ObjectMap<>();
            data.put("lastUpdated", currentTime);
            
            // Add scores
            ObjectMap<String, Integer> scores = new ObjectMap<>();
            for (String username : usernames) {
                scores.put(username, highScores.get(username, 0));
            }
            data.put("scores", scores);
            
            // Convert to JSON string
            final String jsonData = json.toJson(data);
            
            // Create HTTP request
            HttpRequest request = new HttpRequest(HttpMethods.PUT);
            request.setUrl(API_URL + BIN_ID);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("X-Master-Key", API_KEY);
            request.setContent(jsonData);
            
            // Send request
            Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
                @Override
                public void handleHttpResponse(HttpResponse httpResponse) {
                    String response = httpResponse.getResultAsString();
                    Gdx.app.log("WebSync", "Sync successful: " + response);
                    isSyncing = false;
                }
                
                @Override
                public void failed(Throwable t) {
                    Gdx.app.log("WebSync", "Sync failed: " + t.getMessage());
                    isSyncing = false;
                }
                
                @Override
                public void cancelled() {
                    Gdx.app.log("WebSync", "Sync cancelled");
                    isSyncing = false;
                }
            });
        } catch (Exception e) {
            Gdx.app.log("WebSync", "Error during sync: " + e.getMessage());
            isSyncing = false;
        }
    }
    
    // Fetch high scores from remote storage
    public void fetchHighScores(final FetchCallback callback) {
        if (isSyncing) {
            if (callback != null) callback.onComplete(false, null, null);
            return;
        }
        
        isSyncing = true;
        
        try {
            // Create HTTP request
            HttpRequest request = new HttpRequest(HttpMethods.GET);
            request.setUrl(API_URL + BIN_ID);
            request.setHeader("X-Master-Key", API_KEY);
            
            // Send request
            Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
                @Override
                public void handleHttpResponse(HttpResponse httpResponse) {
                    try {
                        String response = httpResponse.getResultAsString();
                        JsonValue json = new JsonReader().parse(response);
                        
                        // Extract data from the response
                        JsonValue record = json.get("record");
                        if (record != null) {
                            JsonValue scores = record.get("scores");
                            
                            if (scores != null) {
                                ObjectMap<String, Integer> highScores = new ObjectMap<>();
                                Array<String> usernames = new Array<>();
                                
                                // Iterate through scores
                                for (JsonValue entry = scores.child; entry != null; entry = entry.next) {
                                    String username = entry.name;
                                    int score = entry.asInt();
                                    
                                    highScores.put(username, score);
                                    usernames.add(username);
                                }
                                
                                if (callback != null) {
                                    callback.onComplete(true, highScores, usernames);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Gdx.app.log("WebSync", "Error parsing response: " + e.getMessage());
                        if (callback != null) callback.onComplete(false, null, null);
                    }
                    
                    isSyncing = false;
                }
                
                @Override
                public void failed(Throwable t) {
                    Gdx.app.log("WebSync", "Fetch failed: " + t.getMessage());
                    if (callback != null) callback.onComplete(false, null, null);
                    isSyncing = false;
                }
                
                @Override
                public void cancelled() {
                    Gdx.app.log("WebSync", "Fetch cancelled");
                    if (callback != null) callback.onComplete(false, null, null);
                    isSyncing = false;
                }
            });
        } catch (Exception e) {
            Gdx.app.log("WebSync", "Error during fetch: " + e.getMessage());
            if (callback != null) callback.onComplete(false, null, null);
            isSyncing = false;
        }
    }
    
    // Interface for fetch callback
    public interface FetchCallback {
        void onComplete(boolean success, ObjectMap<String, Integer> highScores, Array<String> usernames);
    }
}
