package com.bukkit.BallisticBuddha.GoldStandard;

import com.bukkit.BallisticBuddha.GoldStandard.GSPlayerListener;
import com.nijiko.coelho.iConomy.iConomy;

import org.bukkit.Material;
import org.bukkit.block.ContainerBlock;
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
		
	    public static boolean hasPermissions(Player p, String s) {
	        if (GoldStandard.Permissions != null) {
	            return GoldStandard.Permissions.has(p, s);
	        } else {
	            return p.isOp();
	        }
	    }    
	    public void onBlockDamage(BlockDamageEvent event) {
	    	if (event.getBlock().getTypeId() == Material.FURNACE.getId() || event.getBlock().getTypeId() == Material.BURNING_FURNACE.getId()){
		    	Player player = event.getPlayer();
    			if (player instanceof Player) //just in case?
    				if (player.getItemInHand().getTypeId() == plugin.getSellTool()){
    					Inventory stuff = ((Furnace) ((ContainerBlock) event.getBlock().getState())).getInventory();
    					//note to self: !(do that again)
    					if (stuff.contains(plugin.getItem())){
    		    	    	if (!GoldStandard.Permissions.has(player, "goldstandard.sell")){
    		    	    		player.sendMessage(ChatColor.RED.toString() +"You do not have permission to sell.");
    		    	    		return;
    		    	    	}
    						int amt = 0;
    						double totalSale = 0;
    						for (ItemStack is : stuff.getContents()){
    							if (is != null)
	    							if (is.getTypeId() == plugin.getItem()){
	    								amt += is.getAmount();
	    							}
    						}
    						for (int i=0;i<amt;i++){
    							totalSale += plugin.getCalc().getWorth();
    							plugin.getCalc().forceIncrement(1);
    						}
    						stuff.remove(plugin.getItem()); //clear the furnace
    						plugin.getCalc().addEntryNI(amt,player.getName());//add to gslog without incrementing the transactions counter
							iConomy.getBank().getAccount(player.getName()).add(totalSale); //give them money
    						player.sendMessage(ChatColor.GREEN.toString() + "Sold "+amt+" "+ formatMaterialName(Material.getMaterial(plugin.getItem())) + " for " +df.format(totalSale)+ " "+ iConomy.getBank().getCurrency());
    					}
    				}
	    	}
	    }
	    private String formatMaterialName(Material mat){
	    	String toOut = "";
	    	String oldString = mat.name().toLowerCase(); 
	    	for (int i=0;i < oldString.length();i++){
	    		if (oldString.charAt(i) == '_')
	    			toOut += ' ';
	    		else
	    			toOut += oldString.charAt(i);
	    	}
	    	return toOut;
	    }
}