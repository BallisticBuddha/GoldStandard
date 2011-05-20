package com.bukkit.BallisticBuddha.GoldStandard.Transactions;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
    		return true;
		}
		if (amt <= 0){
			player.sendMessage(ChatColor.RED.toString() +"Yeah...right.");
			return true;
		}
		double totalSale = 0;
		int totalInInventory = 0;
		int blocksInInventory = 0;
		Holdings holdings = iConomy.getAccount(player.getName()).getHoldings();
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
			else if ((is.getTypeId() == gs.getGSItem(id).getBlock()) && includeBlocks)
				blocksInInventory += is.getAmount();
		}
		if (totalInInventory+(blocksInInventory*9) < amt){
			player.sendMessage(ChatColor.RED.toString() +"You do not have "+amt+ " "+
					gs.getGSItem(id).getNickname() + " in your inventory.");
			if (includeBlocks)
				player.sendMessage(ChatColor.RED.toString() +"You only have "+(totalInInventory+(blocksInInventory*9))+
						" [including "+gs.getGSItem(id).getNickname()+" blocks].");
			else
				player.sendMessage(ChatColor.RED.toString() +"You only have "+totalInInventory+".");
			return false;
		}
		for (int i=0;i<amt;i++){
			totalSale += gs.getCalc().getWorth();
			gs.getCalc().forceIncrement(1);
		}
		ItemStack bis = null;
		ItemStack tis = null;
		if (((((totalInInventory+blocksInInventory*9)-amt)/9) >= 1) && BlockChange){
			bis = new ItemStack(gs.getGSItem(id).getBlock(),((totalInInventory+blocksInInventory*9)-amt)/9);
			tis = new ItemStack(id,((totalInInventory+blocksInInventory*9)-amt)%9);
		}
		else
			tis = new ItemStack(id,(totalInInventory+blocksInInventory*9)-amt);
		stuff.remove(id);
		if (includeBlocks)
			stuff.remove(gs.getGSItem(id).getBlock());
		if (bis != null)
			stuff.addItem(bis);
		if (tis.getAmount() > 0)
			stuff.addItem(tis);
		gs.getCalc().addEntryNI(amt,player.getName(),id);
		holdings.add(totalSale);
		player.sendMessage(ChatColor.GREEN.toString() + "Sold "+amt+" "+ 
				gs.getGSItem(id).getNickname() + " for " +iConomy.format(totalSale));
		return false;
	}				
}