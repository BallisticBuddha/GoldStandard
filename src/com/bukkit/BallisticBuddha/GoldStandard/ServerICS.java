package com.bukkit.BallisticBuddha.GoldStandard;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

import com.iConomy.*;
import org.bukkit.plugin.Plugin;

/**
 * Checks for plugins whenever one is enabled
 */
public class ServerICS extends ServerListener {
    private GoldStandard plugin;

    public ServerICS(GoldStandard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginDisable(PluginDisableEvent event) {
    	if (plugin.iConomy != null) {
            if (event.getPlugin().getDescription().getName().equals("iConomy")) {
                plugin.iConomy = null;
                System.out.println("[GoldStandard] un-hooked from iConomy.");
            }
        }
    }

    @Override
    public void onPluginEnable(PluginEnableEvent event) {
        if (plugin.iConomy == null) {
            Plugin iConomy = plugin.getServer().getPluginManager().getPlugin("iConomy");

            if (iConomy != null) {
                if (iConomy.isEnabled() && iConomy.getClass().getName().equals("com.iConomy.iConomy")) {
                    plugin.iConomy = (iConomy)iConomy;
                    System.out.println("[GoldStandard] hooked into iConomy.");
                }
            }
        }
    }
}