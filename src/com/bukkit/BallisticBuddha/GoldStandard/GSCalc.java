package com.bukkit.BallisticBuddha.GoldStandard;

import java.util.logging.Logger;

/**
 * Handler for GSData to choose which data class to use and drive it from this object
 */

public class GSCalc {
	
	private GSData data = null;
	private double worth = 0;
	private String system;
	private GoldStandard gs = null;
	protected static Logger log = Logger.getLogger("Minecraft");
	
	public GSCalc(GoldStandard instance){
		this.gs = instance;
		this.system = this.gs.getConfig().getString("Data","none");
		
		if (system.equalsIgnoreCase("none")){
			this.worth =  this.gs.getConfig().getDouble("Base",100.0);
			log.info("[GoldStandard] Using static pricing.");
		}
		else if (system.equalsIgnoreCase("mysql")){
			this.data = new GSDataMySQL();
			this.calculate();
			log.info("[GoldStandard] MySQL driver loaded.");
		}
		else if (system.equalsIgnoreCase("h2sql") || system.equalsIgnoreCase("h2")){
			this.data = new GSDataH2();
			this.calculate();
			log.info("[GoldStandard] H2 driver loaded.");
		}
	}
	private void calculate(){
		double val = (data.getBase() - (data.getTransactions() * data.getRatio()));
		if (val < data.getMin())
			this.worth = data.getMin();
		else if (val > data.getMax())
			this.worth = data.getMax();
		else
			this.worth = val;
	}
	public double getWorth(){
		if (!this.system.equalsIgnoreCase("none"))
			this.calculate();		
		return this.worth;
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
	public void addEntry(int amt, String usr){
		if (this.system.equalsIgnoreCase("none"))
			return;
		data.addEntry(amt, usr);
	}
	public void forceIncrement(int amt){
		if (this.system.equalsIgnoreCase("none"))
			return;
		data.forceIncrement(amt);
	}
	public void addEntryNI(int amt, String usr){
		if (this.system.equalsIgnoreCase("none"))
			return;
		data.addEntryNI(amt, usr);
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
}