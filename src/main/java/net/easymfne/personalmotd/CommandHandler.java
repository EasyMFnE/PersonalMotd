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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

/**
 * The class that handles the "/personalmotd" command for the plugin.
 */
public class CommandHandler implements TabExecutor {
    
    private PersonalMotd plugin = null;
    private List<String> subcommands;
    
    /**
     * Instantiate by getting a reference to the plugin instance and registering
     * this class to handle the '/personalmotd' command.
     * 
     * @param plugin
     *            Reference to PersonalMotd plugin instance
     */
    public CommandHandler(PersonalMotd plugin) {
        this.plugin = plugin;
        plugin.getCommand("personalmotd").setExecutor(this);
        subcommands = new ArrayList<String>();
        subcommands.add("addresses");
        subcommands.add("reload");
    }
    
    /**
     * Release the '/personalmotd' command from its ties to this class.
     */
    public void close() {
        plugin.getCommand("personalmotd").setExecutor(null);
    }
    
    /**
     * This method handles user commands. Usage: "/personalmotd"
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        if (args[0].equalsIgnoreCase("addresses")) {
            sender.sendMessage("Stored address mappings: "
                    + plugin.getAddressMap().size());
            for (Entry<InetAddress, String> entry : plugin.getAddressMap()
                    .entrySet()) {
                sender.sendMessage("  " + entry.getKey().getHostAddress()
                        + " -> " + entry.getValue());
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            sender.sendMessage("Configuration reloaded from disk.");
            return true;
        }
        sender.sendMessage("Unknown subcommand: " + args[0]);
        return false;
    }
    
    /**
     * Handle tab-completion using defined list of subcommands.
     */
    @Override
    public List<String> onTabComplete(CommandSender arg0, Command arg1,
            String arg2, String[] arg3) {
        return subcommands;
    }
    
}
