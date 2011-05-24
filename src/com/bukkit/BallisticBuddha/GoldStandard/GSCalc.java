package com.bukkit.BallisticBuddha.GoldStandard;

import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.Material;

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
			this.data = null;
			gs.getBaseItem().worth = gs.getBaseItem().getPrice();
			log.info("[GoldStandard] Using static pricing.");
		}
		else if (system.equalsIgnoreCase("mysql")){
			this.data = new GSDataMySQL();
			this.calculate(gs.getBaseItem());
			log.info("[GoldStandard] MySQL driver loaded.");
		}
		else if (system.equalsIgnoreCase("h2sql") || system.equalsIgnoreCase("h2")){
			this.data = new GSDataH2();
			this.calculate(gs.getBaseItem());
			log.info("[GoldStandard] H2 driver loaded.");
		}
	}
	private double getStaticPrice(GSItem gsi){
		if (gsi.getGSType() == GSItem.GSType.relative)
			return gs.getBaseItem().getPrice() * gsi.getRelation();
		else
			return gsi.getPrice();
	}
	private void calculate(GSItem gsi){
		if (gsi.getGSType() == GSItem.GSType.base)
			calculateBase(gsi);
		else if (gsi.getGSType() == GSItem.GSType.base)
			calculateRelative(gsi);
	}
	private void calculateBase(GSItem gsi){
		double val = (gsi.getPrice() - (data.getTransactions() * gsi.getRatio()));
		if (val < data.getMin())
			gsi.worth = gsi.getMin();
		else if (val > gsi.getMax())
			gsi.worth = gsi.getMax();
		else
			gsi.worth = val;		
	}
	private void calculateRelative(GSItem gsi){
		calculateBase(gs.getBaseItem());
		double val = gs.getBaseItem().worth * gsi.getRelation();
	}
	public double getWorth(GSItem gsi){
		if (!this.system.equalsIgnoreCase("none"))
			calculate(gsi);		
		return gsi.worth;
	}
	public void clear(){
		if (this.system.equalsIgnoreCase("none"))
			return;
		data.clear();
	}
	public void clearOld(){
		if (this.system.equalsIgnoreCase("none"))
			return;
		data.clearOld();
	}
	public void addEntry(int amt, String usr, int item){
		if (this.system.equalsIgnoreCase("none"))
			return;
		data.addEntry(amt, usr, item);
	}
	public void forceIncrement(int amt){
		if (this.system.equalsIgnoreCase("none"))
			return;
		data.forceIncrement(amt);
	}
	public void addEntryNI(int amt, String usr, int item){
		if (this.system.equalsIgnoreCase("none"))
			return;
		data.addEntryNI(amt, usr, item);
	}
	public boolean needsCleaning(){
		if (this.system.equalsIgnoreCase("none"))
			return false;
		return data.needsCleaning();
	}
	public String getSystem(){
		return system;
	}
	public void closeDBSession(){
		if (this.system.equalsIgnoreCase("none"))
			return;
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
}