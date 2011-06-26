package com.bukkit.BallisticBuddha.GoldStandard.Data;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;
import gnu.trove.procedure.TIntProcedure;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

import com.bukkit.BallisticBuddha.GoldStandard.GSItem;
import com.bukkit.BallisticBuddha.GoldStandard.GSPlayer;
import com.bukkit.BallisticBuddha.GoldStandard.GoldStandard;

public abstract class GSData {
	
	private class SumRelativeTransactions implements TIntDoubleProcedure{
		private GSData data;
		private double sum = 0.0;
		
		public SumRelativeTransactions(GSData dat){
			data = dat;
		}
		@Override
		public boolean execute(int itemId, double relation) {
			sum += relation * data.getTransactions(itemId); 
			return false;
		}
		//GET SOME!
		public double getSum(){
			return this.sum;
		}
	}
	protected class SumCount implements TIntProcedure{
		private int sum = 0;
		@Override
		public boolean execute(int entry) {
			sum += entry;
			return false;
		}
		public int getSum(){
			return this.sum;
		}
	}
	private int duration; //in days
	private TIntDoubleHashMap relativeItems = new TIntDoubleHashMap();
	protected TIntIntHashMap transValues = new TIntIntHashMap();
	protected Object CalcLock = new Object();
	protected static Logger log = Logger.getLogger("Minecraft");
	protected GoldStandard gs;
	protected Configuration config;
	protected Connection conn;
	protected Map<String,GSPlayer> playerData;

	public GSData(){
		initialize();
	}
	private void initialize(){
		gs = (GoldStandard) GoldStandard.getBukkitServer().getPluginManager().getPlugin("GoldStandard");
		this.config = gs.getConfig();
		this.duration = this.config.getInt("Duration", 7);
		this.playerData = new HashMap<String,GSPlayer>();
	}
	/** The reason this return double is due to the fact that when getting the transactions
	 *  for a base item, it factors in the transactions for relative items as well
	 *  and calculates that into this calculation which may be fractional
	 *  
	 *  @param gsi - the GSItem to get the transactions for, may only be independent or base 
	 * 
	 */
	public double getTransactions(GSItem gsi){
		switch(gsi.getGSType()){
			case base:
				SumRelativeTransactions srt = new SumRelativeTransactions(this);
				relativeItems.forEachEntry(srt);
				return srt.getSum() + transValues.get(gsi.getTypeId());
			case independent:
				return transValues.get(gsi.getTypeId());
//			case relative:
//				return 0.0;
//			case fixed:
//				return 0.0;
			default:
				return 0.0;				
		}
	}
	public int getTransactions(int id){
		return this.transValues.get(id);
	}
	/** Gets the net transactions for the item
	 * 
	 * @param itemID - itemID of the item to query (or * for all items)
	 * @return - net transactions for that item
	 */
	public abstract int countTransactions(String itemID);
	/** Gets the number of sales or purchases of the item
	 * 
	 * @param itemID - itemID of the item to the the transactions for
	 * @param positive - True for positive transactions (sales), False for negative transactions (purchases)
	 */
	public abstract int getTransactions(String itemID, boolean positive);
	public int getDuration(){
		return this.duration;
	}
	protected void countAllItems(){
		for (GSItem gsi : gs.getItems().values()){
			if (gsi.getGSType() == GSItem.GSType.relative)
				relativeItems.putIfAbsent(gsi.getTypeId(), gsi.getRelation());
			this.transValues.putIfAbsent(gsi.getTypeId(), countTransactions(Integer.toString(gsi.getTypeId())));
		}
	}
	protected void setAllToZero(){
		for (TIntIntIterator itemItr = this.transValues.iterator();
				itemItr.hasNext();){
			itemItr.advance();
			this.transValues.put(itemItr.key(), 0);
		}
	}
	public void forceIncrement(int amt, int item){
		this.transValues.adjustValue(item, amt);
	}
	public abstract void addEntry(int amt, String usr, int item);
	public abstract void addEntryNI(int amt, String usr, int item);
	public abstract void clear();
	public abstract void clearOld();
	public abstract boolean needsCleaning();
	public abstract void closeSession();
	
	public abstract void loadAllPlayers();
	public abstract boolean loadPlayer(String name);
	public abstract void addPlayer(String name);
	public abstract void storePlayer(String name);
	public GSPlayer getPlayer(String name){
		return playerData.get(name);
	}
	public Set<String> getPlayers(){
		return playerData.keySet();
	}
}