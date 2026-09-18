package com.mycompany.finals_java;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyControls implements KeyListener {
    private GamePanel gamePanel;

    public KeyControls(GamePanel gp) {
        this.gamePanel = gp;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_Q) {
            gamePanel.requestShoot();
        }
        if (e.getKeyCode() == KeyEvent.VK_E) {
            gamePanel.requestDash();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}