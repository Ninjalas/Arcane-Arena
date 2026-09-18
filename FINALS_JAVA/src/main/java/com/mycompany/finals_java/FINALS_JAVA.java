package com.mycompany.finals_java;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

public class FINALS_JAVA {
    

    private static String playerName;
    private static MouseControls gamePanel;
    private static JFrame window;

    public static void main(String[] args) {
         
        DatabaseManager db = new DatabaseManager();
        db.testConnection();

        playerName = JOptionPane.showInputDialog(null,
                "Enter your name:",
                "Player Name",
                JOptionPane.QUESTION_MESSAGE);
        if (playerName == null || playerName.trim().isEmpty()) playerName = "Anonymous";

        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setTitle("Arcane Arena");
        window.setResizable(false);          
        window.setSize(1248, 832);           

        gamePanel = new MouseControls();
        window.add(gamePanel);
        window.setLocationRelativeTo(null);   // center on screen
        window.setVisible(true);

        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();

        // Temporary listener for Enter key to start the game
        gamePanel.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    gamePanel.removeKeyListener(this);
                    gamePanel.addKeyListener(new KeyControls(gamePanel));
                    startGame();
                }
            }
            @Override public void keyReleased(KeyEvent e) {}
            @Override public void keyTyped(KeyEvent e) {}
        });

        JOptionPane.showMessageDialog(window,
                "Press ENTER to start the game!\nQ = Shoot | E = Dash (150px max)",
                "Ready?",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static void startGame() {
        gamePanel.playBackgroundMusic(0.5f);
        gamePanel.startThread();
        gamePanel.requestFocusInWindow();
        startMonitorThread();
    }

    private static void startMonitorThread() {
        new Thread(() -> {
            while (true) {
                try { Thread.sleep(100); } catch (InterruptedException ex) {}
                if (!gamePanel.gameRunning) {
                    SwingUtilities.invokeLater(() -> gameOver());
                    break;
                }
            }
        }).start();
    }

    private static void gameOver() {
        int finalScore = gamePanel.endGameAndGetScore();
        DatabaseManager.saveScore(playerName, finalScore);
        int bestScore = DatabaseManager.getPlayerBestScore(playerName);
        List<String[]> topScores = DatabaseManager.getTopScores(5);

        // Build the leaderboard text
        StringBuilder leaderboardText = new StringBuilder();
        leaderboardText.append("----------------------------\n");
        leaderboardText.append("        !GAME OVER!\n");
        leaderboardText.append("----------------------------\n");
        leaderboardText.append("Your Score: ").append(finalScore);
        if (finalScore > bestScore) leaderboardText.append(" (NEW BEST!)");
        else leaderboardText.append("\n(Best: ").append(bestScore).append(")");
        leaderboardText.append("\n\n    --- LEADERBOARD ---\n");
        int rank = 1;
        for (String[] entry : topScores) {
            leaderboardText.append(rank++).append(". ").append(entry[0]).append(" - ").append(entry[1]).append("\n");
        }

        // Create a custom panel with centered components
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextArea textArea = new JTextArea(leaderboardText.toString());
        textArea.setEditable(false);
        textArea.setBackground(panel.getBackground());
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(textArea);

        JButton tryAgainBtn = new JButton("Try Again");
        JButton exitBtn = new JButton("Exit");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.add(tryAgainBtn);
        buttonPanel.add(exitBtn);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(buttonPanel);

        // Create a modal dialog that cannot be dismissed by clicking outside
        JDialog dialog = new JDialog(window, "Game Over", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(window);
        dialog.setResizable(false);

        tryAgainBtn.addActionListener(e -> {
            dialog.dispose();
            gamePanel.resetGame();
            startMonitorThread();
            gamePanel.requestFocusInWindow();
        });

        exitBtn.addActionListener(e -> System.exit(0));

        dialog.setVisible(true);
    }
}