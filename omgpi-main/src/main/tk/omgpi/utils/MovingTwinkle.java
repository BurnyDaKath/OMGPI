package tk.omgpi.utils;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;

/**
 * Use this class to mark stuff with particles.
 */
public class MovingTwinkle {
    /**
     * Register of twinkles
     */
    public static List<MovingTwinkle> twinkles = new LinkedList<>();
    public Location a;
    public Location b;
    public Color c;
    protected Location drawloc;
    protected double iter;
    protected double dist;
    protected Vector move;

    /**
     * Make a new twinkle with specific color which will move from a to b.
     *
     * @param a First point
     * @param b Second point
     * @param c Color of line
     */
    public MovingTwinkle(Location a, Location b, Color c) {
        this.a = a;
        this.b = b;
        this.c = c;
        drawloc = a.clone();
        dist = a.distance(b);
        move = b.toVector().subtract(a.toVector()).multiply(0.5 / dist);
        twinkles.add(this);
    }

    /**
     * Draw the twinkle. This may take long time if there are too many twinkles. Automatically done on each tick by OMGPI.
     */
    public void draw() {
        drawloc.getWorld().spawnParticle(Particle.REDSTONE, drawloc, 0, (c.getRed() + 1) / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1);
        if (iter >= dist * 2) drawloc.subtract(move);
        else drawloc.add(move);
        iter = (iter + 1) % (dist * 4);
    }

    /**
     * Remove the twinkle from register, to draw it you will need to call draw() yourself.
     */
    public void delete() {
        twinkles.remove(this);
    }

    /**
     * Create Color for use from ChatColor.
     *
     * @param c ChatColor to translate
     * @return Color to use in twinkles
     */
    public static Color fromChatColor(ChatColor c) {
        switch (c) {
            case AQUA:
                return Color.fromRGB(0, 255, 255);
            case BLACK:
                return Color.fromRGB(0, 0, 0);
            case BLUE:
                return Color.fromRGB(0, 0, 255);
            case DARK_AQUA:
                return Color.fromRGB(0, 127, 127);
            case DARK_BLUE:
                return Color.fromRGB(0, 0, 127);
            case DARK_GRAY:
                return Color.fromRGB(85, 85, 85);
            case DARK_GREEN:
                return Color.fromRGB(0, 127, 0);
            case DARK_PURPLE:
                return Color.fromRGB(127, 0, 127);
            case DARK_RED:
                return Color.fromRGB(127, 0, 0);
            case GOLD:
                return Color.fromRGB(255, 127, 0);
            case GRAY:
                return Color.fromRGB(170, 170, 170);
            case GREEN:
                return Color.fromRGB(0, 255, 0);
            case LIGHT_PURPLE:
                return Color.fromRGB(255, 0, 255);
            case RED:
                return Color.fromRGB(255, 0, 0);
            case YELLOW:
                return Color.fromRGB(255, 255, 0);
            default:
                return Color.fromRGB(255, 255, 255);
        }
    }
}
