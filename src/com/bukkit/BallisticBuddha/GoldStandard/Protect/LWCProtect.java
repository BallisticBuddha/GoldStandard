package com.bukkit.BallisticBuddha.GoldStandard.Protect;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.bukkit.BallisticBuddha.GoldStandard.GoldStandard;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;

public class LWCProtect extends ContainerProtect {
	
	LWC lwc = null;
	
	public LWCProtect(GoldStandard instance) {
		super(instance);
		initialize();
	}
	private void initialize(){
		Plugin lwcPlugin = gs.getServer().getPluginManager().getPlugin("LWC");
		if(lwcPlugin != null) {
		    lwc = ((LWCPlugin) lwcPlugin).getLWC();
		}
		else{
			log.info("[GoldStandard] An error occurred while hooking into LWC (is it installed?), block protection is disabled.");
			enabled = false;
		}
	}

	@Override
	public boolean isProtected(Block block) {
		if (enabled){
			Protection protection = lwc.findProtection(block);
			if(protection != null)
				return true;
		}
		return false;
	}

	@Override
	public String getBlockOwner(Block block) {
		if (isProtected(block)){
			Protection protection = lwc.findProtection(block);
			return protection.getOwner();
		}
		return null;
	}
	@Override
	public boolean canSellFrom(Player player, Block block) {
		if (!(player instanceof Player))
			return false;
		if (!isProtected(block))
			return true;
		
		Protection protection = lwc.findProtection(block);
		boolean canAccess = lwc.canAccessProtection(player, protection);
		if (canAccess)
			return true;
		else if (player.getName().equalsIgnoreCase(getBlockOwner(block)))
			return true;
		
		return false;
	}
}
