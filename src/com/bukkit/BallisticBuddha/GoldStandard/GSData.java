package com.bukkit.BallisticBuddha.GoldStandard;

import java.sql.Connection;
import java.util.logging.Logger;

import org.bukkit.util.config.Configuration;

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
		
		this.base = this.config.getDouble("Base", 100.0);
		this.min = (this.config.getDouble("Minimum", 0.0));
		this.max = (this.config.getDouble("Maximum", base));
		this.duration = this.config.getInt("Duration", 7);
		this.ratio = this.config.getDouble("Ratio", .1);
	}
	public int getTransactions(){
		return transactions;
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
	public abstract void addEntry(int amt, String usr);
	public abstract void addEntryNI(int amt, String usr);
	public abstract void clear();
	public abstract void clearOld();
	public abstract boolean needsCleaning();
	public abstract void closeSession();
	
	
}