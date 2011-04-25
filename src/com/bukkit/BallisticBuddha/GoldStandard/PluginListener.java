package com.bukkit.BallisticBuddha.GoldStandard;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

import com.nijiko.coelho.iConomy.iConomy;
import org.bukkit.plugin.Plugin;

/**
 * Checks for plugins whenever one is enabled
 */
public class PluginListener extends ServerListener {
    public PluginListener() { }

    @Override
    public void onPluginEnable (PluginEnableEvent event) {
        if(GoldStandard.getiConomy() == null) {
            Plugin iConomy = GoldStandard.getBukkitServer().getPluginManager().getPlugin("iConomy");

            if (iConomy != null) {
                if(iConomy.isEnabled()) {
                    GoldStandard.setiConomy((iConomy)iConomy);
                    System.out.println("[GoldStandard] Successfully linked with iConomy.");
                }
            }
        }
    }
}