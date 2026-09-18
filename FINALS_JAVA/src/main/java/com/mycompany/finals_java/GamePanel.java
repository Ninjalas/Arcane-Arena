package com.mycompany.finals_java;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.sound.sampled.*;

public class GamePanel extends JPanel implements Runnable {

    final int panelWidth = 1248;
    final int panelHeight = 832;
    final int centerX = panelWidth / 2;
    final int centerY = panelHeight / 2;

    // Avatar
    protected int avatarWidth = 100;
    protected int avatarHeight = 100;
    protected int avatarX, avatarY;
    protected Image currentAvatarImage;
    protected Image avatar_east, avatar_north, avatar_northEast, avatar_northWest;
    protected Image avatar_south, avatar_southEast, avatar_southWest, avatar_west;

    // Monsters
    private ArrayList<Monster> monsters;
    private Image[][] monsterImages;
    private Random random;
    private int spawnTimer = 0;
    private int spawnDelay;
    private static final int MONSTER_SIZE = 120;

    // Difficulty scaling
    private int score = 0;
    private double monsterBaseSpeed = 2.5;
    private double currentMonsterSpeed;

    // Shooting
    private Image lightningImage;
    private ArrayList<Projectile> projectiles;
    private boolean shootRequest = false;
    private long lastShootTime = 0;
    private static final long SHOOT_COOLDOWN_MS = 1000;

    // Dash ability
    protected boolean isDashing = false;
    private Point dashTarget;
    private long lastDashTime = 0;
    private static final long DASH_COOLDOWN_MS = 5000;
    private Image dashLeftImage, dashRightImage;
    private String dashDirection; // "left" or "right"

    protected int mouseX, mouseY;
    private Image background;
    private Thread thread;
    boolean gameRunning = true;

    // Direction constants
    private static final int DIR_EAST = 0;
    private static final int DIR_NORTH_EAST = 1;
    private static final int DIR_NORTH = 2;
    private static final int DIR_NORTH_WEST = 3;
    private static final int DIR_WEST = 4;
    private static final int DIR_SOUTH_WEST = 5;
    private static final int DIR_SOUTH = 6;
    private static final int DIR_SOUTH_EAST = 7;
    
    // ---------- Indicator -------------
    // Target indicator
    private Point targetIndicatorPoint;
    private long targetIndicatorTime;
    private static final long INDICATOR_DURATION = 1000; // milliseconds
    
    // ---------- Monster inner class ----------
    private class Monster {
        double x, y;
        int width, height;
        int type;
        int direction;
        double speed;

        Monster(double x, double y, int type, double speed) {
            this.x = x;
            this.y = y;
            this.width = MONSTER_SIZE;
            this.height = MONSTER_SIZE;
            this.type = type;
            this.direction = DIR_SOUTH;
            this.speed = speed;
        }

        void update() {
            double targetX = avatarX + avatarWidth/2.0;
            double targetY = avatarY + avatarHeight/2.0;
            double dx = targetX - (x + width/2.0);
            double dy = targetY - (y + height/2.0);
            double distance = Math.hypot(dx, dy);
            if (distance > 0.01) {
                double stepX = (dx / distance) * speed;
                double stepY = (dy / distance) * speed;
                x += stepX;
                y += stepY;
                updateDirection(stepX, stepY);
            }
        }

        void updateDirection(double dx, double dy) {
            if (dx == 0 && dy == 0) return;
            double correctedDy = -dy;
            double angle = Math.toDegrees(Math.atan2(correctedDy, dx));
            if (angle < 0) angle += 360;
            if (angle >= 337.5 || angle < 22.5) direction = DIR_EAST;
            else if (angle >= 22.5 && angle < 67.5) direction = DIR_NORTH_EAST;
            else if (angle >= 67.5 && angle < 112.5) direction = DIR_NORTH;
            else if (angle >= 112.5 && angle < 157.5) direction = DIR_NORTH_WEST;
            else if (angle >= 157.5 && angle < 202.5) direction = DIR_WEST;
            else if (angle >= 202.5 && angle < 247.5) direction = DIR_SOUTH_WEST;
            else if (angle >= 247.5 && angle < 292.5) direction = DIR_SOUTH;
            else direction = DIR_SOUTH_EAST;
        }

        Image getCurrentImage() {
            return monsterImages[type - 1][direction];
        }

        void draw(Graphics2D g2d) {
            g2d.drawImage(getCurrentImage(), (int)x, (int)y, width, height, null);
        }

        Rectangle getBounds() {
            return new Rectangle((int)x, (int)y, width, height);
        }
    }

    // ---------- Projectile inner class ----------
    private class Projectile {
        double x, y;
        double velX, velY;
        double distanceTraveled = 0;
        final double maxDistance;
        int width, height;

        Projectile(double x, double y, int width, int height, double velX, double velY, double maxDistance) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.velX = velX;
            this.velY = velY;
            this.maxDistance = maxDistance;
        }

        void update() {
            double step = Math.hypot(velX, velY);
            distanceTraveled += step;
            if (distanceTraveled >= maxDistance) {
                active = false;
                return;
            }
            x += velX;
            y += velY;
            if (x + width < 0 || x > panelWidth || y + height < 0 || y > panelHeight) {
                active = false;
            }
        }

        boolean active = true;
        void draw(Graphics2D g2d) { g2d.drawImage(lightningImage, (int)x, (int)y, width, height, null); }
        Rectangle getBounds() { return new Rectangle((int)x, (int)y, width, height); }
    }

    // ---------- Constructor ----------
    GamePanel() {
        this.setPreferredSize(new Dimension(panelWidth, panelHeight));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        
        // Inside GamePanel() constructor, after loading other images
        try {
            Image cursorImg = new ImageIcon(getClass().getResource("/images/cursor.png")).getImage();
            setCursor(Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0,0), "gameCursor"));
        } catch (HeadlessException | IndexOutOfBoundsException e) {
            // fallback to default cursor if image missing
            setCursor(Cursor.getDefaultCursor());
        }
        
        // Load avatar images
        avatar_east = new ImageIcon(getClass().getResource("/images/east.png")).getImage();
        avatar_north = new ImageIcon(getClass().getResource("/images/north.png")).getImage();
        avatar_northEast = new ImageIcon(getClass().getResource("/images/north-east.png")).getImage();
        avatar_northWest = new ImageIcon(getClass().getResource("/images/north-west.png")).getImage();
        avatar_south = new ImageIcon(getClass().getResource("/images/south.png")).getImage();
        avatar_southEast = new ImageIcon(getClass().getResource("/images/south-east.png")).getImage();
        avatar_southWest = new ImageIcon(getClass().getResource("/images/south-west.png")).getImage();
        avatar_west = new ImageIcon(getClass().getResource("/images/west.png")).getImage();

        // Load monster images
        monsterImages = new Image[3][8];
        // Monster 1
        monsterImages[0][DIR_EAST] = new ImageIcon(getClass().getResource("/images/monster1-east.png")).getImage();
        monsterImages[0][DIR_NORTH_EAST] = new ImageIcon(getClass().getResource("/images/monster1-north-east.png")).getImage();
        monsterImages[0][DIR_NORTH] = new ImageIcon(getClass().getResource("/images/monster1-north.png")).getImage();
        monsterImages[0][DIR_NORTH_WEST] = new ImageIcon(getClass().getResource("/images/monster1-north-west.png")).getImage();
        monsterImages[0][DIR_WEST] = new ImageIcon(getClass().getResource("/images/monster1-west.png")).getImage();
        monsterImages[0][DIR_SOUTH_WEST] = new ImageIcon(getClass().getResource("/images/monster1-south-west.png")).getImage();
        monsterImages[0][DIR_SOUTH] = new ImageIcon(getClass().getResource("/images/monster1-south.png")).getImage();
        monsterImages[0][DIR_SOUTH_EAST] = new ImageIcon(getClass().getResource("/images/monster1-south-east.png")).getImage();
        // Monster 2
        monsterImages[1][DIR_EAST] = new ImageIcon(getClass().getResource("/images/monster2-east.png")).getImage();
        monsterImages[1][DIR_NORTH_EAST] = new ImageIcon(getClass().getResource("/images/monster2-north-east.png")).getImage();
        monsterImages[1][DIR_NORTH] = new ImageIcon(getClass().getResource("/images/monster2-north.png")).getImage();
        monsterImages[1][DIR_NORTH_WEST] = new ImageIcon(getClass().getResource("/images/monster2-north-west.png")).getImage();
        monsterImages[1][DIR_WEST] = new ImageIcon(getClass().getResource("/images/monster2-west.png")).getImage();
        monsterImages[1][DIR_SOUTH_WEST] = new ImageIcon(getClass().getResource("/images/monster2-south-west.png")).getImage();
        monsterImages[1][DIR_SOUTH] = new ImageIcon(getClass().getResource("/images/monster2-south.png")).getImage();
        monsterImages[1][DIR_SOUTH_EAST] = new ImageIcon(getClass().getResource("/images/monster2-south-east.png")).getImage();
        // Monster 3
        monsterImages[2][DIR_EAST] = new ImageIcon(getClass().getResource("/images/monster3-east.png")).getImage();
        monsterImages[2][DIR_NORTH_EAST] = new ImageIcon(getClass().getResource("/images/monster3-north-east.png")).getImage();
        monsterImages[2][DIR_NORTH] = new ImageIcon(getClass().getResource("/images/monster3-north.png")).getImage();
        monsterImages[2][DIR_NORTH_WEST] = new ImageIcon(getClass().getResource("/images/monster3-north-west.png")).getImage();
        monsterImages[2][DIR_WEST] = new ImageIcon(getClass().getResource("/images/monster3-west.png")).getImage();
        monsterImages[2][DIR_SOUTH_WEST] = new ImageIcon(getClass().getResource("/images/monster3-south-west.png")).getImage();
        monsterImages[2][DIR_SOUTH] = new ImageIcon(getClass().getResource("/images/monster3-south.png")).getImage();
        monsterImages[2][DIR_SOUTH_EAST] = new ImageIcon(getClass().getResource("/images/monster3-south-east.png")).getImage();

        background = new ImageIcon(getClass().getResource("/images/arcane.jpg")).getImage();
        lightningImage = new ImageIcon(getClass().getResource("/images/lightning.png")).getImage();

        // Load dash effect images
        dashLeftImage = new ImageIcon(getClass().getResource("/images/dash to left.png")).getImage();
        dashRightImage = new ImageIcon(getClass().getResource("/images/dash to right.png")).getImage();

        avatarX = centerX - avatarWidth/2;
        avatarY = centerY - avatarHeight/2;
        currentAvatarImage = avatar_south;

        projectiles = new ArrayList<>();
        monsters = new ArrayList<>();
        random = new Random();
        mouseX = avatarX + avatarWidth/2;
        mouseY = avatarY + avatarHeight/2;

        currentMonsterSpeed = monsterBaseSpeed;
        spawnDelay = 120;
        spawnTimer = 60;
    }

    // ---------- Dash ability ----------
    public void requestDash() {
        long now = System.currentTimeMillis();
        if (now - lastDashTime < DASH_COOLDOWN_MS) return;
        if (isDashing) return;

        double centerX = avatarX + avatarWidth/2.0;
        double centerY = avatarY + avatarHeight/2.0;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.hypot(dx, dy);
        double maxDistance = 150;

        if (distance > maxDistance) {
            dx = dx * maxDistance / distance;
            dy = dy * maxDistance / distance;
        }
        int targetCenterX = (int) (centerX + dx);
        int targetCenterY = (int) (centerY + dy);
        int targetX = targetCenterX - avatarWidth/2;
        int targetY = targetCenterY - avatarHeight/2;

        dashTarget = new Point(targetX, targetY);
        isDashing = true;
        lastDashTime = now;
        
        if (dx > 0) dashDirection = "right";
        else dashDirection = "left";

        // Stop normal movement if this panel is a MouseControls instance
        if (this instanceof MouseControls) {
            ((MouseControls) this).stopMoving();
        }
    }

    protected void setAvatarPosition(int x, int y) {
        avatarX = x;
        avatarY = y;
    }

    // ---------- Background music (optional) ----------
    public void playBackgroundMusic(float volume) {
        try {
            java.net.URL audioUrl = getClass().getResource("/images/pokemonSE.wav");
            if (audioUrl == null) return;
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioUrl);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            if (volume >= 0 && volume <= 1) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
                gain.setValue(dB);
            }
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    // ---------- Difficulty and game logic ----------
    private void updateDifficulty() {
        int s = score;
        if (s >= 200) {
            spawnDelay = 10;
            currentMonsterSpeed = 5;
        } else if (s >= 150) {
            spawnDelay = 15;
            currentMonsterSpeed = 4.5;
        } else if (s >= 100) {
            spawnDelay = 20;
            currentMonsterSpeed = 4;
        } else if (s >= 70) {
            spawnDelay = 25;
            currentMonsterSpeed = 3.8;
        } else if (s >= 50) {
            spawnDelay = 30;
            currentMonsterSpeed = 3.2;
        } else if (s >= 30) {
            spawnDelay = 35;
            currentMonsterSpeed = 3;
        } else {
            spawnDelay = 120 - (s / 2);
            if (spawnDelay < 35) spawnDelay = 35;
            currentMonsterSpeed = monsterBaseSpeed + (s / 30.0) * 1.3;
            if (currentMonsterSpeed > 3.8) currentMonsterSpeed = 3.8;
        }
        if (spawnDelay < 5) spawnDelay = 5;
    }

    public void startThread() {
        thread = new Thread(this);
        thread.start();
    }

    public void requestShoot() {
        shootRequest = true;
    }

    public int endGameAndGetScore() {
        gameRunning = false;
        return score;
    }

    public void setDirectionToPoint(int targetX, int targetY) {
        double centerX = avatarX + avatarWidth/2.0;
        double centerY = avatarY + avatarHeight/2.0;
        double dx = targetX - centerX;
        double dy = targetY - centerY;
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

    private void shoot() {
        long now = System.currentTimeMillis();
        if (now - lastShootTime < SHOOT_COOLDOWN_MS) return;
        setDirectionToPoint(mouseX, mouseY);
        double centerX = avatarX + avatarWidth/2.0;
        double centerY = avatarY + avatarHeight/2.0;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double length = Math.hypot(dx, dy);
        if (length < 0.001) return;
        double dirX = dx / length;
        double dirY = dy / length;
        double startX = centerX + dirX * (avatarWidth/2.0 + 1);
        double startY = centerY + dirY * (avatarHeight/2.0 + 1);
        int pWidth = 40, pHeight = 40;
        startX -= pWidth/2.0;
        startY -= pHeight/2.0;
        double maxDistance = 2.0 * avatarHeight;
        double speed = maxDistance / 60.0;
        double velX = dirX * speed;
        double velY = dirY * speed;
        projectiles.add(new Projectile(startX, startY, pWidth, pHeight, velX, velY, maxDistance));
        lastShootTime = now;
    }

    private void spawnMonster() {
        int side = random.nextInt(4);
        double x, y;
        if (side == 0) {
            x = random.nextInt(panelWidth - MONSTER_SIZE);
            y = -MONSTER_SIZE;
        } else if (side == 1) {
            x = panelWidth;
            y = random.nextInt(panelHeight - MONSTER_SIZE);
        } else if (side == 2) {
            x = random.nextInt(panelWidth - MONSTER_SIZE);
            y = panelHeight;
        } else {
            x = -MONSTER_SIZE;
            y = random.nextInt(panelHeight - MONSTER_SIZE);
        }
        int type = random.nextInt(3) + 1;
        monsters.add(new Monster(x, y, type, currentMonsterSpeed));
    }

    private void checkCollisions() {
        Rectangle avatarBounds = new Rectangle(avatarX, avatarY, avatarWidth, avatarHeight);
        double halfAvatarArea = (avatarWidth * avatarHeight) / 1.5;
        for (Monster m : monsters) {
            Rectangle monsterBounds = m.getBounds();
            Rectangle intersection = avatarBounds.intersection(monsterBounds);
            if (!intersection.isEmpty()) {
                double overlapArea = intersection.width * intersection.height;
                if (overlapArea >= halfAvatarArea) {
                    gameRunning = false;
                    break;
                }
            }
        }
    }

    private void checkProjectileCollisions() {
        Iterator<Projectile> pIt = projectiles.iterator();
        while (pIt.hasNext()) {
            Projectile p = pIt.next();
            Rectangle pBounds = p.getBounds();
            Iterator<Monster> mIt = monsters.iterator();
            while (mIt.hasNext()) {
                Monster m = mIt.next();
                if (pBounds.intersects(m.getBounds())) {
                    score += 5;
                    updateDifficulty();
                    lastShootTime = 0;
                    pIt.remove();
                    mIt.remove();
                    break;
                }
            }
        }
    }
    
    // --------- Set Target Indicator ----------
    public void setTargetIndicator(Point point) {
        targetIndicatorPoint = point;
        targetIndicatorTime = System.currentTimeMillis();
    }
    
    // ---------- Paint ----------
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(background, 0, 0, null);
        g2d.drawImage(currentAvatarImage, avatarX, avatarY, avatarWidth, avatarHeight, null);
        
        if (isDashing) {
            int effectWidth = avatarWidth;      // same height as avatar
            int effectHeight = avatarHeight;
            int overlap = 45;                   // pixels overlapping the avatar

            if (dashDirection.equals("right")) {
                // Draw on the right side, overlapping the avatar's right edge
                int x = avatarX - overlap;
                g2d.drawImage(dashRightImage, x, avatarY, effectWidth, effectHeight, null);
            } else {
                // Draw on the left side, overlapping the avatar's left edge
                int x = avatarX + overlap;
                g2d.drawImage(dashLeftImage, x, avatarY, effectWidth, effectHeight, null);
            }
        }

        for (Monster m : monsters) m.draw(g2d);
        for (Projectile p : projectiles) p.draw(g2d);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Score: " + score, 20, 40);
        if (!gameRunning) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            String msg = "GAME OVER - Final Score: " + score;
            FontMetrics fm = g2d.getFontMetrics();
            int x = (panelWidth - fm.stringWidth(msg)) / 2;
            int y = panelHeight / 2;
            g2d.drawString(msg, x, y);
        }
        
        // Draw target indicator (green arrow)
        if (targetIndicatorPoint != null) {
            long elapsed = System.currentTimeMillis() - targetIndicatorTime;
            if (elapsed < INDICATOR_DURATION) {
                int x = targetIndicatorPoint.x;
                int y = targetIndicatorPoint.y;

                // Draw a green arrow pointing DOWN
                g2d.setColor(new Color(0, 255, 0, 180));
                int[] xPoints = {x, x - 10, x - 5, x - 5, x + 5, x + 5, x + 10};
                int[] yPoints = {y + 15, y + 5, y + 5, y - 5, y - 5, y + 5, y + 5};
                g2d.fillPolygon(xPoints, yPoints, 7);

                g2d.fillOval(x - 8, y - 8, 16, 16);
            } else {
                targetIndicatorPoint = null; // clear after duration
            }
        }
        
    }

    // ---------- Game loop ----------
    @Override
    public void run() {
        while (thread != null) {
            if (gameRunning) update();
            repaint();
            try { Thread.sleep(16); } catch (InterruptedException e) {}
        }
    }

    public void update() {
        // Shooting
        if (shootRequest) {
            shoot();
            shootRequest = false;
        }

        // Dash movement (takes priority)
        if (isDashing && dashTarget != null) {
            double dx = dashTarget.x - avatarX;
            double dy = dashTarget.y - avatarY;
            double distance = Math.hypot(dx, dy);
            if (distance <= 6) {
                setAvatarPosition(dashTarget.x, dashTarget.y);
                isDashing = false;
                dashTarget = null;
            } else {
                double stepX = (dx / distance) * 6;
                double stepY = (dy / distance) * 6;
                setAvatarPosition(avatarX + (int) stepX, avatarY + (int) stepY);
            }
        }

        // Update projectiles
        Iterator<Projectile> pIt = projectiles.iterator();
        while (pIt.hasNext()) {
            Projectile p = pIt.next();
            p.update();
            if (!p.active) pIt.remove();
        }

        // Update monsters
        for (Monster m : monsters) m.update();

        // Spawn monsters
        spawnTimer--;
        if (spawnTimer <= 0) {
            spawnMonster();
            spawnTimer = spawnDelay;
        }

        checkCollisions();
        checkProjectileCollisions();
    }
    
    // Reset Game Window Configuration
    public void resetGame() {
        // Reset game state
        gameRunning = true;
        score = 0;
        monsters.clear();
        projectiles.clear();
        lastShootTime = 0;
        lastDashTime = 0;
        isDashing = false;
        dashTarget = null;
        shootRequest = false;

        // Reset avatar position to center (feet at center of screen)
        avatarX = centerX - avatarWidth/2;
        avatarY = centerY - avatarHeight;
        if (this instanceof MouseControls) {
            ((MouseControls) this).resetPosition(avatarX, avatarY);
        }

        // Reset difficulty
        currentMonsterSpeed = monsterBaseSpeed;
        spawnDelay = 120;
        spawnTimer = 60;

        // Clear target indicator
        targetIndicatorPoint = null;

        // Ensure mouse position is updated
        mouseX = avatarX + avatarWidth/2;
        mouseY = avatarY + avatarHeight;
    }
    
}