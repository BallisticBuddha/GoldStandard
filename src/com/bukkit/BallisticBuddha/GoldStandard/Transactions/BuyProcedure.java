package com.bukkit.BallisticBuddha.GoldStandard.Transactions;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.bukkit.BallisticBuddha.GoldStandard.GoldStandard;
import com.iConomy.system.Holdings;

public class BuyProcedure extends GSTransaction{

	public BuyProcedure(GoldStandard instance, Player player, Inventory stuff) {
		super(instance, player, stuff);
	}

	@Override
	public boolean execute(int id, int amt) {
		if (!(player instanceof Player))
			return false;
    	if (!gs.getGSItem(id).canBeBought())
    		return false;
		if (!gs.hasPermissions(player, "goldstandard.buy")){
			player.sendMessage(ChatColor.RED.toString() +"You do not have permission to buy.");
    		return false;
		}
		if (amt <= 0){
			player.sendMessage(ChatColor.RED.toString() +"Yeah...right.");
			return false;
		}
		double total = 0;
		Holdings holdings = iConomy.getAccount(player.getName()).getHoldings();
		if (holdings == null){
			player.sendMessage(ChatColor.RED.toString() +
					"An error occurred while retrieving your holdings :(");
			return false;
		}
		for (int i=0;i<amt;i++){
			total += gs.getCalc().getWorth();
			gs.getCalc().forceIncrement(-1);
		}
    	if (total > holdings.balance()){
    		player.sendMessage(ChatColor.RED.toString() +"Insufficient Funds");
    		player.sendMessage(ChatColor.RED.toString() + amt+" "+ 
					gs.getGSItem(id).getNickname() + 
					" would cost "+iConomy.format(total));
    		gs.getCalc().forceIncrement(amt);
    		return false;
    	}
		gs.getCalc().addEntryNI(-amt,player.getName(),id);
		holdings.subtract(total);
    	ItemStack tis = new ItemStack(id,amt);
		player.getInventory().addItem(tis);
		player.sendMessage(ChatColor.GREEN.toString() + "Purchased "+amt+" "+ 
				gs.getGSItem(id).getNickname() + " for " +iConomy.format(total));
		return false;
	}
}
