package com.mycompany.finals_java;

import java.awt.Point;
import java.awt.event.*;
import javax.swing.JFrame;

public class MouseControls extends GamePanel implements MouseListener, MouseMotionListener {

    private Point targetPoint; // target top-left coordinate
    private boolean moving;
    private double preciseX, preciseY;
    private static final int MOVE_SPEED = 3;  // normal movement speed

    public MouseControls() {
        targetPoint = null;
        moving = false;
        preciseX = avatarX;
        preciseY = avatarY;
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void stopMoving() {
        moving = false;
        targetPoint = null;
    }

    public void resetPosition(int x, int y) {
        avatarX = x;
        avatarY = y;
        preciseX = x;
        preciseY = y;
        moving = false;
        targetPoint = null;
    }

    @Override
    protected void setAvatarPosition(int x, int y) {
        avatarX = x;
        avatarY = y;
        preciseX = x;
        preciseY = y;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) setTarget(e.getPoint());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) setTarget(e.getPoint());
    }

    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    private void setTarget(Point clickPoint) {
        // Convert click point (desired bottom-center) to top-left coordinate
        int targetX = clickPoint.x - avatarWidth / 2;
        int targetY = clickPoint.y - avatarHeight;
        targetPoint = new Point(targetX, targetY);
        moving = true;
        // Show indicator at the click point (bottom-center)
        setTargetIndicator(clickPoint);
    }

    @Override
    public void update() {
        // If dashing, skip normal movement
        if (isDashing) {
            super.update();
            return;
        }

        // Normal movement
        if (moving && targetPoint != null) {
            double dx = targetPoint.x - preciseX;
            double dy = targetPoint.y - preciseY;
            double distance = Math.hypot(dx, dy);
            if (distance <= MOVE_SPEED) {
                preciseX = targetPoint.x;
                preciseY = targetPoint.y;
                moving = false;
                targetPoint = null;
            } else {
                double stepX = (dx / distance) * MOVE_SPEED;
                double stepY = (dy / distance) * MOVE_SPEED;
                preciseX += stepX;
                preciseY += stepY;
                updateDirectionFromVector(stepX, stepY);
            }
            avatarX = (int) Math.round(preciseX);
            avatarY = (int) Math.round(preciseY);
        }

        super.update();
    }

    private void updateDirectionFromVector(double dx, double dy) {
        if (dx == 0 && dy == 0) return;
        double correctedDy = -dy;
        double angle = Math.toDegrees(Math.atan2(correctedDy, dx));
        if (angle < 0) angle += 360;
        String direction;
        if (angle >= 337.5 || angle < 22.5) direction = "east";
        else if (angle >= 22.5 && angle < 67.5) direction = "northEast";
        else if (angle >= 67.5 && angle < 112.5) direction = "north";
        else if (angle >= 112.5 && angle < 157.5) direction = "northWest";
        else if (angle >= 157.5 && angle < 202.5) direction = "west";
        else if (angle >= 202.5 && angle < 247.5) direction = "southWest";
        else if (angle >= 247.5 && angle < 292.5) direction = "south";
        else direction = "southEast";
        switch (direction) {
            case "east": currentAvatarImage = avatar_east; break;
            case "northEast": currentAvatarImage = avatar_northEast; break;
            case "north": currentAvatarImage = avatar_north; break;
            case "northWest": currentAvatarImage = avatar_northWest; break;
            case "west": currentAvatarImage = avatar_west; break;
            case "southWest": currentAvatarImage = avatar_southWest; break;
            case "south": currentAvatarImage = avatar_south; break;
            case "southEast": currentAvatarImage = avatar_southEast; break;
        }
    }

    
}