package com.bukkit.BallisticBuddha.GoldStandard;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class NoneProtect extends ContainerProtect {

	public NoneProtect(GoldStandard instance) {
		super(instance);
		enabled = true;
	}

	@Override
	public boolean isProtected(Block block) {
		return false;
	}
	@Override
	public String getBlockOwner(Block block) {
		return null;
	}
	@Override
	public boolean canSellFrom(Player player, Block block) {
		if (player instanceof Player)
			return true;
		return false;
	}
}