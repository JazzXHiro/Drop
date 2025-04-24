package com.badlogic.drop;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

/**
 * Factory class to provide the appropriate DatabaseManager implementation
 * based on the current platform
 */
public class DatabaseManagerFactory {
    private static DatabaseManager instance;
    
    public static DatabaseManager getDatabaseManager() {
        if (instance == null) {
            // Create appropriate implementation based on platform
            if (Gdx.app.getType() == Application.ApplicationType.WebGL) {
                instance = new WebDatabaseManager();
            } else {
                instance = new LocalDatabaseManager();
            }
        }
        return instance;
    }
}
