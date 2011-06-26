package com.bukkit.BallisticBuddha.GoldStandard;

import java.util.logging.Logger;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;


	public class GSPlayerListener extends PlayerListener{
		public static GoldStandard gs;
		protected static Logger log = Logger.getLogger("Minecraft");
		public GSPlayerListener(GoldStandard instance){
			gs = instance;
		}

		@Override
		public void onPlayerJoin(PlayerJoinEvent event) {
			if (!gs.getCalc().loadPlayer(event.getPlayer().getName())){
				gs.getCalc().addPlayer(event.getPlayer().getName());
				log.info("[GoldStandard] Adding new user " + event.getPlayer().getName());
			}
		}
		@Override
		public void onPlayerQuit(PlayerQuitEvent event) {
			gs.getCalc().storePlayer(event.getPlayer().getName());
		}
}
