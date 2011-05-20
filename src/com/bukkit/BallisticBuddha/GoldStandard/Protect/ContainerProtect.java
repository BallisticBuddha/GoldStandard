package com.bukkit.BallisticBuddha.GoldStandard.Protect;

import java.util.logging.Logger;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.bukkit.BallisticBuddha.GoldStandard.GoldStandard;


public abstract class ContainerProtect {
	protected GoldStandard gs = null;
	protected boolean enabled = false;
	protected static Logger log = Logger.getLogger("Minecraft");
	
	public ContainerProtect(GoldStandard instance){
		this.gs = instance;	
	}
	
	public void setProtectionStatus(boolean toSet){
		enabled = toSet;
	}
	public boolean getProtectionStatus(){
		return enabled;
	}
	
	public abstract boolean isProtected(Block block);
	public abstract String getBlockOwner(Block block);
	public abstract boolean canSellFrom(Player player, Block block);
}
