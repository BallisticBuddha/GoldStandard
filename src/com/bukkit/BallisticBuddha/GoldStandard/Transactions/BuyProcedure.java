package com.bukkit.BallisticBuddha.GoldStandard.Transactions;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.bukkit.BallisticBuddha.GoldStandard.GSItem;
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
		GSItem gsi = gs.getGSItem(id);
		if (gsi == null){
			player.sendMessage(ChatColor.RED.toString() +"Invalid item passed into sell procedure, cancelling transaction.");
			Logger.getLogger("Minecraft").severe("[GoldStandard] Null item was passed as valid!");
			return false;
		}
    	if (!gsi.canBeBought())
    		return false;
		if (holdings == null){
			player.sendMessage(ChatColor.RED.toString() +
					"An error occurred while retrieving your holdings :(");
			return false;
		}
		for (int i=0;i<amt;i++){
			gs.getCalc().forceIncrement(-1,gsi.getTypeId());
			total += gs.getCalc().getWorth(gsi);
		}
    	if (total > holdings.balance()){
    		player.sendMessage(ChatColor.RED.toString() +"Insufficient Funds");
    		player.sendMessage(ChatColor.RED.toString() + amt+" "+ 
					gsi.getNickname() + 
					" would cost "+iConomy.format(total));
    		gs.getCalc().forceIncrement(amt,gsi.getTypeId());
    		return false;
    	}
		gs.getCalc().addEntryNI(-amt,player.getName(),id);
		holdings.subtract(total);
    	ItemStack tis = new ItemStack(id,amt);
		player.getInventory().addItem(tis);
		gs.getCalc().iJustBought(player.getName());
		player.sendMessage(ChatColor.GREEN.toString() + "Purchased "+amt+" "+ 
				gsi.getNickname() + " for " +iConomy.format(total));
		return false;
	}
}
