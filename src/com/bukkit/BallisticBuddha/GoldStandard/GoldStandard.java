package com.bukkit.BallisticBuddha.GoldStandard;

import com.bukkit.BallisticBuddha.GoldStandard.GSItem.GSType;
import com.bukkit.BallisticBuddha.GoldStandard.Protect.*;
import com.bukkit.BallisticBuddha.GoldStandard.Transactions.*;
//import com.bukkit.BallisticBuddha.GoldStandard.griefcraft.*;
import com.iConomy.*;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
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
	@version 0.8
	
	Gold Standard plugin for bukkit
	
*/
public class GoldStandard extends JavaPlugin{
	
	//private final GSPlayerListener playerListener = new GSPlayerListener(this);
	private final GSBlockListener blockListener = new GSBlockListener(this);
	private static final Pattern positiveInt = Pattern.compile("^\\d+$");
	public iConomy iConomy = null;
	private static Logger log = Logger.getLogger("Minecraft");
	private static Server bukkitServer = null;
	private GSItem baseItem = null;
	private TreeMap<Integer,GSItem> items = new TreeMap<Integer,GSItem>();
	private int sellTool = 0;
	private boolean buybackEnabled;
	private int reloadInterval;
	private List<String> sellMethods = new ArrayList<String>();
	private GSCalc calc = null;
	private Configuration config = new Configuration(new File("plugins/GoldStandard/config.yml"));
	private Configuration itemConfig = new Configuration(new File("plugins/GoldStandard/items.yml"));
	private PermissionHandler Permissions = null;
	private int CleanseTask;
	private String protectSystem;
	private ContainerProtect protection;
	//private Updater updater;
	//public final HashMap<Player, ArrayList<Block>> gsUsers = new HashMap<Player, ArrayList<Block>>();
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
        pm.registerEvent(Type.PLUGIN_ENABLE, new ServerICS(this), Priority.Monitor, this);
        pm.registerEvent(Type.PLUGIN_DISABLE, new ServerICS(this), Priority.Monitor, this);
        config.load();
        loadItems();
        calc = new GSCalc(this);
        PluginDescriptionFile pdfFile = this.getDescription();
        sellTool = config.getInt("SellTool", 283);
        buybackEnabled = config.getBoolean("Buyback", false);
        reloadInterval = config.getInt("Reload Interval", 60);
    	ArrayList<String> tempAR = (ArrayList<String>) config.getStringList("SellMethods", null);
    	for (String item : tempAR){
    		sellMethods.add(item.toLowerCase());
    	}
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
	    			player.sendMessage(ChatColor.YELLOW.toString() +"The current going price of "+ 
	    					getGSItem(getBaseItem().getTypeId()).getNickname() +
	    					" is "+iConomy.format(this.getCalc().getWorth(getBaseItem())));
	    			return true;
	    		}
	    		if (commandName.equalsIgnoreCase("gsclear")){
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
	    		if (commandName.equalsIgnoreCase("gssell")){
	    			if (!commandMode()){
	    				player.sendMessage(ChatColor.RED.toString() +"Selling on command is disabled.");
	    				return true;
	    			}
	    			if (args.length < 1)
	    				return false;
	    			Inventory stuff = player.getInventory();
	    			int amt = 0;
	    			boolean includeBlocks = false;
	    			boolean blockChange = false;
	    			if (isPositiveInt(args[0]))
	    				 amt = Integer.parseInt(args[0]);//I have faith in my RegEx
	    			else if (isPositiveInt(args[0].substring(0, args[0].length()-1))){
	    				if (args[0].charAt(args[0].length()-1) == '~'){
	    					amt = Integer.parseInt(args[0].substring(0, args[0].length()-1));
	    					includeBlocks = true;
	    				}
	    				else if (args[0].charAt(args[0].length()-1) == '*'){
	    					amt = Integer.parseInt(args[0].substring(0, args[0].length()-1));
	    					includeBlocks = true;
	    					blockChange = true;
	    				}
	    			}
	    			else
	    				return false;
	    			SellProcedure action = new SellProcedure(this,player,stuff,includeBlocks,blockChange);
	    			action.execute(getBaseItem().getTypeId(), amt);
					return true;
	    		}
	    		if (commandName.equalsIgnoreCase("gsbuy")){
	    			if (!commandMode()){
	    				player.sendMessage(ChatColor.RED.toString() +"Buying on command is disabled.");
	    				return true;
	    			}
	    			if (args.length < 1)
	    				return false;
		    		Inventory stuff = player.getInventory();
		    		int amt = 0;
		    		if (isPositiveInt(args[0]))
	    				 amt = Integer.parseInt(args[0]);
		    		else
		    			return false;
		    		BuyProcedure action = new BuyProcedure(this,player,stuff);
	    			action.execute(getBaseItem().getTypeId(), amt);
					return true;
	    		}
	    		if (commandName.equalsIgnoreCase("gshistory")){
	    			if (!(player instanceof Player))
	    				return false;
	    			if (!hasPermissions(player, "goldstandard.history")){
	    	    		player.sendMessage(ChatColor.RED.toString() +"You do not have permission to use that command.");
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
	    return false;
	}
	@Deprecated
	/**
	 * The material format is now deprecated as most transactions are now labeled with a GSItems nickname
	 */
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
    private void setupPermissions() {
        Plugin permissions = this.getServer().getPluginManager().getPlugin("Permissions");
        if (Permissions == null) {
            if (permissions != null) 
                this.getServer().getPluginManager().enablePlugin(permissions);
                Permissions = ((Permissions) permissions).getHandler();
        }
    }
    public boolean hasPermissions(Player p, String s) {
        if (Permissions != null) {
            return Permissions.has(p, s);
        } else {
            return p.isOp();
        }
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
        	boolean allowBlock = itemConfig.getBoolean("Items."+item+".Allow Block", false);
        	boolean buyback = itemConfig.getBoolean("Items."+item+".Buyback", buybackEnabled);
        	GSType gst = GSItem.GSType.valueOf(itemConfig.getString("Items."+item+".Type"));
        	
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
        log.info("[GoldStandard] Loaded "+items.size()+" items.");
	}
	/** Gets the GSItem of a valid sellable item by it's itemID. If the item is not valid, returns 0
	 * 
	 * @param ItemID - the nickname to search for in the list of valid items
	 * @return the itemID of a valid item's 
	 */
	public GSItem getGSItem(int itemID){
		return items.get(itemID);
	}
	/** Gets the GSItem of a valid sellable item by it's nickname. If the item is not valid, returns 0
	 * 
	 * @param nickname - the nickname to search for in the list of valid items
	 * @return the itemID of a valid item's 
	 */
	public GSItem getGSItem(String nickname){
		//TODO: This search does not take advantage of TreeMap's O(logn) time
		for (GSItem gsi : items.values()){
			if (gsi.getNickname().equalsIgnoreCase(nickname))
				return gsi;
		}
		return null;
	}
	public boolean validItem(int itemID){
		return items.containsKey(itemID);
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
}