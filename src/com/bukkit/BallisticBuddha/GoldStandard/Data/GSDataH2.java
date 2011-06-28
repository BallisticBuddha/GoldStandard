package com.bukkit.BallisticBuddha.GoldStandard.Data;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;

import com.bukkit.BallisticBuddha.GoldStandard.GSItem;
import com.bukkit.BallisticBuddha.GoldStandard.GSPlayer;

/**
 * Class for handling all H2SQL backend tasks
 */
public class GSDataH2 extends GSData {

	public GSDataH2(){
		super();
		this.initSQL();
		this.createTables();
		this.countAllItems();
	}
	//thread locked to make all operations happen in a single connection
	private void initSQL(){
		synchronized(CalcLock){
			try{
				Class.forName("org.h2.Driver");
				String sqlurl = "jdbc:h2:plugins/GoldStandard/gsH2data";
				conn = DriverManager.getConnection(sqlurl, config.getString("H2SQL.username","GoldStandard"), config.getString("H2SQL.password","gsP@SSwrd"));
			}
		    catch (SQLException ex){
		      log.severe("An error occurred initiating the connection" + "\n" +ex);
		    }
		    catch (ClassNotFoundException ex) {
		    	log.severe("H2SQL connectivity error" + "\n" +ex);
			}
		}
	}
	private void createTables(){
		synchronized (CalcLock){
			Statement stmt = null;
			try{
				stmt = conn.createStatement();
				stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `gsusers` ("+
						"`pkgsusers` INT UNSIGNED NOT NULL AUTO_INCREMENT ,"+
  						"`username` VARCHAR(45) NOT NULL DEFAULT 'foobar' ,"+
  						"`buyItem` INT UNSIGNED NOT NULL DEFAULT 0 ,"+
  						"`buyQty` INT UNSIGNED NOT NULL DEFAULT 1 ,"+
  						"`sellItems` VARCHAR(45) NOT NULL DEFAULT '' ,"+
  						"`lastBought` TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:00',"+
  						"`lastSold` TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:00',"+
  						"PRIMARY KEY (`pkgsusers`) ,"+
  						"UNIQUE INDEX `name_UNIQUE` (`username` ASC)"+
  						") ENGINE = InnoDB DEFAULT CHARACTER SET = latin1");
				stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `gslog` ("+
						"`pkgslog` int(32) unsigned NOT NULL AUTO_INCREMENT,"+
						"`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,"+
						"`amount` int(10) NOT NULL DEFAULT '1',"+
						"`item` int(10) unsigned DEFAULT NULL,"+
						"`user` int(10) unsigned DEFAULT NULL,"+
						"PRIMARY KEY (`pkgslog`),"+
						"KEY `fk_user` (`user`),"+
						"CONSTRAINT `fk_user` FOREIGN KEY (`user`) REFERENCES `gsusers` (`pkgsusers`) ON DELETE SET NULL ON UPDATE CASCADE"+
						") ENGINE = InnoDB DEFAULT CHARSET = latin1");
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when creating gslog." + "\n" +ex);
			}
		}
	}
	public int countTransactions(String itemID){
		ArrayList<Integer> ar = new ArrayList<Integer>();
		int toRet = 0;
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				if (itemID.equals("*"))
					stmt = conn.prepareStatement("SELECT amount from gslog");
				else{
					stmt = conn.prepareStatement("SELECT amount from gslog where item = ?");
					stmt.setString(1, itemID);
				}
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
	@Override
	public void addEntry(int amt, String usr, int item){
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gslog (amount,user,item,time) VALUES (?,?,?,?)");
				stmt.setInt(1, amt);
				stmt.setString(2, usr.toLowerCase());
				stmt.setInt(3, item);
				stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
				stmt.execute();
				this.transValues.adjustValue(item, amt);
			}
			catch(SQLException ex){
				log.warning("Error when adding GoldStandard entry" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
	}
	@Override
	public void addEntryNI(int amt, String usr, int item){
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gslog (amount,user,item,time) VALUES (?,?,?,?)");
				stmt.setInt(1, amt);
				stmt.setString(2, usr.toLowerCase());
				stmt.setInt(3, item);
				stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
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
	@Override
	public void clear(){
		synchronized(CalcLock){
			Statement stmt = null;
			try{
				stmt = conn.createStatement();
				stmt.executeUpdate("delete from gslog"); //requires non-safe mode
				this.setAllToZero();
			}
			catch(SQLException ex){
				log.warning("Error when clearing GoldStandard table" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
	}
	@Override
	public void clearOld(){
		synchronized(CalcLock){
			PreparedStatement stmt = null;
			try{
				Timestamp limit = new Timestamp(System.currentTimeMillis()-(86400000*getDuration()));
				stmt = conn.prepareStatement("delete from gslog WHERE time < ?");
				stmt.setTimestamp(1, limit);
				int amtDeleted = stmt.executeUpdate();
				log.info("[GoldStandard] Cleared out "+ amtDeleted +" records older than "+ getDuration() + " days.");
			}
			catch(SQLException ex){
				log.warning("Error when clearing GoldStandard table" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
		countAllItems();
	}
	@Override
	public boolean needsCleaning(){
		synchronized(CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				Timestamp limit = new Timestamp(System.currentTimeMillis()-(86400000*getDuration()));
				stmt = conn.prepareStatement("SELECT pkgslog FROM gslog WHERE time < ?");
				stmt.setTimestamp(1, limit);
				rs = stmt.executeQuery();
				if(rs.next())
					return true;
			}
			catch(SQLException ex){
				log.warning("Error when querying GoldStandard table" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
		return false;
	}
	@Override
	public void closeSession() {
		synchronized(CalcLock){
			SQLUtils.closeQuietly(conn);
		}
	}
	@Override
	public int getTransactions(String itemID, boolean positive) {
		ArrayList<Integer> ar = new ArrayList<Integer>();
		int toRet = 0;
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			String operator = "<";
			if (positive)
				operator = ">";
			try{
				if (itemID.equals("*"))
					stmt = conn.prepareStatement("SELECT * from gslog where amount "+operator+" 0");
				else{
					stmt = conn.prepareStatement("SELECT * from gslog where amount "+operator+" 0 and item = ?");
					stmt.setString(1, itemID);
				}
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
	@Override
	public void loadAllPlayers() {
		playerData.clear();
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				stmt = conn.prepareStatement("SELECT * from gsusers");
				rs = stmt.executeQuery();

				while (rs.next()){
					String pname = rs.getString("username");
					GSPlayer gsp = new GSPlayer(rs.getInt("pkgsusers"),pname);
					gsp.setBuyItem(rs.getInt("buyItem"));
					gsp.setBuyQty(rs.getInt("buyQty"));
					gsp.setSellItems(rs.getString("sellItems"));
					gsp.setLastBought(rs.getTimestamp("lastBought").getTime());
					gsp.setLastSold(rs.getTimestamp("lastSold").getTime());
					playerData.put(pname, gsp);
				}
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error retrieving users from gsusers" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(rs);
				SQLUtils.closeQuietly(stmt);
			}
		}
	}
	@Override
	/** Ejects a players entry from memory (if applicable) and re-loads the data from the database
	 * 
	 * @param name - the name of the String 
	 * @return false if the user does not exist in the table, and must be inserted first
	 */
	public boolean loadPlayer(String name) {
		Boolean userexists = false;
		if (playerData.containsKey(name))
			playerData.remove("name");
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				stmt = conn.prepareStatement("SELECT * from gsusers WHERE username = ?");
				stmt.setString(1, name);
				rs = stmt.executeQuery();
				
				if (rs.next()){
					do {
						String pname = rs.getString("username");
						GSPlayer gsp = new GSPlayer(rs.getInt("pkgsusers"),pname);
						gsp.setBuyItem(rs.getInt("buyItem"));
						gsp.setBuyQty(rs.getInt("buyQty"));
						gsp.setSellItems(rs.getString("sellItems"));
						gsp.setLastBought(rs.getTimestamp("lastBought").getTime());
						gsp.setLastSold(rs.getTimestamp("lastSold").getTime());
						playerData.put(pname, gsp);
					}while (rs.next());
					userexists = true;
				}	
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error retrieving user "+name+" from gsusers" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(rs);
				SQLUtils.closeQuietly(stmt);
			}
		}
		return userexists;
	}
	@Override
	public void addPlayer(String name) {
		int id = 0;
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gsusers (username) VALUES (?)");
				stmt.setString(1, name);
				stmt.executeUpdate();

				stmt = conn.prepareStatement("SELECT * from gsusers WHERE username = ?");
				stmt.setString(1, name);
				rs = stmt.executeQuery();
				while (rs.next()){
					id = rs.getInt("pkgsusers");
				}
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when adding player"+ name + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
				SQLUtils.closeQuietly(rs);
			}
		}
		GSPlayer gsp = new GSPlayer(id, name);
		if (!playerData.containsKey(name))
			playerData.put(name, gsp);
		else
			log.severe("[GoldStandard] User "+name+" already exists!");
	}
	@Override
	public void storePlayer(String name) {
		if (!playerData.containsKey(name)){
			log.severe("[GoldStandard] Could not store user"+name+". Player was not loaded into memory!");
			return;
		}
		GSPlayer gsp = playerData.get(name);
		synchronized(CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gsusers (username,buyItem,buyQty,sellItems,lastBought,lastSold) VALUES (?,?,?,?,?,?) " +
						"ON DUPLICATE KEY UPDATE buyItem=?, buyQty=?, sellItems=?, lastBought=?, lastSold=?");
				stmt.setString(1, name);
				stmt.setInt(2, gsp.getBuyItem());/**/stmt.setInt(7, gsp.getBuyItem());
				stmt.setInt(3, gsp.getBuyQty());/**/stmt.setInt(8, gsp.getBuyQty());
				stmt.setString(4, gsp.getSellItems());/**/stmt.setString(9, gsp.getSellItems());
				stmt.setTimestamp(5, new Timestamp(gsp.getLastBought()));/**/stmt.setTimestamp(10, new Timestamp(gsp.getLastBought()));
				stmt.setTimestamp(6, new Timestamp(gsp.getLastSold()));/**/stmt.setTimestamp(11, new Timestamp(gsp.getLastSold()));
				//stmt.setInt(12, gsp.getId());
				stmt.executeUpdate();
				
				playerData.remove(gsp.getName());
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when storing user " + name + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
	}
	@Override
	public void storePlayerND(String name) {
		if (!playerData.containsKey(name)){
			log.severe("[GoldStandard] Could not store user"+name+". Player was not loaded into memory!");
			return;
		}
		GSPlayer gsp = playerData.get(name);
		synchronized(CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gsusers (username,buyItem,buyQty,sellItems,lastBought,lastSold) VALUES (?,?,?,?,?,?) " +
						"ON DUPLICATE KEY UPDATE buyItem=?, buyQty=?, sellItems=?, lastBought=?, lastSold=?");
				stmt.setString(1, name);
				stmt.setInt(2, gsp.getBuyItem());/**/stmt.setInt(7, gsp.getBuyItem());
				stmt.setInt(3, gsp.getBuyQty());/**/stmt.setInt(8, gsp.getBuyQty());
				stmt.setString(4, gsp.getSellItems());/**/stmt.setString(9, gsp.getSellItems());
				stmt.setTimestamp(5, new Timestamp(gsp.getLastBought()));/**/stmt.setTimestamp(10, new Timestamp(gsp.getLastBought()));
				stmt.setTimestamp(6, new Timestamp(gsp.getLastSold()));/**/stmt.setTimestamp(11, new Timestamp(gsp.getLastSold()));
				//stmt.setInt(12, gsp.getId());
				stmt.executeUpdate();
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when storing user " + name + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
	}
}
