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
		this.countAllItems();
		try {
			initFlatFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
				fw.write('\n'+"    "+itemId+":");
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
		return 0;
	}

	@Override
	public int getTransactions(String itemID, boolean positive) {
		//not supported for flatfiles
		return 0;
	}

	@Override
	public void addEntry(int amt, String usr, int item) {
		transValues.adjustOrPutValue(item, amt, amt);
		return;
	}

	@Override
	public void addEntryNI(int amt, String usr, int item) {
		//not supported for flatfiles
		return;
	}

	@Override
	public void clear() {
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
		players.save();
		return;
	}

	@Override
	public void loadAllPlayers() {
		return;
	}

	@Override
	public boolean loadPlayer(String name) {
		return false;
	}

	@Override
	public void addPlayer(String name) {
		this.playerData.put(name, new GSPlayer(tmpCounter++, name));
		return;
	}
	
	@Override
	public void storePlayer(String name) {
		playerData.remove(name);
		return;
	}

	@Override
	public void storePlayerND(String name) {
		return;
	}
}