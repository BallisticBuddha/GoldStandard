package com.bukkit.BallisticBuddha.GoldStandard;

import java.util.logging.Logger;

import gnu.trove.map.hash.TIntShortHashMap;
import gnu.trove.procedure.TIntShortProcedure;

public class GSPlayer {
	
	private class convertToString implements TIntShortProcedure{
		private String theString = "";
		
		@Override
		public boolean execute(int arg0, short includeBlocks) {
			if (!theString.equals(""))
				theString += ","+arg0+(includeBlocks!=0?"~":"");
			else
				theString += arg0+(includeBlocks!=0?"~":"");
			return true;
		}
		public String getString(){
			return this.theString;
		}
	}
	int ID;
	String name;
	TIntShortHashMap sellItems = new TIntShortHashMap();
	int buyItem;
	int buyQty;
	long lastBought = System.currentTimeMillis();
	long lastSold = System.currentTimeMillis();
	
	public GSPlayer(int id, String name, GoldStandard instance){
		this.ID = id;
		this.name = name;
		this.buyItem = instance.getDefaultBuyItem();
		this.buyQty = instance.getDefaultBuyQty();
		this.setSellItems(instance.getDefaultSellItems());
	}
	
	public void setBuyItem(int bi){
		this.buyItem = bi;
	}
	public void setBuyQty(int qty){
		this.buyQty = qty;
	}
	public void setSellList(TIntShortHashMap itms){
		this.sellItems = itms;
	}
	public void setSellItems(String items){
		String[] itmlist = items.split(",");
		for (String i : itmlist){
			if (i.equals("") || i == null)
				continue;
			if (GoldStandard.isPositiveInt(i))
				sellItems.put(Integer.parseInt(i),(short) 0);
			else if (GoldStandard.isPositiveInt(items.substring(0, items.length()-1)))
				if (items.charAt(items.length()-1) == '~')
					sellItems.put(Integer.parseInt(items.substring(0, items.length()-1)),(short) 1);
			else
				Logger.getLogger("Minecraft").warning("[GoldStandard] Invalid Item number"+ i +" while loading the sell list of player "+ this.name +".");
		}
	}
	//                 It's been such a
	public void setLastBought(long time){
		this.lastBought = time;
	}
	public void setLastSold(long time){
		this.lastSold = time;
	}
	
	public void addSellItem(int itemId, boolean includeBlock){
		this.sellItems.put(itemId,(short) (includeBlock?1:0));
	}
	public void removeSellItem(int itemId){
		this.sellItems.remove(itemId);
	}
	public void iJustSold(){
		this.lastSold = System.currentTimeMillis();
	}
	public void iJustBought(){
		this.lastBought = System.currentTimeMillis();
	}
	
	public int getBuyItem(){
		return this.buyItem;
	}
	public int getBuyQty(){
		return this.buyQty;
	}
	public TIntShortHashMap getSellList(){
		return this.sellItems;
	}
	public String getSellItems(){
		convertToString operation = new convertToString();
		this.sellItems.forEachEntry(operation);
		return operation.getString();
	}
	public long getLastSold(){
		return this.lastSold;
	}
	public long getLastBought(){
		return this.lastBought;
	}
	
	public boolean isInSellList(int itemId){
		return this.sellItems.contains(itemId);
	}
	public boolean itemCanBeBlock(int itemId){
		if (isInSellList(itemId))
			return this.sellItems.get(itemId)!=0;
		return false;
	}
	public int getId(){
		return this.ID;
	}
	public String getName(){
		return this.name;
	}
}