package com.bukkit.BallisticBuddha.GoldStandard.Transactions;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.bukkit.BallisticBuddha.GoldStandard.GoldStandard;
import com.iConomy.iConomy;

import gnu.trove.procedure.TIntIntProcedure;

public abstract class GSTransaction implements TIntIntProcedure{

	protected GoldStandard gs;
	protected Player player;
	protected Inventory stuff;
	protected iConomy iConomy;
	
	/** Sets up a purchase or sale of items from either a container block or a persons inventory
	 *  the method execute must be called for each unique item type that is bought or sold in this transaction
	 * 
	 * @param instance - the instance of GoldStandard
	 * @param player - the seller of the item(s)
	 * @param stuff - either a persons inventory or the contents of a container block
	 */
	public GSTransaction(GoldStandard instance, Player player, Inventory stuff){
		gs = instance;
		this.player = player;
		this.iConomy = gs.iConomy;
		this.stuff = stuff;
	}
	
	@Override
	public abstract boolean execute(int id, int amt);
}