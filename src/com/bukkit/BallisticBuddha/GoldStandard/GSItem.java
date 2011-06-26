package com.bukkit.BallisticBuddha.GoldStandard;

import org.bukkit.inventory.ItemStack;

public class GSItem extends ItemStack {

	public static enum GSType{
		base,
		relative,
		independent,
		fixed;
	}
	private String nickname;
	private GSType type;
	private double relation;
	private double price;
	private double minimum;
	private double maximum;
	private double ratio;
	private boolean allowBlock;
	private boolean buyBack;
	/**
	 * Constructor for Base and Independent items
	 * 
	 * @param nick - Nickname of the item
	 * @param id - Item ID of this item
	 * @param allowBlock - if the item can be sold in block form
	 * @param bb - whether this item can be bought back or not
	 * @param gst - Type of pricing to use (Base or Independent)
	 * @param price - Base (initial) price for this item
	 * @param min - price floor for this item
	 * @param max - price ceiling for this item
	 * @param ratio - scaling ratio (change in price per transaction)
	 */
	public GSItem(String nick, int id , boolean allowBlock , boolean bb, GSType gst, double price, double min, double max, double ratio) {
		super(id);
		this.nickname = nick;
		this.type = gst;
		this.price = price;
		this.minimum = min;
		this.maximum = max;
		this.ratio = ratio;
		this.allowBlock = allowBlock;
		this.buyBack = bb;
	}
	
	/** Constructor for Relative and Fixed items
	 * 
	 * @param nick - Nickname of the item
	 * @param id - Item ID of this item
	 * @param allowBlock - if the item can be sold in block form
	 * @param bb - whether this item can be bought back or not
	 * @param amount - size of the item stack
	 * @param gst - Type of pricing to use (Relative or Fixed)
	 * @param arg - either the set price of a fixed item or the relation of a relative item
	 */
	public GSItem(String nick, int id, boolean allowBlock , boolean bb, GSType gst, double arg){
		super(id);
		this.nickname = nick;
		this.type = gst;
		this.allowBlock = allowBlock;
		this.buyBack = bb;
		if (gst == GSType.relative)
			this.relation = arg;
		else if (gst == GSType.fixed)
			this.price = arg;
	}
	/** Returns the type of pricing method for this item [Base,Relative,Independent, or Fixed]
	 * @valid_types ALL 
	 * @return the GSType of this item
	 */
	public GSType getGSType(){
		return this.type;
	}
	/**Gets the base (or fixed) price of an item
	 * @valid_types BASE, INDEPENDENT, FIXED
	 * @return 
	 */
	public double getPrice(){
		return this.price;
	}
	/** Return the nickname of this item that was specified in items.yml
	 * @valid_types ALL
	 * @return Specified nickname of this item
	 */
	public String getNickname(){
		return this.nickname;
	}
	/** Returns the relation of an item
	 * @valid_types RELATIVE
	 * @return the relation to the base item of this item
	 */
	public double getRelation(){
		return this.relation;
	}
	/**Return the floor price of an item
	 * @valid_types BASE, INDEPENDENT
	 * @return the minimum price
	 */
	public double getMin(){
		return this.minimum;
	}
	/**Return the celiling price of an item
	 * @valid_types BASE, INDEPENDENT
	 * @return the maximum price
	 */
	public double getMax(){
		return this.maximum;
	}
	/**Return the ratio of growth/decay of this item
	 * @valid_types BASE, INDEPENDENT
	 * @return 
	 */
	public double getRatio(){
		return this.ratio;
	}
	/** Returns the ID of the equivalent block, if this item can be stored as a block.
	 *  Returns -1 If the item has no block equivalent or if representing this item as a block is disallowed
	 * 
	 * @return the itemID of the appropriate block
	 */
	public int getBlock(){
		if (blockAllowed())
			switch(getType()){
				case IRON_INGOT:
					return 42;
				case GOLD_INGOT:
					return 41;
				case DIAMOND:
					return 57;
				case INK_SACK:
					if (this.getDurability() == 4)
						return 22;
				default:
					return -1;
			}
		return -1;
	}
	/** Returns the sub-item of the specified block, if this is a block of 9 sub-items.
	 *  Returns -1 if said block has no equivalent sub-item
	 * 
	 * @return the itemID of the appropriate sub-item
	 * @param id - the itemID of the block to lookup
	 */
	public static int reverseGetBlock(int id){
		switch (id){
			case 42: //iron block
				return 265;
			case 41: //gold block
				return 266;
			case 57: //diamond block
				return 264;
			case 22: //lapis lazuli block
				return 351; //TODO: this is a generic dye item, need to somehow specify it's color as blue
			default: //not a valid block
				return -1;
		}
	}
	public boolean blockAllowed(){
		return this.allowBlock;
	}
	public boolean canBeBought(){
		return this.buyBack;
	}
}
