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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.MetricsLite;

/**
 * Main plugin class, responsible for its own setup, logging, reloading, and
 * shutdown operations. Maintains instances of Conf, CommandHandler, and
 * EventListener.
 * 
 * @author Eric Hildebrand
 */
public class PersonalMotd extends JavaPlugin {
    
    private final File addressMapFile = new File(getDataFolder().getPath()
            + File.separator + "addressmap.yml");
    private Map<InetAddress, String> addressMap;
    
    private Conf conf = null;
    private CommandHandler commandHandler = null;
    private EventListener eventListener = null;
    
    private File baseImage;
    
    /* Strings for fancyLog() methods */
    private final String logPrefix = ChatColor.BLUE + "[PersonalMotd] ";
    
    private final String logColor = ChatColor.YELLOW.toString();
    
    /**
     * Log a message to the console using color, with a specific logging Level.
     * If there is no console open, log the message without any coloration.
     * 
     * @param level
     *            Level at which the message should be logged
     * @param message
     *            The message to be logged
     */
    protected void fancyLog(Level level, String message) {
        if (getServer().getConsoleSender() != null) {
            getServer().getConsoleSender().sendMessage(
                    logPrefix + logColor + message);
        } else {
            getServer().getLogger().log(level,
                    ChatColor.stripColor(logPrefix + message));
        }
    }
    
    /**
     * Log a message to the console using color, defaulting to the Info level.
     * If there is no console open, log the message without any coloration.
     * 
     * @param message
     *            The message to be logged
     */
    protected void fancyLog(String message) {
        fancyLog(Level.INFO, message);
    }
    
    /**
     * @return Map of Address,ID pairs
     */
    public final Map<InetAddress, String> getAddressMap() {
        return addressMap;
    }
    
    /**
     * @return The default/base server icon image
     */
    public final File getBaseImage() {
        return baseImage;
    }
    
    /**
     * @return the configuration helper instance
     */
    public Conf getConf() {
        return conf;
    }
    
    /**
     * Load the saved address map from file.
     */
    private void loadAddressMap() {
        addressMap = new HashMap<InetAddress, String>();
        if (addressMapFile.exists() && addressMapFile.isFile()) {
            YamlConfiguration addressConfig = YamlConfiguration
                    .loadConfiguration(addressMapFile);
            for (String a : addressConfig.getKeys(false)) {
                ConfigurationSection csA = addressConfig
                        .getConfigurationSection(a);
                for (String b : csA.getKeys(false)) {
                    ConfigurationSection csB = csA.getConfigurationSection(b);
                    for (String c : csB.getKeys(false)) {
                        ConfigurationSection csC = csB
                                .getConfigurationSection(c);
                        for (String d : csC.getKeys(false)) {
                            String address = StringUtils.join(new String[] { a,
                                    b, c, d }, ".");
                            String playerId = csC.getString(d);
                            try {
                                addressMap.put(InetAddress.getByName(address),
                                        playerId);
                            } catch (UnknownHostException e) {
                                fancyLog(Level.WARNING, "Unknown host: "
                                        + address + " (" + playerId + ")");
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Close all event handlers and command listeners, then null instances to
     * mark them for garbage collection. Displays elapsed time to console when
     * finished.
     */
    @Override
    public void onDisable() {
        long start = Calendar.getInstance().getTimeInMillis();
        fancyLog("=== DISABLE START ===");
        try {
            saveAddressMap();
        } catch (IOException e) {
            fancyLog(Level.SEVERE, "Failed to save address-map configuration!");
        }
        eventListener.close();
        eventListener = null;
        commandHandler.close();
        commandHandler = null;
        conf = null;
        fancyLog("=== DISABLE COMPLETE ("
                + (Calendar.getInstance().getTimeInMillis() - start)
                + "ms) ===");
    }
    
    /**
     * Set up the plugin by: loading config.yml (creating from default if not
     * existent), then instantiating its own Conf, CommandHandler, and
     * EventListener objects. Displays elapsed time to console when finished.
     */
    @Override
    public void onEnable() {
        long start = Calendar.getInstance().getTimeInMillis();
        fancyLog("=== ENABLE START ===");
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
            fancyLog("Saved default config.yml");
        }
        loadAddressMap();
        conf = new Conf(this);
        commandHandler = new CommandHandler(this);
        eventListener = new EventListener(this);
        startMetrics();
        fancyLog("=== ENABLE COMPLETE ("
                + (Calendar.getInstance().getTimeInMillis() - start)
                + "ms) ===");
    }
    
    /**
     * Reload the configuration from disk and perform any necessary functions.
     * Displays elapsed time to console when finished.
     */
    public void reload() {
        long start = Calendar.getInstance().getTimeInMillis();
        fancyLog("=== RELOAD START ===");
        reloadConfig();
        conf.reload();
        fancyLog("=== RELOAD COMPLETE ("
                + (Calendar.getInstance().getTimeInMillis() - start)
                + "ms) ===");
    }
    
    /**
     * Save the existing address map to file.
     * 
     * @throws IOException
     */
    private void saveAddressMap() throws IOException {
        YamlConfiguration addressConfig = new YamlConfiguration();
        for (Entry<InetAddress, String> entry : addressMap.entrySet()) {
            addressConfig
                    .set(entry.getKey().getHostAddress(), entry.getValue());
        }
        addressConfig.save(addressMapFile);
    }
    
    /**
     * If possible, instantiate Metrics and connect with mcstats.org
     */
    private void startMetrics() {
        MetricsLite metrics;
        try {
            metrics = new MetricsLite(this);
            if (metrics.start()) {
                fancyLog("Metrics enabled.");
            }
        } catch (IOException e) {
            fancyLog(Level.WARNING, "Metrics exception: " + e.getMessage());
        }
    }
    
}
