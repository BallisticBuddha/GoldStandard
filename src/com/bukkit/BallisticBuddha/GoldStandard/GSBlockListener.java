package com.bukkit.BallisticBuddha.GoldStandard;

import java.util.logging.Logger;

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
import gnu.trove.map.hash.TIntShortHashMap;
/** A very, very, very ugly listener that sells & buys items from container blocks 
 * 
 * @author BallisticBuddha
 *
 */
public class GSBlockListener extends BlockListener{
	
		private static GoldStandard plugin = null;
		public GSBlockListener(GoldStandard instance){
			plugin = instance;
		}
	    public void onBlockDamage(BlockDamageEvent event) {
	    	if (!(event.getPlayer() instanceof Player))
	    		return;
	    	if ((event.getBlock().getTypeId() == Material.FURNACE.getId() 
	    			|| event.getBlock().getTypeId() == Material.BURNING_FURNACE.getId()) && plugin.furnaceMode())
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
	    	    	long timeSinceLast = plugin.getCalc().timeSinceBought(player);
	    	    	if (timeSinceLast < plugin.getBuyCooldown()){
	    	    		if ((plugin.getBuyCooldown()-timeSinceLast) >= 1000)
	    	    			player.sendMessage(ChatColor.RED.toString() +"You must wait "+
	    	    					(plugin.getBuyCooldown()-timeSinceLast)/1000 +" seconds before you may buy again");
	    	    		else
	    	    			player.sendMessage(ChatColor.RED.toString() +"You must wait "+
	    	    					(plugin.getBuyCooldown()-timeSinceLast) +" miliseconds before you may buy again");
	    	    		return;
	    	    	}
					GSPlayer gsp = plugin.getCalc().getPlayer(player.getName());
					if ((gsp.getBuyItem() <= 0) || (gsp.getBuyQty()) <= 0){
						player.sendMessage(ChatColor.RED.toString() +"You must first set an item and quantity to buy with /gsset.");
						return;
					}
					if (!plugin.validBuy(player))
						return;
					action = new BuyProcedure(plugin,player,stuff);
					action.execute(gsp.getBuyItem(), gsp.getBuyQty());
					return;
				}
    	    	long timeSinceLast = plugin.getCalc().timeSinceSold(player);
    	    	if (timeSinceLast < plugin.getSellCooldown()){
    	    		if ((plugin.getSellCooldown()-timeSinceLast) >= 1000)
    	    			player.sendMessage(ChatColor.RED.toString() +"You must wait "+
    	    					(plugin.getSellCooldown()-timeSinceLast)/1000 +" seconds before you may sell again");
    	    		else
    	    			player.sendMessage(ChatColor.RED.toString() +"You must wait "+
    	    					(plugin.getSellCooldown()-timeSinceLast) +" miliseconds before you may sell again");
    	    		return;
    	    	}
				if (containsAGSItem(player,stuff)){
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
					TIntShortHashMap blockOptions = new TIntShortHashMap();
					GSPlayer gsp = plugin.getCalc().getPlayer(player.getName());
					if (gsp == null){
						Logger.getLogger("Minecraft").info("[GoldStandard] Null player tried to sell!");
						return;
					}
					for (ItemStack is : stuff.getContents()){
						if (is != null){
	    					if (gsp.isInSellList(is.getTypeId())){
	    						if (!plugin.validSale(player, is.getTypeId()))
	    							continue;
	    						itemsSold.putIfAbsent(is.getTypeId(), 0);
	    						itemsSold.adjustValue(is.getTypeId(),is.getAmount());
	    						blockOptions.putIfAbsent(is.getTypeId(), (short) 0);
	    					}
	    					else if (gsp.itemCanBeBlock(GSItem.reverseGetBlock(is.getTypeId()))){
	    						if (!plugin.validSale(player, GSItem.reverseGetBlock(is.getTypeId())))
	    							continue;
	    						if (!plugin.getGSItem(GSItem.reverseGetBlock(is.getTypeId())).blockAllowed())
	    							continue;
	    						itemsSold.putIfAbsent(GSItem.reverseGetBlock(is.getTypeId()), 0);
	    						itemsSold.adjustValue(GSItem.reverseGetBlock(is.getTypeId()),is.getAmount()*9);
	    						blockOptions.putIfAbsent(GSItem.reverseGetBlock(is.getTypeId()), (short) 1);
	    					}
	    				}
					}
					for (int i : blockOptions.keys()){
						new SellProcedure(plugin,player,stuff,blockOptions.get(i)!=0,false).execute(i, itemsSold.get(i));
					}
				}
			}
	    }
	    private boolean isEmpty(Inventory i){
	    	for (ItemStack is : i.getContents())
	    		if (is != null)
	    			return false;
	    	return true;
	    }
	    private boolean containsAGSItem(Player player, Inventory i){
	    	for (ItemStack is : i.getContents()){
	    		if (is == null)
	    			continue;
	    		else if (plugin.getItems().containsKey(is.getTypeId()))
	    			return true;
	    		else if (plugin.getItems().containsKey(GSItem.reverseGetBlock(is.getTypeId())))
	    			if (plugin.getGSItem(GSItem.reverseGetBlock(is.getTypeId())).blockAllowed())
	    				return true;
	    	}
	    	return false;
	    }
}