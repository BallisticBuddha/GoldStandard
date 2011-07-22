package com.bukkit.BallisticBuddha.GoldStandard;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

import com.bukkit.BallisticBuddha.GoldStandard.Data.*;

/**
 * Handler for GSData to choose which data class to use and drive it from this object
 */

public class GSCalc {
	
	private GSData data = null;
	private String system;
	private GoldStandard gs = null;
	protected static Logger log = Logger.getLogger("Minecraft");
	
	public GSCalc(GoldStandard instance){
		this.gs = instance;
		this.system = this.gs.getConfig().getString("Data","none");
		
		if (system.equalsIgnoreCase("none")){
			this.data = new GSNoneData();
			log.info("[GoldStandard] Using temporary (\"none\") data.");
			log.warning("[GoldStandard] Data will not be stored after shutdown.");
		}
		else if (system.equalsIgnoreCase("mysql")){
			this.data = new GSDataMySQL();
			log.info("[GoldStandard] MySQL driver loaded.");
		}
		else if (system.equalsIgnoreCase("h2sql") || system.equalsIgnoreCase("h2")){
			this.data = new GSDataH2();
			log.info("[GoldStandard] H2 driver loaded.");
		}
	}
	private double calculate(GSItem gsi){
		if (gsi.getGSType() == GSItem.GSType.base)
			return calculateBase(gsi);
		else if (gsi.getGSType() == GSItem.GSType.relative)
			return calculateRelative(gsi);
		else if (gsi.getGSType() == GSItem.GSType.independent)
			return calculateIndependent(gsi);
		else if (gsi.getGSType() == GSItem.GSType.fixed)
			return gsi.getPrice();
		else 
			return 0;
	}
	private double calculateBase(GSItem gsi){
		double val = (gsi.getPrice() - (data.getTransactions(gsi) * gsi.getRatio()));
		if (val < gsi.getMin())
			return gsi.getMin();
		else if (val > gsi.getMax())
			return gsi.getMax();
		else
			return val;		
	}
	private double calculateRelative(GSItem gsi){
		return calculate(gsi.getParent(gs)) * gsi.getRelation();
	}
	private double calculateIndependent(GSItem gsi){
		double val = (gsi.getPrice() - (data.getTransactions(gsi) * gsi.getRatio()));
		if (val < gsi.getMin())
			return gsi.getMin();
		else if (val > gsi.getMax())
			return gsi.getMax();
		else
			return val;		
	}
	public double getWorth(GSItem gsi){
		if (this.system.equalsIgnoreCase("none"))
			return gsi.getPrice();
		else
			return calculate(gsi);
	}
	public double checkBuyPrice(GSItem gsi){
		if (this.system.equalsIgnoreCase("none"))
			return gsi.getPrice();
		double val = 0.0;
		switch(gsi.getGSType()){
			case base:
				val += (gsi.getPrice() - (data.getTransactions(gsi) * gsi.getRatio()))+gsi.getRatio();
				if (val < gsi.getMin())
					return gsi.getMin();
				else if (val > gsi.getMax())
					return gsi.getMax();
				else
					return val;
			case independent:
				val += (gsi.getPrice() - (data.getTransactions(gsi) * gsi.getRatio()))+gsi.getRatio();
				if (val < gsi.getMin())
					return gsi.getMin();
				else if (val > gsi.getMax())
					return gsi.getMax();
				else
					return val;	
			case relative:
				return (calculate(gs.getBaseItem())*gsi.getRelation())+(gs.getBaseItem().getRatio()*gsi.getRelation());
			case fixed:
				return gsi.getPrice();
			default: //just so the eclipse interpreter won't yell at me :/
				return 0;
		}
	}
	public void clear(){
		data.clear();
	}
	public void clearOld(){
		data.clearOld();
	}
	public void addEntry(int amt, String usr, int item){
		data.addEntry(amt, usr, item);
	}
	public void forceIncrement(int amt, int gsid){
		data.forceIncrement(amt,gsid);
	}
	public void addEntryNI(int amt, String usr, int item){
		data.addEntryNI(amt, usr, item);
	}
	public boolean needsCleaning(){
		return data.needsCleaning();
	}
	public String getSystem(){
		return system;
	}
	public void closeDBSession(){
		data.closeSession();
	}
	/** Gets the number of sales or purchases of the item and constructs
	 * a formatted string to display to the client
	 * 
	 * @param itemID - itemID of the item to the the transactions for
	 * @param type - Type of transactions to get (sales, purchases, or net) defaults to net if an invalid or null option was specified
	 */
	public String getTransactions(String itemID, String type){
		if (this.system.equalsIgnoreCase("none")){
			return "No transaction data available while not using a database.";
		}
		String item = "0";
		String name = "";
		if (GoldStandard.isPositiveInt(itemID)){
			item = itemID;
			name = gs.getGSItem(Integer.parseInt(itemID)).getNickname();
		}
		else if (itemID.equals("*")){
			item = itemID;
			name = "All Items";
		}
		else if (gs.getGSItem(itemID) != null){
			item = Integer.toString(gs.getGSItem(itemID).getTypeId());
			name = itemID;
		}
		else
			return "Invalid ItemID, must be a positive integer, the nickname of an item, or the wildcard \"*\"";
		
		if (type.equalsIgnoreCase("sales") || type.equalsIgnoreCase("+"))
			return data.getTransactions(item,true)+" sales of "+ name +" ("+item+") in the past "+data.getDuration()+" days.";
		else if (type.equalsIgnoreCase("purchases") || type.equalsIgnoreCase("-"))
			return -data.getTransactions(item,false)+" purchases of "+ name +" ("+item+") in the past "+data.getDuration()+" days.";
		else
			return data.countTransactions(item)+" net transactions of "+ name +" ("+item+") in the past "+data.getDuration()+" days.";
	}
	public void loadAllPlayers(){
		data.loadAllPlayers();
	}
	public boolean loadPlayer(String name){
		return data.loadPlayer(name);
	}
	public void addPlayer(String name){
		data.addPlayer(name);
	}
	public void storePlayer(String name){
		data.storePlayer(name);
	}
	public void storePlayerND(String name){
		data.storePlayerND(name);
	}
	public GSPlayer getPlayer(String name){
		return data.getPlayer(name);
	}
	public long timeSinceBought(Player player){
		if (data.getPlayer(player.getName()) == null){
			log.severe("[GoldStandard] A null player is trying to buy something");
			return 0;
		}
		System.out.println();
		long diff = (System.currentTimeMillis() - data.getPlayer(player.getName()).getLastBought());
		if (!gs.opsObeyCooldown() && player.isOp())
			return gs.getBuyCooldown();
		else
			return diff;
	}
	public long timeSinceSold(Player player){
		if (data.getPlayer(player.getName()) == null){
			log.severe("[GoldStandard] A null player is trying to sell something");
			return 0;
		}
		long diff = (System.currentTimeMillis() - data.getPlayer(player.getName()).getLastSold());
		if (!gs.opsObeyCooldown() && player.isOp())
			return gs.getSellCooldown();
		else 
			return diff;
	}
	public void iJustSold(String playerName){
		data.getPlayer(playerName).iJustSold();
	}
	public void iJustBought(String playerName){
		data.getPlayer(playerName).iJustBought();
	}
	public Set<String> getPlayers(){
		return data.getPlayers();
	}
}