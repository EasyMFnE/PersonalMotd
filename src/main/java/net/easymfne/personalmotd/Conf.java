/*
 * This file is part of the PersonalMotd plugin by EasyMFnE.
 * 
 * PersonalMotd is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 * 
 * PersonalMotd is distributed in the hope that it will be useful, but without
 * any warranty; without even the implied warranty of merchantability or fitness
 * for a particular purpose. See the GNU General Public License for details.
 * 
 * You should have received a copy of the GNU General Public License v3 along
 * with PersonalMotd. If not, see <http://www.gnu.org/licenses/>.
 */
package net.easymfne.personalmotd;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.bukkit.util.CachedServerIcon;

/**
 * Configuration helper class, with methods for accessing the configuration.
 */
public class Conf {
    
    private PersonalMotd plugin = null;
    
    private BufferedImage defaultImage;
    private CachedServerIcon defaultIcon;
    private File skinFolder;
    private File iconFolder;
    
    /**
     * Instantiate the class and give it a reference back to the plugin itself.
     * 
     * @param plugin
     *            The PersonalMotd plugin
     */
    public Conf(PersonalMotd plugin) {
        this.plugin = plugin;
        skinFolder = new File(plugin.getDataFolder(), "player-skins");
        if (!skinFolder.exists()) {
            skinFolder.mkdirs();
        }
        iconFolder = new File(plugin.getDataFolder(), "personal-icons");
        if (!iconFolder.exists()) {
            iconFolder.mkdirs();
        }
        load();
    }
    
    /**
     * @param playerId
     *            Player to fetched cached skin for
     * @return Image of skin, or null in none cached
     */
    public BufferedImage getCachedPlayerSkin(String playerId) {
        File skin = new File(skinFolder, playerId + ".png");
        try {
            return ImageIO.read(skin);
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * @return Default server icon
     */
    public CachedServerIcon getDefaultIcon() {
        if (defaultIcon != null) {
            return defaultIcon;
        }
        return plugin.getServer().getServerIcon();
    }
    
    /**
     * @return Default server icon image
     */
    public BufferedImage getDefaultImage() {
        ColorModel cm = defaultImage.getColorModel();
        boolean isAlphaPremultiplied = defaultImage.isAlphaPremultiplied();
        WritableRaster raster = defaultImage.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
    
    /**
     * @return Integer array {x, y, w, h}
     */
    public int[] getFaceSkinLocation() {
        int[] values = new int[4];
        values[0] = plugin.getConfig().getInt("skin-face-location.x", 8);
        values[1] = plugin.getConfig().getInt("skin-face-location.y", 8);
        values[2] = plugin.getConfig().getInt("skin-face-location.w", 8);
        values[3] = plugin.getConfig().getInt("skin-face-location.h", 8);
        return values;
    }
    
    /**
     * @param playerId
     *            Player name
     * @return Skin image fetched from configured URL, or null if not found
     */
    public BufferedImage getFetchedPlayerSkin(String playerId) {
        try {
            URL skinUrl = new URL(
                    plugin.getConfig()
                            .getString("skin-url",
                                    "http://s3.amazonaws.com/MinecraftSkins/{PLAYERNAME}.png")
                            .replace("{PLAYERNAME}", playerId));
            return ImageIO.read(skinUrl);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * @return Integer array {x, y, w, h}
     */
    public int[] getHatSkinLocation() {
        int[] values = new int[4];
        values[0] = plugin.getConfig().getInt("skin-hat-location.x", 40);
        values[1] = plugin.getConfig().getInt("skin-hat-location.y", 8);
        values[2] = plugin.getConfig().getInt("skin-hat-location.w", 8);
        values[3] = plugin.getConfig().getInt("skin-hat-location.h", 8);
        return values;
    }
    
    /**
     * @return Integer array {scale, rotate, shift-x, shift-y}
     */
    public int[] getHeadTransforms() {
        int[] values = new int[4];
        values[0] = plugin.getConfig().getInt("head-transform.scale", 4);
        values[1] = plugin.getConfig().getInt("head-transform.rotate", 0);
        values[2] = plugin.getConfig().getInt("head-transform.shift.x", 0);
        values[3] = plugin.getConfig().getInt("head-transform.shift.y", 0);
        return values;
    }
    
    /**
     * @return Desired icon-selection mode
     */
    public IconMode getIconMode() {
        return IconMode.valueOf(plugin.getConfig().getString("icon-mode",
                "PLAYER"));
    }
    
    /**
     * @return Default text to replace placeholder with when no name found
     */
    public String getNameTagDefault() {
        return plugin.getConfig().getString("name-tag-default", "Guest");
    }
    
    /**
     * @return Placeholder used for name substitutions
     */
    public String getNameTagPlaceholder() {
        return plugin.getConfig().getString("name-tag-placeholder", "{PLAYER}");
    }
    
    /**
     * @param playerId
     *            Player
     * @return Personalized icon, or null if none exists
     */
    public CachedServerIcon getPersonalizedIcon(String playerId) {
        if (playerId == null) {
            return null;
        }
        File icon = new File(iconFolder, playerId + ".png");
        if (icon.exists()) {
            try {
                return plugin.getServer().loadServerIcon(icon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    /**
     * Fetch and cache the base icon for the server list.
     */
    private void load() {
        File defaultFile = new File(plugin.getConfig().getString("base-icon",
                "server-icon.png"));
        try {
            defaultImage = ImageIO.read(defaultFile);
            defaultIcon = plugin.getServer().loadServerIcon(defaultFile);
        } catch (Exception e) {
            plugin.fancyLog(Level.SEVERE, "Error loading base server icon! {"
                    + defaultFile.getAbsolutePath() + "}");
        }
    }
    
    /**
     * Reload all cached values/images.
     */
    public void reload() {
        load();
    }
    
    /**
     * @param playerId
     *            Player
     * @param icon
     *            Icon image to save
     * @return Whether the image was saved
     */
    public boolean savePersonalizedIcon(String playerId, BufferedImage icon) {
        File iconFile = new File(iconFolder, playerId + ".png");
        try {
            ImageIO.write(icon, "PNG", iconFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * @param playerId
     *            Name associated with the skin
     * @param skin
     *            The player skin image
     * @return Whether the image was saved
     */
    public boolean savePlayerSkin(String playerId, BufferedImage skin) {
        File skinFile = new File(skinFolder, playerId + ".png");
        try {
            ImageIO.write(skin, "PNG", skinFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
}
