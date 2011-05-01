package com.bukkit.BallisticBuddha.GoldStandard;

import com.bukkit.BallisticBuddha.GoldStandard.GSPlayerListener;
import com.nijiko.coelho.iConomy.iConomy;

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
import java.text.*;

public class GSBlockListener extends BlockListener{
	
		public static GoldStandard plugin = null;	
		private static iConomy iConomy = null;
		private DecimalFormat df = new DecimalFormat("#.##");
		public GSBlockListener(GoldStandard instance){
			plugin = instance;
			iConomy = GoldStandard.getiConomy();
		}
		
	    private static boolean hasPermissions(Player p, String s) {
	        if (GoldStandard.Permissions != null) {
	            return GoldStandard.Permissions.has(p, s);
	        } else {
	            return p.isOp();
	        }
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
	    	if (player instanceof Player) //just in case?
				if (player.getItemInHand().getTypeId() == plugin.getSellTool()){
					Inventory stuff = null;
					if (SellObject.equalsIgnoreCase("Furnace"))
						stuff = ((Furnace) ((ContainerBlock) event.getBlock().getState())).getInventory();
					else if (SellObject.equalsIgnoreCase("Chest"))
						stuff = ((Chest) ((ContainerBlock) event.getBlock().getState())).getInventory();
					else if (SellObject.equalsIgnoreCase("Dispenser"))
						stuff = ((Dispenser) ((ContainerBlock) event.getBlock().getState())).getInventory();
					else
						return;
					if (isEmpty(stuff)){
						buyIt(stuff,player);
						return;
					}
					if (stuff.contains(plugin.getItem())){
			    	   	if (!hasPermissions(player, "goldstandard.sell")){
			    	   		player.sendMessage(ChatColor.RED.toString() +"You do not have permission to sell.");
			    	   		return;
			    	   	}
						int amt = 0;
						double totalSale = 0;
						for (ItemStack is : stuff.getContents()){
							if (is != null)
	    						if (is.getTypeId() == plugin.getItem())
	    							amt += is.getAmount();
						}
						for (int i=0;i<amt;i++){
							totalSale += plugin.getCalc().getWorth();
							plugin.getCalc().forceIncrement(1); //increment counter once
						}
						stuff.remove(plugin.getItem()); //clear the container of all matching items
						plugin.getCalc().addEntryNI(amt,player.getName());//add to gslog without incrementing the transactions counter
						iConomy.getBank().getAccount(player.getName()).add(totalSale); //give them money
						player.sendMessage(ChatColor.GREEN.toString() + "Sold "+amt+" "+ plugin.formatMaterialName(Material.getMaterial(plugin.getItem())) + " for " +df.format(totalSale)+ " "+ iConomy.getBank().getCurrency());
					}
				}
	    }
	    private void buyIt (Inventory stuff, Player player){
	    	if (!plugin.getBuyback())
	    		return;
	    	if (!GoldStandard.Permissions.has(player, "goldstandard.buy")){
	    		player.sendMessage(ChatColor.RED.toString() +"You do not have permission to buy.");
	    		return;
	    	}
	    	if (plugin.getCalc().getWorth() > iConomy.getBank().getAccount(player.getName()).getBalance()){
				player.sendMessage(ChatColor.RED.toString() +"Insufficient Funds");
				return;
	    	}
			iConomy.getBank().getAccount(player.getName()).add(-plugin.getCalc().getWorth());
			player.getInventory().addItem(new ItemStack(plugin.getItem(),1));
			plugin.getCalc().addEntry(-1,player.getName());
			player.sendMessage(ChatColor.GREEN.toString() + "Bought 1 "+ plugin.formatMaterialName(Material.getMaterial(plugin.getItem())) + " for " +df.format(plugin.getCalc().getWorth())+ " "+ iConomy.getBank().getCurrency());
	    }
	    private boolean isEmpty(Inventory i){
	    	for (ItemStack is : i.getContents())
	    		if (is != null)
	    			return false;
	    	return true;
	    }

}