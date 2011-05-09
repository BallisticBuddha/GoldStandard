package com.bukkit.BallisticBuddha.GoldStandard;

import org.bukkit.inventory.ItemStack;



public class GSItem extends ItemStack {

	public enum GSType{
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
	
	/**
	 * Constructor for Base and Independent items
	 * 
	 * @param id - Item ID of this item
	 * @param amount - size of the item stack
	 * @param gst - Type of pricing to use (Base or Independent)
	 * @param price - Base (initial) price for this item
	 * @param min - price floor for this item
	 * @param max - price ceiling for this item
	 * @param ratio - scaling ratio (change in price per transaction)
	 */
	public GSItem(int id, String nick, GSType gst, double price, double min, double max, double ratio) {
		super(id);
		this.nickname = nick;
		this.type = gst;
		this.price = price;
		this.minimum = min;
		this.maximum = max;
		this.ratio = ratio;
	}
	
	/** Constructor for Relative and Fixed items
	 * 
	 * @param id - Item ID of this item
	 * @param amount - size of the item stack
	 * @param gst - Type of pricing to use (Relative or Fixed)
	 * @param arg - either the set price of a fixed item or the relation of a relative item
	 */
	public GSItem(int id, String nick ,GSType gst, double arg){
		super(id);
		this.nickname = nick;
		this.type = gst;
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
}
