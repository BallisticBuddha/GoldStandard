package com.bukkit.BallisticBuddha.GoldStandard;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.bukkit.util.config.Configuration;

import java.sql.*;
/**
 * Class for handling all MYSQL backend tasks
 * TODO: make this abstract and extend MYSQL and SQLite child classes
 */
public class GSData {
	private double base;
	private int duration; //in days
	private int transactions;
	private double ratio;
	private double min;
	private Object CalcLock = new Object();
	private static Logger log = Logger.getLogger("Minecraft");	private GoldStandard gs;
	private Configuration config;
	private Connection conn;
	
	public void initialize(){
		gs = (GoldStandard) GoldStandard.getBukkitServer().getPluginManager().getPlugin("GoldStandard");
		this.config = gs.getConfig();
		
		this.base = this.config.getDouble("base", 100.0);
		this.min = (this.config.getDouble("minimum", 0.0));
		this.duration = this.config.getInt("duration", 7);
		this.ratio = this.config.getDouble("ratio", .1);
		
		this.initSQL();
		this.createTable();
		this.transactions = this.getTransactions();
	}
	
	//thread locked to make all operations happen in a single connection
	public void initSQL(){
		synchronized(CalcLock){
			try{
				Class.forName("com.mysql.jdbc.Driver");
				String sqlurl = "jdbc:mysql://"+config.getString("MySQL.hostname","localhost")+":"+config.getString("MySQL.port","3306")+"/"+config.getString("MySQL.database","minecraft");
				conn = DriverManager.getConnection(sqlurl, config.getString("MySQL.username"), config.getString("MySQL.password"));
			}
		    catch (SQLException ex){
		      log.severe("An error occurred initiating the connection" + "\n" +ex);
		    } 
		    catch (ClassNotFoundException ex) {
		    	log.severe("*sigh* Another MySQL connectivity error" + "\n" +ex);
			}
		}
	}
	private void createTable(){
		synchronized (CalcLock){
			Statement stmt = null;
			try{
				stmt = conn.createStatement();
				stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `gslog` (" +
						"`pkgslog` int(10) unsigned NOT NULL AUTO_INCREMENT," +
						"`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," +
						"`amount` int(10) unsigned NOT NULL DEFAULT '1'," +
						"`user` varchar(45) DEFAULT NULL," +
						"PRIMARY KEY (`pkgslog`)" +
						") ENGINE=InnoDB DEFAULT CHARSET=latin1");
			}
			catch(SQLException ex){
				log.severe("Error when creating gslog." + "\n" +ex);
			}
		}
	}
	private int getTransactions(){
		ArrayList<Integer> ar = new ArrayList<Integer>();
		int toRet = 0;
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				stmt = conn.prepareStatement("SELECT amount from gslog");
				rs = stmt.executeQuery();
				
				while (rs.next()){
					ar.add(rs.getInt("amount"));
				}
			}
			catch(SQLException ex){
				log.warning("Error retrieving transactions from gslog" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(rs);
				SQLUtils.closeQuietly(stmt);
			}
		}
		for (int i : ar){
			toRet += i;
		}
		return toRet;
	}
	public void addEntry(int amt, String usr){
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gslog (amount,user) VALUES (?,?)");
				stmt.setInt(1, amt);
				stmt.setString(2, usr.toLowerCase());
				stmt.execute();
				this.transactions += amt;
			}
			catch(SQLException ex){
				log.warning("Error when adding GoldStandard entry" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
	}
	public void forceIncrement(int amt){
		this.transactions += amt;
	}
	public void addEntryNI(int amt, String usr){
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gslog (amount,user) VALUES (?,?)");
				stmt.setInt(1, amt);
				stmt.setString(2, usr.toLowerCase());
				stmt.execute();
			}
			catch(SQLException ex){
				log.warning("Error when adding GoldStandard entry" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
	}
	public void clear(){
		synchronized(CalcLock){
			Statement stmt = null;
			try{
				stmt = conn.createStatement();
				stmt.executeUpdate("delete from gslog"); //requires non-safe mode
				this.transactions = 0;
			}
			catch(SQLException ex){
				log.warning("Error when clearing GoldStandard table" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
	}
	public void clearOld(){
		synchronized(CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("delete from gslog WHERE time < (NOW() - INTERVAL ? DAY)");
				stmt.setInt(1, this.duration);
				stmt.execute();
				log.info("[GoldStandard] Cleared out records older than "+this.duration + " days.");
			}
			catch(SQLException ex){
				log.warning("Error when clearing GoldStandard table" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
		this.transactions = getTransactions();
	}
	public boolean needsCleaning(){
		synchronized(CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				stmt = conn.prepareStatement("SELECT pkgslog FROM gslog WHERE time < (NOW() - INTERVAL ? DAY)");
				stmt.setInt(1, this.duration);
				rs = stmt.executeQuery();
				if(!rs.next())
					return false;
			}
			catch(SQLException ex){
				log.warning("Error when querying GoldStandard table" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
		return true;
	}
	public int getMyTransactions(){
		return transactions;
	}
	public double getRatio(){
		return ratio;
	}
	public double getBase(){
		return base;
	}
	public double getMin() {
		return min;
	}
}
