package com.bukkit.BallisticBuddha.GoldStandard;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;
import java.text.*;

/**
	@author BallisticBuddha
	@version 1.0
	
	Gold Standard plugin for bukkit
	
	TODO: hook into LWC or other block protection system to disable selling from private furnaces
	TODO: create SQLite version of GSData
*/
public class GoldStandard extends JavaPlugin{
	
	private final GSPlayerListener playerListener = new GSPlayerListener(this);
	private final GSBlockListener blockListener = new GSBlockListener(this);
	private static PluginListener pluginListener = null;
	private static Logger log = Logger.getLogger("Minecraft");
	private static iConomy iConomy = null;
	private static Server Server = null;
	private int sellItem = 0;
	private int sellTool = 0;
	private GSCalc calc = null;
	private Configuration config = new Configuration(new File("plugins/GoldStandard/config.yml"));
	public static PermissionHandler Permissions = null;
	private DecimalFormat df = new DecimalFormat("#.##");
	private int CleanseTask;
	
	public final HashMap<Player, ArrayList<Block>> gsUsers = new HashMap<Player, ArrayList<Block>>();
	//private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
	
	@Override
	public void onDisable() {
		this.getServer().getScheduler().cancelTask(CleanseTask);
		log.info("Gold Standard Disabled!");
	}
	@Override
	public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        Server = getServer();
        pluginListener = new PluginListener();
        pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Priority.High, this);
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, pluginListener, Priority.Monitor, this);
        //getCommand("debug").setExecutor(new SampleDebugCommand(this));
        config.load();
        calc = new GSCalc (this);
        PluginDescriptionFile pdfFile = this.getDescription();
        //Start synchronous cleanser thread
        CleanseTask = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
            	if (calc.needsCleaning())
            		calc.clearOld();
            }
        }, 60 * 21L, 360 * 21L);
        sellItem = config.getInt("Item", 266);
        sellTool = config.getInt("SellTool", 283);
        setupPermissions();
        log.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled!" );
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
	    			player.sendMessage(ChatColor.YELLOW.toString() +"The current going price of "+ formatMaterialName(Material.getMaterial(sellItem)) +" is "+df.format(this.getCalc().getWorth())+ " "+ getiConomy().getBank().getCurrency());
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
	    return false;
	}
    private String formatMaterialName(Material mat){
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
    public static Server getBukkitServer() {
        return Server;
    }
    public static iConomy getiConomy() {
        return iConomy;
    }
    public static boolean setiConomy(iConomy plugin) {
        if (iConomy == null) {
            iConomy = plugin;
        } else {
            return false;
        }
        return true;
    }
}