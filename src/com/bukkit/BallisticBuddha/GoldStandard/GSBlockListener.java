package com.bukkit.BallisticBuddha.GoldStandard;

import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;

import com.bukkit.BallisticBuddha.GoldStandard.Transactions.*;

import gnu.trove.map.hash.TIntIntHashMap;

public class GSBlockListener extends BlockListener{
	
		private static GoldStandard plugin = null;
		public GSBlockListener(GoldStandard instance){
			plugin = instance;
		}
		 
	    public void onBlockDamage(BlockDamageEvent event) {
	    	if ((event.getBlock().getTypeId() == Material.FURNACE.getId() || event.getBlock().getTypeId() == Material.BURNING_FURNACE.getId()) && plugin.furnaceMode())
	    		sellIt(event, "Furnace");
	    	else if (event.getBlock().getTypeId() == Material.CHEST.getId() && plugin.chestMode())
	    		sellIt(event, "Chest");
	    	else if (event.getBlock().getTypeId() == Material.DISPENSER.getId() && plugin.dispenserMode())
	    		sellIt(event, "Dispenser");
	    }
	    private void sellIt(BlockDamageEvent event, String SellObject){
	    	Player player = event.getPlayer();
			if (player.getItemInHand().getTypeId() == plugin.getSellTool()){
				Inventory stuff = null;
				GSTransaction action;
				if (SellObject.equalsIgnoreCase("Furnace"))
					stuff = ((Furnace) ((ContainerBlock) event.getBlock().getState())).getInventory();
				else if (SellObject.equalsIgnoreCase("Chest"))
					stuff = ((Chest) ((ContainerBlock) event.getBlock().getState())).getInventory();
				else if (SellObject.equalsIgnoreCase("Dispenser"))
					stuff = ((Dispenser) ((ContainerBlock) event.getBlock().getState())).getInventory();
				else
					return;
				if (stuff == null)
					return;
				if (isEmpty(stuff)){
					action = new BuyProcedure(plugin,player,stuff);
					action.execute(plugin.getBaseItem().getTypeId(), 1);
					return;
				}
				//TODO: un-ugly this next conditional
				if (stuff.contains(plugin.getBaseItem().getTypeId()) || 
						(stuff.contains(plugin.getBaseItem().getBlock()))){
			   	   	if (!plugin.hasPermissions(player, "goldstandard.sell")){
			   	   		player.sendMessage(ChatColor.RED.toString() +"You do not have permission to sell.");
			   	   		return;
			   	   	}
					if (!plugin.getProtection().canSellFrom(player, event.getBlock())){
						player.sendMessage(ChatColor.RED.toString() +"You do not have access to this "+SellObject+".");
						return;
					}
			   	   	//start selling
					TIntIntHashMap itemsSold = new TIntIntHashMap();
					for (ItemStack is : stuff.getContents()){
						if (is != null){
	    					if (is.getTypeId() == plugin.getBaseItem().getTypeId()){
	    						itemsSold.putIfAbsent(is.getTypeId(), 0);
	    						itemsSold.adjustValue(is.getTypeId(),is.getAmount());
	    					}
	    					else if (is.getTypeId() == plugin.getBaseItem().getBlock()){
	    						itemsSold.putIfAbsent(GSItem.reverseGetBlock(is.getTypeId()), 0);
	    						itemsSold.adjustValue(GSItem.reverseGetBlock(is.getTypeId()),is.getAmount()*9);
	    					}
						}
					}
					action = new SellProcedure(plugin,player,stuff,true,false);
					itemsSold.forEachEntry(action);
				}
			}
	    }
	    private boolean isEmpty(Inventory i){
	    	for (ItemStack is : i.getContents())
	    		if (is != null)
	    			return false;
	    	return true;
	    }
}