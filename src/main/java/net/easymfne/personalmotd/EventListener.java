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

import java.util.Iterator;
import java.util.Random;

import net.easymfne.factionsdb.PlayerDeathBanEvent;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

/**
 * The class that monitors and reacts to server events.
 * 
 * @author Eric Hildebrand
 */
public class EventListener implements Listener {
    
    private PersonalMotd plugin;
    private Random random;
    
    private String latestDeath;
    private String latestDeathBan;
    
    /**
     * Instantiate by getting a reference to the plugin instance and registering
     * each of the defined EventHandlers.
     * 
     * @param plugin
     *            Reference to PersonalMotd plugin instance
     */
    public EventListener(PersonalMotd plugin) {
        this.plugin = plugin;
        random = new Random();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Unregister all registered EventHandlers, preventing further reactions.
     */
    public void close() {
        plugin = null;
        random = null;
        HandlerList.unregisterAll(this);
    }
    
    /**
     * @param event
     * @return Random player name from the event's iterator
     */
    private String getRandom(ServerListPingEvent event) {
        Iterator<Player> iter = event.iterator();
        String player = null;
        for (int i = 0; i <= random.nextInt(event.getNumPlayers()); i++) {
            player = iter.next().getName();
        }
        return player;
    }
    
    /**
     * Detect pre-login events for mapping Address->Name pairs and generation of
     * personalized icons.
     * 
     * @param event
     */
    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.getAddressMap().containsKey(event.getAddress())
                || plugin.getAddressMap().get(event.getAddress()) != event
                        .getName()) {
            plugin.getAddressMap().put(event.getAddress(), event.getName());
        }
        IconFactory.generateIcon(plugin, event.getName());
    }
    
    /**
     * Record name of most recently deceased player.
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        latestDeath = event.getEntity().getName();
    }
    
    /**
     * Record name of most recently deathbanned player.
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeathBan(PlayerDeathBanEvent event) {
        latestDeathBan = event.getPlayer().getName();
    }
    
    /**
     * Modify the server icon and MOTD text.
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onServerListPing(ServerListPingEvent event) {
        String playerId = plugin.getAddressMap().get(event.getAddress());
        if (playerId != null) {
            event.setMotd(event.getMotd().replace(
                    plugin.getConf().getNameTagPlaceholder(), playerId));
        } else {
            event.setMotd(event.getMotd().replace(
                    plugin.getConf().getNameTagPlaceholder(),
                    plugin.getConf().getNameTagDefault()));
        }
        CachedServerIcon icon = null;
        switch (plugin.getConf().getIconMode()) {
        case DEATH:
            icon = plugin.getConf().getPersonalizedIcon(latestDeath);
            break;
        case DEATHBAN:
            icon = plugin.getConf().getPersonalizedIcon(latestDeathBan);
            break;
        case PLAYER:
            icon = plugin.getConf().getPersonalizedIcon(playerId);
            break;
        case RANDOM:
            icon = plugin.getConf().getPersonalizedIcon(getRandom(event));
            break;
        }
        if (icon != null) {
            event.setServerIcon(icon);
            return;
        } else {
            event.setServerIcon(plugin.getConf().getDefaultIcon());
        }
    }
    
}
