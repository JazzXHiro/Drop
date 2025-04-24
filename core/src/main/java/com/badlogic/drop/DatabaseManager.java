package com.badlogic.drop;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Interface for managing game data storage across different platforms
 */
public interface DatabaseManager {
    void saveHighScore(String username, int score);
    int getHighScore(String username);
    ObjectMap<String, Integer> getAllHighScores();
    Array<String> getAllUsernames();
    void flush(); // Save data immediately
}
