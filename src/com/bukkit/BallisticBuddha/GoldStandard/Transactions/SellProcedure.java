package com.bukkit.BallisticBuddha.GoldStandard.Transactions;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.bukkit.BallisticBuddha.GoldStandard.GSItem;
import com.bukkit.BallisticBuddha.GoldStandard.GoldStandard;
import com.iConomy.system.Holdings;

public class SellProcedure extends GSTransaction{
	
	private boolean includeBlocks = false;
	private boolean BlockChange = false;
	
	public SellProcedure(GoldStandard instance, Player player, Inventory stuff, boolean sb, boolean bc) {
		super(instance, player, stuff);
		this.includeBlocks = sb;
		this.BlockChange = bc;
	}
	/** Commits the sale of an item (or multiple items of the same type)
	 * 
	 * You should only run one execute per actual transaction per item due to every execute logging a new entry 
	 * 
	 * @param id The key of the map (itemID)
	 * @param amt The value of this key (amount to sell)
	 * 
	 * @return whether or not to execute again
	 */
	@Override
	public boolean execute(int id, int amt) {
		if (!(player instanceof Player))
			return false;
		if (!gs.hasPermissions(player, "goldstandard.sell")){
			player.sendMessage(ChatColor.RED.toString() +"You do not have permission to sell.");
    		return false;
		}
		if (amt <= 0){
			player.sendMessage(ChatColor.RED.toString() +"Yeah...right.");
			return false;
		}
		double totalSale = 0;
		int totalInInventory = 0;
		int blocksInInventory = 0;
		Holdings holdings = iConomy.getAccount(player.getName()).getHoldings();
		GSItem gsi = gs.getGSItem(id);
		if (gsi == null){
			player.sendMessage(ChatColor.RED.toString() +"Invalid item passed into sell procedure, cancelling transaction.");
			Logger.getLogger("Minecraft").severe("[GoldStandard] Null item was passed as valid!");
			return false;
		}
		if (holdings == null){
			player.sendMessage(ChatColor.RED.toString() +
					"An error occurred while retrieving your holdings :(");
			return false;
		}
		for (ItemStack is : stuff.getContents()){
			if (is == null)
				continue;
			if (is.getTypeId() == id)
				totalInInventory += is.getAmount();
			else if ((is.getTypeId() == gsi.getBlock()) && includeBlocks)
				blocksInInventory += is.getAmount();
		}
		if (totalInInventory+(blocksInInventory*9) < amt){
			player.sendMessage(ChatColor.RED.toString() +"You do not have "+amt+ " "+
					gsi.getNickname() + " in your inventory.");
			if (includeBlocks)
				player.sendMessage(ChatColor.RED.toString() +"You only have "+(totalInInventory+(blocksInInventory*9))+
						" [including "+gsi.getNickname()+" blocks].");
			else
				player.sendMessage(ChatColor.RED.toString() +"You only have "+totalInInventory+".");
			return false;
		}
		for (int i=0;i<amt;i++){
			totalSale += gs.getCalc().getWorth(gsi);
			gs.getCalc().forceIncrement(1,gsi.getTypeId());
		}
		ItemStack bis = null;
		ItemStack tis = null;
		if (((((totalInInventory+blocksInInventory*9)-amt)/9) >= 1) && BlockChange){
			bis = new ItemStack(gsi.getBlock(),((totalInInventory+blocksInInventory*9)-amt)/9);
			tis = new ItemStack(id,((totalInInventory+blocksInInventory*9)-amt)%9);
		}
		else
			tis = new ItemStack(id,(totalInInventory+blocksInInventory*9)-amt);
		stuff.remove(id);
		if (includeBlocks)
			stuff.remove(gsi.getBlock());
		if (bis != null)
			stuff.addItem(bis);
		if (tis.getAmount() > 0)
			stuff.addItem(tis);
		gs.getCalc().addEntryNI(amt,player.getName(),id);
		holdings.add(totalSale);
		gs.getCalc().iJustSold(player.getName());
		player.sendMessage(ChatColor.GREEN.toString() + "Sold "+amt+" "+ 
				gsi.getNickname() + " for " +iConomy.format(totalSale));
		return false;
	}				
}