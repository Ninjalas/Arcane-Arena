package com.mycompany.finals_java;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    // Update with your MySQL credentials
    private static final String URL = "jdbc:mysql://localhost:3306/finals_java" 
                       + "?useSSL=false"
                       + "&allowPublicKeyRetrieval=true"
                       + "&serverTimezone=Asia/Manila"  
                       + "&useUnicode=true"
                       + "&characterEncoding=UTF-8";
    
    private static final String USER = "root";
    private static final String PASSWORD = "santosgrow5416";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Error: " + e);
        }
    }
    
    public static boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Database connected successfully!");
            return true;
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }
    
    // Save a player's score to the Leaderboards table
    public static void saveScore(String playerName, int score) {
        String sql = "INSERT INTO Leaderboards (player_name, score) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE score = GREATEST(score, VALUES(score))";
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error: " + e);
        }
    }

    // Retrieve top scores (highest first)
    public static List<String[]> getTopScores(int limit) {
        List<String[]> topScores = new ArrayList<>();
        String sql = "SELECT player_name, score FROM Leaderboards ORDER BY score DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                topScores.add(new String[]{
                    rs.getString("player_name"),
                    String.valueOf(rs.getInt("score"))
                });
            }
            
        } catch (SQLException e) {
            System.out.println("Error: " + e);
        }
        return topScores;
    }
    
    public static int getPlayerBestScore(String playerName) {
        String sql = "SELECT score FROM Leaderboards WHERE player_name = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("score");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
}