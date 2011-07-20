package com.bukkit.BallisticBuddha.GoldStandard.Data;

import com.bukkit.BallisticBuddha.GoldStandard.GSPlayer;

public class GSNoneData extends GSData {
	
	int tmpCounter = 0;

	@Override
	public int countTransactions(String itemID) {
		return 0;
	}

	@Override
	public int getTransactions(String itemID, boolean positive) {
		return 0;
	}

	@Override
	public void addEntry(int amt, String usr, int item) {
		transValues.adjustOrPutValue(item, amt, amt);
		return;
	}

	@Override
	public void addEntryNI(int amt, String usr, int item) {
		return;
	}

	@Override
	public void clear() {
		return;
	}

	@Override
	public void clearOld() {
		return;
	}

	@Override
	public boolean needsCleaning() {
		return false;
	}

	@Override
	public void closeSession() {
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
