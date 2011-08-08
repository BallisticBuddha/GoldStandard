package com.bukkit.BallisticBuddha.GoldStandard.Data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.bukkit.util.config.Configuration;

import com.bukkit.BallisticBuddha.GoldStandard.GSPlayer;

public class GSNoneData extends GSData {
	
	int tmpCounter = 0;
	File playerFile = new File("plugins/GoldStandard/players.yml");
	File transactionFile = new File("plugins/GoldStandard/transactions.yml");
	Configuration players;
	Configuration transactions;
	
	public GSNoneData(){
		try {
			initFlatFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.countAllItems();
	}
	private void initFlatFiles() throws IOException{
		createFlatFiles();
		players.load();
		if (players.getKeys().isEmpty()){
			FileWriter fw = new FileWriter(playerFile);
			fw.write("#Auto-generated flat-file for storing player data"+'\n'+"Players:");
			fw.close();
		}
		if (transactions.getKeys().isEmpty()){
			FileWriter fw = new FileWriter(transactionFile);
			fw.write("#Auto-generated flat-file for storing transaction data"+'\n'+"Items:");
			for (int itemId : transValues.keys()){
				fw.write('\n'+"    '"+itemId+"': 0");
			}
			fw.close();
		}
		players.load();
		transactions.load();
	}
	private void createFlatFiles() throws IOException{
		if (!playerFile.exists()){
			playerFile.createNewFile();	
		}
		players = new Configuration(playerFile);
		if (!transactionFile.exists()){
			transactionFile.createNewFile();
		}
		transactions = new Configuration(transactionFile);
	}
	
	@Override
	public int countTransactions(String itemID) {
		return transactions.getInt("Items."+itemID,0);
	}

	@Override
	public int getTransactions(String itemID, boolean positive) {
		//not supported for flatfiles
		return 0;
	}

	@Override
	public void addEntry(int amt, String usr, int item) {
		transValues.adjustOrPutValue(item, amt, amt);
		transactions.setProperty("Items."+item, transValues.get(item));
		return;
	}

	@Override
	public void addEntryNI(int amt, String usr, int item) {
		transactions.setProperty("Items."+item, transValues.get(item));
		return;
	}

	@Override
	public void clear() {
		for (int itemId : transValues.keys()){
			transactions.setProperty("Items."+itemId, 0);
		}
		return;
	}

	@Override
	public void clearOld() {
		//not supported for flatfiles
		return;
	}

	@Override
	public boolean needsCleaning() {
		//not supported for flatfiles
		return false;
	}

	@Override
	public void closeSession() {
		forceFileSave();
		return;
	}

	@Override
	public void loadAllPlayers() {
		playerData.clear();
		tmpCounter = 0;
		for (String playerName : transactions.getKeys("Players")){
			GSPlayer gsp = new GSPlayer(tmpCounter++, playerName,gs);
			gsp.setBuyItem(players.getInt("Players."+playerName+".buyItem",0));
			gsp.setBuyQty(players.getInt("Players."+playerName+".buyQty", 1));
			gsp.setSellItems(players.getString("Players."+playerName+".sellItems"));
			gsp.setLastBought(Long.parseLong(players.getString("Players."+playerName+".lastBought")));
			gsp.setLastSold(Long.parseLong(players.getString("Players."+playerName+".lastSold")));
			playerData.put(playerName, gsp);
		}
		return;
	}

	@Override
	public boolean loadPlayer(String playerName) {
		Boolean userexists = false;
		if (players.getNode("Players."+playerName) != null){
			GSPlayer gsp = new GSPlayer(tmpCounter++, playerName,gs);
			gsp.setBuyItem(players.getInt("Players."+playerName+".buyItem",0));
			gsp.setBuyQty(players.getInt("Players."+playerName+".buyQty", 1));
			gsp.setSellItems(players.getString("Players."+playerName+".sellItems"));
			gsp.setLastBought(Long.parseLong(players.getString("Players."+playerName+".lastBought")));
			gsp.setLastSold(Long.parseLong(players.getString("Players."+playerName+".lastSold")));
			playerData.put(playerName, gsp);
			userexists = true;
			
			if (playerData.containsKey(playerName))
				playerData.remove(playerName);
		}
		return userexists;
	}

	@Override
	public void addPlayer(String name) {
		players.setProperty("Players."+name, null);
		players.setProperty("Players."+name+".buyItem",gs.getDefaultBuyItem());
		players.setProperty("Players."+name+".buyQty",gs.getDefaultBuyQty());
		players.setProperty("Players."+name+".sellItems",gs.getDefaultSellItems());
		players.setProperty("Players."+name+".lastBought",Long.toString(System.currentTimeMillis()));
		players.setProperty("Players."+name+".lastSold",Long.toString(System.currentTimeMillis()));
		players.save();
		this.playerData.put(name, new GSPlayer(tmpCounter++, name,gs));
		return;
	}
	
	@Override
	public void storePlayer(String name) {
		if (!playerData.containsKey(name)){
			log.severe("[GoldStandard] Could not store user "+name+". Player was not loaded into memory!");
			return;
		}
		GSPlayer gsp = playerData.get(name);
		players.setProperty("Players."+name+".buyItem",gsp.getBuyItem());
		players.setProperty("Players."+name+".buyQty",gsp.getBuyQty());
		players.setProperty("Players."+name+".sellItems",gsp.getSellItems());
		players.setProperty("Players."+name+".lastBought",gsp.getLastBought());
		players.setProperty("Players."+name+".lastSold",gsp.getLastBought());
		players.save();
		
		playerData.remove(name);
		return;
	}

	@Override
	public void storePlayerND(String name) {
		if (!playerData.containsKey(name)){
			log.severe("[GoldStandard] Could not store user "+name+". Player was not loaded into memory!");
			return;
		}
		GSPlayer gsp = playerData.get(name);
		players.setProperty("Players."+name+".buyItem",gsp.getBuyItem());
		players.setProperty("Players."+name+".buyQty",gsp.getBuyQty());
		players.setProperty("Players."+name+".sellItems",gsp.getSellItems());
		players.setProperty("Players."+name+".lastBought",gsp.getLastBought());
		players.setProperty("Players."+name+".lastSold",gsp.getLastBought());
		players.save();
		return;
	}
	public void storePlayerNDNS(String name) {
		if (!playerData.containsKey(name)){
			log.severe("[GoldStandard] Could not store user "+name+". Player was not loaded into memory!");
			return;
		}
		GSPlayer gsp = playerData.get(name);
		players.setProperty("Players."+name+".buyItem",gsp.getBuyItem());
		players.setProperty("Players."+name+".buyQty",gsp.getBuyQty());
		players.setProperty("Players."+name+".sellItems",gsp.getSellItems());
		players.setProperty("Players."+name+".lastBought",gsp.getLastBought());
		players.setProperty("Players."+name+".lastSold",gsp.getLastBought());
		return;
	}
	public void forceFileSave(){
		players.save();
		transactions.save();
	}
}