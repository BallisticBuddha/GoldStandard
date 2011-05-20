package com.bukkit.BallisticBuddha.GoldStandard.Data;

import java.sql.Connection;
import java.util.logging.Logger;

import org.bukkit.util.config.Configuration;

import com.bukkit.BallisticBuddha.GoldStandard.GoldStandard;

public abstract class GSData {
	
	protected double base;
	protected int duration; //in days
	protected int transactions = 0;
	protected double ratio;
	protected double min;
	protected double max;
	protected Object CalcLock = new Object();
	protected static Logger log = Logger.getLogger("Minecraft");
	protected GoldStandard gs;
	protected Configuration config;
	protected Connection conn;
	
	public GSData(){
		initialize();
	}
	
	private void initialize(){
		gs = (GoldStandard) GoldStandard.getBukkitServer().getPluginManager().getPlugin("GoldStandard");
		this.config = gs.getConfig();
		this.base = gs.getBaseItem().getPrice();
		this.min = gs.getBaseItem().getMin();
		this.max = gs.getBaseItem().getMax();
		this.ratio = gs.getBaseItem().getRatio();
		this.duration = this.config.getInt("Duration", 7);
	}
	public int getTransactions(){
		return transactions;
	}
	public abstract int countTransactions(String itemID);
	/** Gets the number of sales or purchases of the item
	 * 
	 * @param itemID - itemID of the item to the the transactions for
	 * @param positive - True for positive transactions (sales), False for negative transactions (purchases)
	 */
	public abstract int getTransactions(String itemID, boolean positive);
	public int getDuration(){
		return duration;
	}
	public double getRatio() {
		return ratio;
	}
	public double getBase() {
		return base;
	}
	public double getMin() {
		return min;
	}
	public double getMax() {
		return max;
	}
	public void forceIncrement(int amt){
		this.transactions += amt;
	}
	public abstract void addEntry(int amt, String usr, int item);
	public abstract void addEntryNI(int amt, String usr, int item);
	public abstract void clear();
	public abstract void clearOld();
	public abstract boolean needsCleaning();
	public abstract void closeSession();
}