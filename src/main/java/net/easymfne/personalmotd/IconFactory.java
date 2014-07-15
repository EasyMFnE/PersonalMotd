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

import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import org.bukkit.scheduler.BukkitRunnable;

public class IconFactory extends BukkitRunnable {
    
    /**
     * Runnable class for scheduling logger actions from an asynchronous thread.
     */
    private class RunnableLogger extends BukkitRunnable {
        private PersonalMotd plugin;
        private String message;
        
        public RunnableLogger(PersonalMotd plugin, String message) {
            this.plugin = plugin;
            this.message = message;
        }
        
        @Override
        public void run() {
            plugin.fancyLog(message);
        }
    }
    
    /**
     * @param img1
     * @param img2
     * @return Whether the two images are different
     */
    private static boolean different(BufferedImage img1, BufferedImage img2) {
        if (img1 == null || img2 == null
                || img1.getHeight() != img2.getHeight()
                || img1.getWidth() != img2.getWidth()) {
            return true;
        }
        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return true;
                }
            }
        }
        return false;
        
    }
    
    /**
     * Create a new asynchronous task for generating a personalized icon.
     * 
     * @param playerId
     *            Player to generate icon for
     */
    public static void generateIcon(PersonalMotd plugin, String playerId) {
        plugin.fancyLog("Scheduling icon creation task for " + playerId);
        plugin.getServer()
                .getScheduler()
                .runTaskAsynchronously(plugin,
                        new IconFactory(plugin, playerId));
    }
    
    /**
     * @param plugin
     *            Plugin reference
     * @param skin
     *            Skin image
     * @return Subimage containing the face texture
     */
    private static BufferedImage getFace(PersonalMotd plugin, BufferedImage skin) {
        int[] val = plugin.getConf().getFaceSkinLocation();
        return skin.getSubimage(val[0], val[1], val[2], val[3]);
    }
    
    /**
     * @param plugin
     *            Plugin reference
     * @param skin
     *            Skin image
     * @return Subimage containing the hat (front) texture
     */
    private static BufferedImage getHat(PersonalMotd plugin, BufferedImage skin) {
        int[] val = plugin.getConf().getHatSkinLocation();
        return skin.getSubimage(val[0], val[1], val[2], val[3]);
    }
    
    /**
     * @param face
     *            Face image
     * @param hat
     *            Hat (front) image
     * @return Combined image (hat overlayed on face)
     */
    private static BufferedImage getHead(BufferedImage face, BufferedImage hat) {
        int w = Math.max(face.getWidth(), hat.getWidth());
        int h = Math.max(face.getHeight(), hat.getHeight());
        BufferedImage head = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = head.getGraphics();
        g.drawImage(face, 0, 0, null);
        g.drawImage(hat, 0, 0, null);
        return head;
    }
    
    /**
     * @param plugin
     *            Plugin reference
     * @param base
     *            Base icon image
     * @param head
     *            Customized head image
     * @return Finished icon image with head overlaid on base
     */
    private static BufferedImage overlayHead(PersonalMotd plugin,
            BufferedImage base, BufferedImage head) {
        int[] val = plugin.getConf().getHeadTransforms();
        head = scaleAndRotate(head, val[0], val[1]);
        BufferedImage icon = new BufferedImage(base.getWidth(),
                base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = icon.getGraphics();
        g.drawImage(base, 0, 0, null);
        g.drawImage(head, val[2], val[3], null);
        return icon;
    }
    
    /**
     * @param image
     *            Image to manipulate
     * @param scale
     *            Scaling factor
     * @param rotate
     *            Number of quadrants to rotate by
     * @return Modified image
     */
    private static BufferedImage scaleAndRotate(BufferedImage image, int scale,
            int rotate) {
        AffineTransform scaleTransform = AffineTransform.getScaleInstance(
                scale, scale);
        AffineTransformOp scaleOp = new AffineTransformOp(scaleTransform,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        AffineTransform rotateTransform = AffineTransform
                .getQuadrantRotateInstance(rotate, image.getWidth() / 2,
                        image.getHeight() / 2);
        AffineTransformOp rotateOp = new AffineTransformOp(rotateTransform,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        image = rotateOp.filter(image, null);
        image = scaleOp.filter(image, null);
        return image;
    }
    
    private PersonalMotd plugin;
    
    private String playerId;
    
    private IconFactory(PersonalMotd plugin, String playerId) {
        this.plugin = plugin;
        this.playerId = playerId;
    }
    
    /**
     * Asynchronously runnable task for checking player skins and generation of
     * new personalized icon if necessary.
     */
    @Override
    public void run() {
        BufferedImage cachedSkin = plugin.getConf().getCachedPlayerSkin(
                playerId);
        BufferedImage fetchedSkin = plugin.getConf().getFetchedPlayerSkin(
                playerId);
        if (fetchedSkin == null) {
            plugin.getServer()
                    .getScheduler()
                    .runTask(
                            plugin,
                            new RunnableLogger(plugin, "No skin online for "
                                    + playerId));
            return;
        }
        if (cachedSkin == null || different(cachedSkin, fetchedSkin)) {
            plugin.getConf().savePlayerSkin(playerId, fetchedSkin);
            BufferedImage head = getHead(getFace(plugin, fetchedSkin),
                    getHat(plugin, fetchedSkin));
            BufferedImage icon = overlayHead(plugin, plugin.getConf()
                    .getDefaultImage(), head);
            plugin.getConf().savePersonalizedIcon(playerId, icon);
            plugin.getServer()
                    .getScheduler()
                    .runTask(
                            plugin,
                            new RunnableLogger(plugin, "Icon generated for "
                                    + playerId));
        }
    }
    
}
