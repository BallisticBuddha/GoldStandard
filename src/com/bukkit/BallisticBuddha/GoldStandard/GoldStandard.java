package com.bukkit.BallisticBuddha.GoldStandard;

import com.iConomy.*;
import com.iConomy.system.Holdings;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ContainerBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;
import java.text.*;

/**
	@author BallisticBuddha
	@version 0.6
	
	Gold Standard plugin for bukkit
	
	TODO: create SQLite version of GSData
*/
public class GoldStandard extends JavaPlugin{
	
	private final GSPlayerListener playerListener = new GSPlayerListener(this);
	private final GSBlockListener blockListener = new GSBlockListener(this);
	public iConomy iConomy = null;
	private static Logger log = Logger.getLogger("Minecraft");
	private static Server bukkitServer = null;
	private int sellItem = 0;
	private int sellTool = 0;
	private boolean buybackEnabled;
	private int reloadInterval;
	private List<String> sellMethods = new ArrayList<String>();
	private GSCalc calc = null;
	private Configuration config = new Configuration(new File("plugins/GoldStandard/config.yml"));
	private Configuration itemConfig = new Configuration(new File("plugins/GoldStandard/items.yml"));
	public static PermissionHandler Permissions = null;
	private int CleanseTask;
	private String protectSystem;
	private ContainerProtect protection;
	
	public final HashMap<Player, ArrayList<Block>> gsUsers = new HashMap<Player, ArrayList<Block>>();
	
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
        pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Priority.High, this);
        pm.registerEvent(Type.PLUGIN_ENABLE, new ServerICS(this), Priority.Monitor, this);
        pm.registerEvent(Type.PLUGIN_DISABLE, new ServerICS(this), Priority.Monitor, this);
        config.load();
        itemConfig.load();
        calc = new GSCalc (this);
        PluginDescriptionFile pdfFile = this.getDescription();
        sellItem = config.getInt("Item", 266);
        sellTool = config.getInt("SellTool", 283);
        buybackEnabled = config.getBoolean("Buyback", false);
        reloadInterval = config.getInt("Reload Interval", 60);
    	ArrayList<String> tempAR = (ArrayList<String>) config.getStringList("SellMethods", null);
    	for (String item : tempAR){
    		sellMethods.add(item.toLowerCase());
    	}
        //Start synchronous cleanser thread
        CleanseTask = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
            	if (calc.needsCleaning())
            		calc.clearOld();
            }
        }, 60 * 21L, reloadInterval*60 * 21L);
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
	    	    	if (!GoldStandard.Permissions.has(player, "goldstandard.worth")){
	    	    		player.sendMessage(ChatColor.RED.toString() +"You do not have permission to use that command.");
	    	    		return true;
	    	    	}
	    			player.sendMessage(ChatColor.YELLOW.toString() +"The current going price of "+ formatMaterialName(Material.getMaterial(sellItem)) +" is "+iConomy.format(this.getCalc().getWorth()));
	    			return true;
	    		}
	    		if (commandName.equalsIgnoreCase("gsclear")){
	    			if (!(player instanceof Player))
	    				return false;
	    			if (!GoldStandard.Permissions.has(player, "goldstandard.clear")){
	    	    		player.sendMessage(ChatColor.RED.toString() +"You do not have permission to use that command.");
	    	    		return true;
	    	    	}
	    			this.getCalc().clear();
	    			player.sendMessage(ChatColor.RED.toString() +"All GS records cleared.");
	    			return true;
	    		}
	    		if (commandName.equalsIgnoreCase("gssell")){
	    			if (!(player instanceof Player))
	    				return false;
	    			if (!commandMode()){
	    				player.sendMessage(ChatColor.RED.toString() +"Selling on command is disabled.");
	    				return true;
	    			}
	    			if (!GoldStandard.Permissions.has(player, "goldstandard.sell")){
	    				player.sendMessage(ChatColor.RED.toString() +"You do not have permission to use that command.");
	    	    		return true;
	    			}
	    			Inventory stuff = player.getInventory();
	    			int amt = 0;
	    			try{
	    				 amt = Integer.parseInt(args[0]);
	    			}
	    			catch (NumberFormatException ex){
	    				return false;
	    			}
	    			if (amt < 0){
	    				player.sendMessage(ChatColor.RED.toString() +"Yeah...right.");
	    				return true;
	    			}
	    			int totalInInventory = 0;
					double totalSale = 0;
					Holdings holdings = iConomy.getAccount(player.getName()).getHoldings();
					if (holdings == null){
						player.sendMessage(ChatColor.RED.toString() + "An error occurred while retrieving your holdings :(");
						return true;
					}
					for (ItemStack is : stuff.getContents()){
						if (is != null)
							if (is.getTypeId() == getItem())
								totalInInventory += is.getAmount();
					}
					if (totalInInventory < amt){						
						player.sendMessage(ChatColor.RED.toString() +"You do not have "+amt+ " "+formatMaterialName(Material.getMaterial(getItem()))+ " in your inventory.");
						player.sendMessage(ChatColor.RED.toString() +"You only have "+totalInInventory+".");
						return true;
					}
					for (int i=0;i<amt;i++){
						totalSale += getCalc().getWorth();
						getCalc().forceIncrement(1); //increment counter once
					}
					ItemStack tis = new ItemStack(getItem(),totalInInventory-amt);
			    	stuff.remove(getItem());
			    	if (tis.getAmount() > 0)
			    		stuff.addItem(tis);
					getCalc().addEntryNI(amt,player.getName());//add to gslog without incrementing the transactions counter
					holdings.add(totalSale); //give them money
					player.sendMessage(ChatColor.GREEN.toString() + "Sold "+amt+" "+ formatMaterialName(Material.getMaterial(getItem())) + " for " +iConomy.format(totalSale));
					return true;
	    		}
	    		if (commandName.equalsIgnoreCase("gsbuy")){
	    			if (buybackEnabled){
		    			if (!(player instanceof Player))
		    				return false;
		    			if (!GoldStandard.Permissions.has(player, "goldstandard.buy")){
		    	    		player.sendMessage(ChatColor.RED.toString() +"You do not have permission to use that command.");
		    	    		return true;
		    	    	}
		    			Inventory stuff = player.getInventory();
		    			int amt = 0;
		    			try{
		    				 amt = Integer.parseInt(args[0]);
		    			}
		    			catch (NumberFormatException ex){
		    				return false;
		    			}
		    			if (amt < 0){
		    				player.sendMessage(ChatColor.RED.toString() +"Yeah...right.");
		    				return true;
		    			}
						Holdings holdings = iConomy.getAccount(player.getName()).getHoldings();
						if (holdings == null){
							player.sendMessage(ChatColor.RED.toString() + "An error occurred while retrieving your holdings :(");
							return true;
						}
		    			if (amt*calc.getWorth() > holdings.balance()){
		    				player.sendMessage(ChatColor.RED.toString() +"Insufficient Funds");
		    				player.sendMessage(ChatColor.RED.toString() + amt+" "+ formatMaterialName(Material.getMaterial(getItem())) + " would cost "+iConomy.format(amt*calc.getWorth()));
		    				return true;
		    			}
		    			double totalSale = 0.0;
						for (int i=0;i<amt;i++){
							totalSale += getCalc().getWorth();
							getCalc().forceIncrement(-1); //decrement counter once
						}
						ItemStack tis = new ItemStack(getItem(),amt);
						stuff.addItem(tis);
						getCalc().addEntryNI(-amt,player.getName());//add negative transaction to gslog without decrimating the transactions counter
						holdings.add(-totalSale); //take their money
						player.sendMessage(ChatColor.GREEN.toString() + "Bought "+amt+" "+ formatMaterialName(Material.getMaterial(getItem())) + " for " +iConomy.format(totalSale));
						return true;
	    			}
	    		}
	    return false;
	}
    public String formatMaterialName(Material mat){
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
        if (GoldStandard.Permissions == null) {
            if (permissions != null) 
                this.getServer().getPluginManager().enablePlugin(permissions);
                GoldStandard.Permissions = ((Permissions) permissions).getHandler();
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
	public void stopCleanseThread(){
		this.getServer().getScheduler().cancelTask(CleanseTask);
	}
    //Organization stuff goes here
    public GSCalc getCalc(){
    	return this.calc;
    }
    public Configuration getConfig(){
    	return this.config;
    }
    public int getItem(){
    	return this.sellItem;
    }
    public int getSellTool(){
    	return this.sellTool;
    }
    public boolean getBuyback(){
    	return this.buybackEnabled;
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