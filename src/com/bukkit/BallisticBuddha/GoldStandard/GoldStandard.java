package com.bukkit.BallisticBuddha.GoldStandard;

import com.bukkit.BallisticBuddha.GoldStandard.GSItem.GSType;
import com.bukkit.BallisticBuddha.GoldStandard.Protect.*;
import com.bukkit.BallisticBuddha.GoldStandard.Transactions.*;
//import com.bukkit.BallisticBuddha.GoldStandard.griefcraft.*;
import com.iConomy.*;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;

/**
	@author BallisticBuddha
	@version 1.0
	
	Gold Standard plugin for bukkit
	
*/
public class GoldStandard extends JavaPlugin{
	
	private final GSPlayerListener playerListener = new GSPlayerListener(this);
	private final GSBlockListener blockListener = new GSBlockListener(this);
	private static final Pattern positiveInt = Pattern.compile("^\\d+$");
	public iConomy iConomy = null;
	private static Logger log = Logger.getLogger("Minecraft");
	private static Server bukkitServer = null;
	private GSItem baseItem = null;
	private Map<Integer,GSItem> items = new HashMap<Integer,GSItem>();
	private int sellTool = 0;
	private boolean buybackEnabled;
	private boolean allowBlock;
	private int reloadInterval;
	private int buyCooldown;
	private int sellCooldown;
	private boolean opCools;
	private List<String> sellMethods = new ArrayList<String>();
	private GSCalc calc = null;
	private Configuration config = new Configuration(new File("plugins/GoldStandard/config.yml"));
	private Configuration itemConfig = new Configuration(new File("plugins/GoldStandard/items.yml"));
	private boolean usePermissions = false;
	private PermissionHandler Permissions = null;
	private int CleanseTask;
	private String protectSystem;
	private ContainerProtect protection;
	//private Updater updater;
	@Override
	public void onDisable() {
		stopCleanseThread();
		calc.closeDBSession();
		log.info("Gold Standard Disabled!");
	}
	@Override
	public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        bukkitServer = getServer();
//        updater = new Updater();
//        try {
//            updater.check();
//            updater.update();
//        } catch (Exception e) {
//        }
        pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Priority.High, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLUGIN_ENABLE, new ServerICS(this), Priority.Monitor, this);
        pm.registerEvent(Type.PLUGIN_DISABLE, new ServerICS(this), Priority.Monitor, this);
        config.load();
        PluginDescriptionFile pdfFile = this.getDescription();
        usePermissions = config.getBoolean("Permissions", true);
        sellTool = config.getInt("SellTool", 283);
        buybackEnabled = config.getBoolean("Buyback", false);
        allowBlock = config.getBoolean("Allow Block", true);
        reloadInterval = config.getInt("Reload Interval", 60);
        sellCooldown = config.getInt("Cooldown.Sell", 0);
        buyCooldown = config.getInt("Cooldown.Buy", 0);
        opCools = config.getBoolean("Cooldown.Ops Obey", false);
    	ArrayList<String> tempAR = (ArrayList<String>) config.getStringList("SellMethods", null);
    	for (String item : tempAR){
    		sellMethods.add(item.toLowerCase());
    	}
        loadItems();
        calc = new GSCalc(this);
    	startCleanseThread();
        setupPermissions();
        log.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled!" );
        setupProtection();
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
	    String commandName = command.getName().toLowerCase();
	    Player player = (Player) sender;
	    	if(commandName.equalsIgnoreCase("gsworth")) {
	    	    if (!(player instanceof Player)) //in-game only command
	    	    	return false;
	    	    if (!hasPermissions(player, "goldstandard.worth")){
	    	    	player.sendMessage(ChatColor.RED.toString() +"You do not have permission to use that command.");
	    	    	return true;
	    	    }
	    	    if (args.length < 1)
	    	    	return false;
	    	    String[] items = args[0].split(",");
	    	    for (String itm : items){
	    	    	GSItem thisItem = parseGSItem(itm); 
	    	    	if (thisItem != null){
	    	    		player.sendMessage(ChatColor.YELLOW.toString() +"The current sale price of "+ 
	    	    				thisItem.getNickname() +
	    	    				" is "+iConomy.format(this.calc.getWorth(thisItem)));
	    	    		player.sendMessage(ChatColor.YELLOW.toString() +"The buy price of "+
	    	    				thisItem.getNickname() +
	    	    				" is "+iConomy.format(this.calc.checkBuyPrice(thisItem)));
	    	    	}
	    	    	else
	    	    		player.sendMessage(ChatColor.RED.toString() + itm+" is not a valid item.");
	    	    }
	    		return true;
	    	}
	    	else if (commandName.equalsIgnoreCase("gsclear")){
	    		if (!(player instanceof Player))
	    			return false;
	    		if (!hasPermissions(player, "goldstandard.clear")){
	       		player.sendMessage(ChatColor.RED.toString() +"You do not have permission to use that command.");
	    	    	return true;
	    	    }
	    		this.getCalc().clear();
	    		player.sendMessage(ChatColor.RED.toString() +"All GS records cleared.");
	   			return true;
	   		}
	    	else if (commandName.equalsIgnoreCase("gssell")){
	    		if (!commandMode()){
	    			player.sendMessage(ChatColor.RED.toString() +"Selling on command is disabled.");
	    			return true;
	    		}
	    		if (args.length < 1)
	    			return false;
	    		Inventory stuff = player.getInventory();
	    		TIntArrayList itms = new TIntArrayList();
	    		TIntArrayList qtys = new TIntArrayList();
	    		ArrayList<Boolean> iBlocks = new ArrayList<Boolean>();
	    		ArrayList<Boolean> bChange = new ArrayList<Boolean>();
	    		String[] itemStrings = args[0].split(",");
	    		String[] qtyStrings = null;
	    		if (args.length == 2)
	    			qtyStrings = args[1].split(",");
	    		else
	    			return false;
	    	    for (String itm : itemStrings){
	    	    	GSItem thisItem = parseGSItem(itm); 
	    	    	if (thisItem != null)
	    	    		itms.add(thisItem.getTypeId());
	    	    	else
	    	    		player.sendMessage(itm+" is not a valid item or item number.");
	    	    }
	    	    for (int i=0;i<qtyStrings.length;i++){
	    	    	if (isPositiveInt(qtyStrings[i])){
	    	    		qtys.add(Integer.parseInt(qtyStrings[i]));
	    	    		iBlocks.add(false);
	    	    		bChange.add(false);
	    	    	}
	    	    	else if (qtyStrings[i].length() > 1){
			    		if (isPositiveInt(qtyStrings[i].substring(0, qtyStrings[i].length()-1))){
			    			if (qtyStrings[i].charAt(qtyStrings[i].length()-1) == '~'){
			    				if (itms.size()<i+1){
			    					player.sendMessage("Item-quantity mismatch, more quantities than items.");
			    					return true;
			    				}
			    				qtys.add(Integer.parseInt(qtyStrings[i].substring(0, qtyStrings[i].length()-1)));
			    				if (getGSItem(itms.get(i)).blockAllowed()){
			    					iBlocks.add(true);
			    					bChange.add(false);
			    				}
			    				else{
			    					iBlocks.add(false);
			    					bChange.add(false);
			    				}
			    			}
			    			else if (qtyStrings[i].charAt(qtyStrings[i].length()-1) == '*'){
			    				if (itms.size()<i+1){
			    					player.sendMessage("Item-quantity mismatch, more quantities than items.");
			    					return true;
			    				}
			    				qtys.add(Integer.parseInt(qtyStrings[i].substring(0, qtyStrings[i].length()-1)));
			    				if (getGSItem(itms.get(i)).blockAllowed()){
			    					iBlocks.add(true);
			    					bChange.add(true);
			    				}
			    				else{
			    					iBlocks.add(false);
			    					bChange.add(false);
			    				}
			    			}
				   			else{
				   				player.sendMessage(qtyStrings[i]+" is not a valid quantity.");
				    			continue;
				    		}
			    		}
			    		else{
			    			player.sendMessage(qtyStrings[i]+" is not a valid quantity.");
			    			continue;
			    		}
	    	    	}
		    		else{
		   				player.sendMessage(qtyStrings[i]+" is not a valid quantity.");
		   				continue;
		   			}
	   	    		//TODO: add option for selling/buying all items in inventory
//	   	    		if (qtyStrings[i].charAt(0) == '#'){
//	   	    			int totalInInventory = 0;
//	   	    			for (ItemStack is : stuff.getContents()){
//	   	    				if (is == null)
//	   	    					continue;
//	   	    				if (is.getTypeId() == getGSItem(itemStrings[i]))
//	   	    					totalInInventory += is.getAmount();
//	   	    				else if ((is.getTypeId() == gsi.getBlock()) && includeBlocks)
//	   	    					totalInInventory += is.getAmount()*9;
//	   	    			}
//	   	    		}
	   	    	}
	   	    	long timeSinceLast = getCalc().timeSinceSold(player);
	   	    	if (timeSinceLast < getSellCooldown()){
	   	    		if ((getSellCooldown()-timeSinceLast) >= 1000)
	   	    			player.sendMessage(ChatColor.RED.toString() +"You must wait "+
	   	    					(getSellCooldown()-timeSinceLast)/1000 +" seconds before you may sell again");
	   	    		else
	   	    			player.sendMessage(ChatColor.RED.toString() +"You must wait "+
	   	    					(getSellCooldown()-timeSinceLast) +" miliseconds before you may sell again");
	   	    		return true;
	   	    	}	    	    	
	   	    	if (itms.size() != qtys.size()){
	   	    		player.sendMessage(ChatColor.RED.toString() +"You must specify a quantity for each item specified.");
	   	    		return true;
	   	    	}
	   	    	for (int i=0;i<itms.size();i++)
		   			new SellProcedure(this,player,stuff,iBlocks.get(i),bChange.get(i)).execute(itms.get(i), qtys.get(i));
				return true;
	    	}
	    	else if (commandName.equalsIgnoreCase("gsbuy")){
	    		if (!commandMode()){
	    			player.sendMessage(ChatColor.RED.toString() +"Buying on command is disabled.");
	    			return true;
	    		}
	    		if (args.length < 1)
	    			return false;
		   		Inventory stuff = player.getInventory();
		   		//TODO: Use TIntIntHashMap instead?
		   		//Is there a way to sanitize input through doing it that way?
		   		TIntArrayList itms = new TIntArrayList();
		   		TIntArrayList qtys = new TIntArrayList();
	    		String[] itemStrings = args[0].split(",");
	    		String[] qtyStrings = null;
	    		if (args.length == 2)
	    			qtyStrings = args[1].split(",");
	    		else
	    			return false;
	   	    	for (String itm : itemStrings){
	   	    		GSItem thisItem = parseGSItem(itm); 
	   	    		if (thisItem != null)
	   	    			itms.add(thisItem.getTypeId());
    	    		else
    	    			player.sendMessage(itm+" is not a valid item or item number.");
	    	    }
	    	    for (int i=0;i<qtyStrings.length;i++){
	    	   		if (isPositiveInt(qtyStrings[i]))
	    	   			qtys.add(Integer.parseInt(qtyStrings[i]));
		    		else{
		   				player.sendMessage(qtyStrings[i]+" is not a valid quantity.");
		   				continue;
		   			}
	   	    	}
	   	    	if (itms.size() != qtys.size()){
	   	    		player.sendMessage(ChatColor.RED.toString() +"You must specify a quantity for each item specified.");
	   	    		return true;
    	    	}
	   	    	long timeSinceLast = calc.timeSinceBought(player);
	    	    if (timeSinceLast < getBuyCooldown()){
	    	    	if ((getBuyCooldown()-timeSinceLast) >= 1000)
	    	    		player.sendMessage(ChatColor.RED.toString() +"You must wait "+
	    	    				(getBuyCooldown()-timeSinceLast)/1000 +" seconds before you may buy again");
	    	    	else
	    	    		player.sendMessage(ChatColor.RED.toString() +"You must wait "+
	    	    				(getBuyCooldown()-timeSinceLast) +" miliseconds before you may buy again");
	    	    	return true;
	    	    }
	    	    for (int i=0;i<itms.size();i++)
		    		new BuyProcedure(this,player,stuff).execute(itms.get(i), qtys.get(i));
				return true;
	    	}
	    	else if (commandName.equalsIgnoreCase("gshistory")){
	   			if (!(player instanceof Player))
	   				return false;
	   			if (!hasPermissions(player, "goldstandard.history")){
	    	    	player.sendMessage(ChatColor.RED.toString() +
	    	    	"You do not have permission to use that command.");
	    		   		return true;
	    	    	}
	    		if (args.length < 1)
	    			return false;
	   			String itemID = args[0];
	   			String type = "";
	   			if (args.length < 2)
	   				type = "net";
	   			else
	   				type = args[1];
	   			player.sendMessage(ChatColor.YELLOW.toString() + calc.getTransactions(itemID, type));
	   			return true;
	   		}
	    	else if (commandName.equalsIgnoreCase("gsadd")){
	    		if (args.length != 1)
	    			return false;
	    		TIntArrayList itms = new TIntArrayList();
	    		ArrayList<Boolean> iBlocks = new ArrayList<Boolean>();
	    		String[] itemStrings = args[0].split(",");
	    		for (String itm : itemStrings){
	    	    	GSItem thisItem = parseGSItem(itm);
	    	    	if (thisItem != null){
	    	    		itms.add(thisItem.getTypeId());
	    	    		iBlocks.add(false);
	    	    	}
	    	    	else{
	    	    		if (itm.length() > 1){
	    	    			GSItem truncItem = parseGSItem(itm.substring(0,itm.length()-1));
	    	    			if (truncItem != null && itm.charAt(itm.length()-1)=='~'){
	    	    	    		itms.add(truncItem.getTypeId());
	    	    	    		iBlocks.add(true);	    	    				
	    	    			}
	    	    			else
		    	    			player.sendMessage(itm+" is not a valid item name or number.");
	    	    		}
	    	    		else
	    	    			player.sendMessage(itm+" is not a valid item name or number.");
	    	    	}
	    	    }
	    		for (int i=0;i<itms.size();i++)
		   			getCalc().getPlayer(player.getName()).addSellItem(itms.get(i), iBlocks.get(i));
	    		displayItemList(player, false);
	    		return true;
			}
			else if (commandName.equalsIgnoreCase("gsremove")){
				if (args.length != 1)
					return false;
				
				TIntArrayList itms = new TIntArrayList();
				String[] itemStrings = args[0].split(",");
				for (String itm : itemStrings){
	    	    	GSItem thisItem = parseGSItem(itm);
	    	    	if (thisItem != null)
	    	    		if (getCalc().getPlayer(player.getName()).isInSellList(thisItem.getTypeId())){
	    	    			itms.remove(thisItem.getTypeId());
	    	    			player.sendMessage(ChatColor.YELLOW.toString() + thisItem.getNickname()+" was removed from you sell list.");
	    	    		}
	    	    		else
	    	    			player.sendMessage(ChatColor.RED.toString() + thisItem.getNickname()+" is not in your sell list.");
	    	    	else
    	    			player.sendMessage(ChatColor.RED.toString() + itm+" is not a valid item name or number.");
				}
				return true;
			}
			else if (commandName.equals("gsset")){
				if (args.length < 2)
					return false;
				if (isPositiveInt(args[0])){
					GSItem gsi = getGSItem(Integer.parseInt(args[1]));
					if (gsi != null){
						if (gsi.canBeBought()){
							if (isPositiveInt(args[1])){
								getCalc().getPlayer(player.getName()).setBuyItem(gsi.getTypeId());
								getCalc().getPlayer(player.getName()).setBuyQty(Integer.parseInt(args[1]));
								displayItemList(player, true);
							}
							else
								player.sendMessage(ChatColor.RED.toString() + args[1]+ " is not a valid quantity.");
						}
						else
							player.sendMessage(ChatColor.RED.toString() + gsi.getNickname()+ " cannot be bought.");
					}
					else
						player.sendMessage(ChatColor.RED.toString() + args[0] + " Is not a valid item.");
				}
				else{
					GSItem gsi = getGSItem(args[0]);
					if (gsi != null){
						if (gsi.canBeBought()){
							if (isPositiveInt(args[1])){
								getCalc().getPlayer(player.getName()).setBuyItem(gsi.getTypeId());
								getCalc().getPlayer(player.getName()).setBuyQty(Integer.parseInt(args[1]));
								displayItemList(player, true);
							}
							else
								player.sendMessage(ChatColor.RED.toString() + args[1]+ " is not a valid quantity.");
						}
						else
							player.sendMessage(ChatColor.RED.toString() + gsi.getNickname()+ " cannot be bought.");
					}
					else
						player.sendMessage(ChatColor.RED.toString() + args[0] + " Is not a valid item.");
				}
				return true;
			}
	    	else if (commandName.equalsIgnoreCase("gslist")){
	    		if (args.length < 1)
	    			return false;
	    		if (args[0].equalsIgnoreCase("buy"))
	    			displayItemList(player, true);
	    		else if (args[0].equalsIgnoreCase("sell"))
	    			displayItemList(player, false);
	    		return true;
	    	}
	    return false;
	}
    private void setupPermissions() {
    	if (!usePermissions)
    		return;
        Plugin permissions = this.getServer().getPluginManager().getPlugin("Permissions");
        if (Permissions == null) {
            if (permissions != null) 
                this.getServer().getPluginManager().enablePlugin(permissions);
                Permissions = ((Permissions) permissions).getHandler();
        }
    }
    public boolean hasPermissions(Player p, String s) {
        if (!usePermissions && !s.equalsIgnoreCase("goldstandard.clear"))
        	return true;
        else if (Permissions != null)
            return Permissions.has(p, s); 
        else
            return p.isOp();
    }
	private void setupProtection(){
		protectSystem = getConfig().getString("Protection","none");
		
		if (protectSystem.equalsIgnoreCase("none")){
			log.info("[GoldStandard] No protection system was specified, block protection is disabled.");
			protection = new NoneProtect(this);
		}
		else if (protectSystem.equalsIgnoreCase("LWC")){
			log.info("[GoldStandard] Block protection set to use LWC.");
			protection = new LWCProtect(this);
		}
		else{
			protection = new NoneProtect(this);
			log.info("[GoldStandard] An invalid protection system was specified, block protection is disabled.");
			protectSystem = "None";
		}		
	}
	public void startCleanseThread(){
        CleanseTask = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
			public void run() {
            	if (calc.needsCleaning())
            		calc.clearOld();
            	for (String pName : calc.getPlayers()){
                	getCalc().storePlayer(pName);
                	getCalc().loadPlayer(pName);
            	}
            }
        }, 60 * 20L, reloadInterval*60 * 20L );
	}
	public void stopCleanseThread(){
		this.getServer().getScheduler().cancelTask(CleanseTask);
	}
	private void loadItems(){
		itemConfig.load();
        List<String> tempItems = itemConfig.getKeys("Items"); 
        for (String item : tempItems){
        	String name = item;
        	int id = itemConfig.getInt("Items."+item+".Id",0);
        	boolean allowBlock = itemConfig.getBoolean("Items."+item+".Allow Block", this.allowBlock);
        	boolean buyback = itemConfig.getBoolean("Items."+item+".Buyback", this.buybackEnabled);
        	GSType gst = GSItem.GSType.valueOf(itemConfig.getString("Items."+item+".Type").toLowerCase());
        	
        	double price;
        	double min;
        	double max;
        	double ratio;
        	double relation;
        	GSItem gsi;
        	switch(gst){
	        	case base:
	        		price = itemConfig.getDouble("Items."+item+".Price",0);
	        		min = itemConfig.getDouble("Items."+item+".Minimum",0);
	        		max = itemConfig.getDouble("Items."+item+".Maximum",0);
	        		ratio = itemConfig.getDouble("Items."+item+".Ratio",0);
	        		gsi = new GSItem(name,id,allowBlock,buyback,gst,price,min,max,ratio);
	        		items.put(id,gsi);
	        		baseItem = gsi;
	        		break;
	        	case relative:
	        		relation = itemConfig.getDouble("Items."+item+".Relation",0);
	        		gsi = new GSItem(name,id,allowBlock,buyback,gst,relation);
	        		items.put(id,gsi);
	        		break;
	        	case independent:
	        		price = itemConfig.getDouble("Items."+item+".Price",0);
	        		min = itemConfig.getDouble("Items."+item+".Minimum",0);
	        		max = itemConfig.getDouble("Items."+item+".Maximum",0);
	        		ratio = itemConfig.getDouble("Items."+item+".Ratio",0);
	        		gsi = new GSItem(name,id,allowBlock,buyback,gst,price,min,max,ratio);
	        		items.put(id,gsi);
	        		break;
	        	case fixed:
	        		price = itemConfig.getDouble("Items."+item+".Price",0);
	        		gsi = new GSItem(name,id,allowBlock,buyback,gst,price);
	        		items.put(id,gsi);
	        		break;
	        	default:
	        		log.warning("[GoldStandard] Error when loading item "+name+"\n"+
	        				"    "+gst.toString()+" is an invalid item type.");
	        		break;
        	}
        }
        log.info("[GoldStandard] "+items.size()+" Items loaded.");
	}
	//Re-inserted to display removed items to players
    public static String formatMaterialName(Material mat){
        String toOut = "";
        String oldString = mat.name().toLowerCase();
        for (int i=0;i < oldString.length();i++){
        if (oldString.charAt(i) == '_')
        toOut += ' ';
        else
        toOut += oldString.charAt(i);
        }
        return toOut;
       }
	/** Gets a GSItem if the string input is valid (valid itemID or item nickname)
	 *  Returns null if invalid 
	 * 
	 * @param item - String to parse
	 * @return the GSItem you're looking for (or null if not found) 
	 */
	public GSItem parseGSItem(String item){
		if (isPositiveInt(item)){
			int itemID = Integer.parseInt(item);
			if (validItem(itemID)){
				return getGSItem(itemID);
			}
		}
		return getGSItem(item);
	}
	/** Gets the GSItem of a valid sellable item by it's itemID. If the item is not valid, returns 0
	 * 
	 * @param ItemID - the nickname to search for in the list of valid items
	 * @return the itemID of a valid item's 
	 */
	public GSItem getGSItem(int itemID){
		return items.get(itemID);
	}
	/** Gets the GSItem of a valid sellable item by it's nickname. If the item is not valid, returns null
	 * 
	 * @param nickname - the nickname to search for in the list of valid items
	 * @return the itemID of a valid item's 
	 */
	public GSItem getGSItem(String nickname){
		for (GSItem gsi : items.values()){
			if (gsi.getNickname().equalsIgnoreCase(nickname))
				return gsi;
		}
		return null;
	}
	public boolean validItem(int itemID){
		return items.containsKey(itemID);
	}
	public boolean validBuy(Player player){
		GSItem myBuyItem = getGSItem(getCalc().getPlayer(player.getName()).getBuyItem());
		if (myBuyItem == null){
			player.sendMessage(ChatColor.RED.toString() +
					"The Item "+GoldStandard.formatMaterialName(Material.getMaterial(calc.getPlayer(player.getName()).getBuyItem())) +
					" is no longer valid.");
			player.sendMessage(ChatColor.RED.toString() +"Please reset it");
			return false;
		}			
		else if (!myBuyItem.canBeBought()){
			player.sendMessage(ChatColor.RED.toString() +
					"The Item "+GoldStandard.formatMaterialName(Material.getMaterial(calc.getPlayer(player.getName()).getBuyItem())) +
					" can no longer be bought.");
			player.sendMessage(ChatColor.RED.toString() +"Please reset it");
			return false;
		}
		return true;
	}
	public boolean validSale(Player player, int itemID){
		if (!validItem(itemID)){
			player.sendMessage(ChatColor.RED.toString() +
					"The Item "+GoldStandard.formatMaterialName(Material.getMaterial(itemID)) +
					" is no longer valid.");
			player.sendMessage(ChatColor.RED.toString() +"It will be removed from your sell list.");
			getCalc().getPlayer(player.getName()).removeSellItem(itemID);
			return false;
		}
		return true;
	}
	private void displayItemList(Player player, boolean buyItem){
		if (buyItem){
			if (!validBuy(player))
				return;
			player.sendMessage(ChatColor.YELLOW.toString() + "You are currently buying:");
			player.sendMessage(ChatColor.YELLOW.toString() +
					getCalc().getPlayer(player.getName()).getBuyQty()+" "+
					getGSItem(getCalc().getPlayer(player.getName()).getBuyItem()).getNickname());
		}
		else{
			player.sendMessage(ChatColor.YELLOW.toString() + "You are currently selling:");
			if (getCalc().getPlayer(player.getName()).getSellList().keys().length == 0){
					player.sendMessage(ChatColor.RED.toString() +"NOTHING!");
					return;
			}
			for (int itemID : getCalc().getPlayer(player.getName()).getSellList().keys()){
				if (!validSale(player,itemID))
						continue;
				player.sendMessage(ChatColor.YELLOW.toString() + getGSItem(itemID).getNickname()+
						(getCalc().getPlayer(player.getName()).itemCanBeBlock(itemID)?"~":""));
			}
		}
	}
    //Organization stuff goes here
    public GSCalc getCalc(){
    	return this.calc;
    }
    public Configuration getConfig(){
    	return this.config;
    }
    public GSItem getBaseItem(){
    	return this.baseItem;
    }
    public int getSellTool(){
    	return this.sellTool;
    }
    public boolean chestMode(){
    	return sellMethods.containsAll(Collections.singleton("chest"));
    }
    public boolean furnaceMode(){
    	return sellMethods.containsAll(Collections.singleton("furnace"));
    }
    public boolean dispenserMode(){
    	return sellMethods.containsAll(Collections.singleton("dispenser"));
    }
    public boolean commandMode(){
    	return sellMethods.containsAll(Collections.singleton("command"));
    }
    public static boolean isPositiveInt(String str){
    	return positiveInt.matcher(str).matches();
    }
    public String getProtectionType(){
    	return protectSystem;
    }
    public ContainerProtect getProtection(){
    	return protection;
    }
    public static Server getBukkitServer() {
        return bukkitServer;
    }
    public Map<Integer,GSItem> getItems(){
    	return this.items;
    }
    public int getBuyCooldown(){
    	return this.buyCooldown;
    }
    public int getSellCooldown(){
    	return this.sellCooldown;
    }
    public boolean opsObeyCooldown(){
    	return this.opCools;
    }
}