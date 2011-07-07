package com.bukkit.BallisticBuddha.GoldStandard.Data;

import gnu.trove.list.array.TIntArrayList;

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
import com.bukkit.BallisticBuddha.GoldStandard.Data.GSData.SumCount;

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
				String sqlurl = "jdbc:h2:plugins/GoldStandard/gsH2data;MODE=MYSQL";
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
				stmt.executeUpdate("CREATE TABLE IF NOT EXISTS GSUSERS"+
					"(PKGSUSERS INTEGER AUTO_INCREMENT NOT NULL,"+
					"USERNAME VARCHAR(45) DEFAULT 'foobar' NOT NULL,"+
					"BUYITEM INTEGER DEFAULT 0 NOT NULL,"+
					"BUYQTY INTEGER DEFAULT 1 NOT NULL,"+
					"SELLITEMS VARCHAR(160) DEFAULT '' NOT NULL,"+
					"LASTBOUGHT TIMESTAMP DEFAULT '1970-01-01 00:00:01' NOT NULL,"+
					"LASTSOLD TIMESTAMP DEFAULT '1970-01-01 00:00:01' NOT NULL,"+
					"PRIMARY KEY (PKGSUSERS),"+
					"UNIQUE (USERNAME))");

				stmt.executeUpdate("CREATE TABLE IF NOT EXISTS GSLOG"+
					"(PKGSLOG INTEGER AUTO_INCREMENT NOT NULL,"+
					"TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,"+
					"AMOUNT INTEGER DEFAULT 1 NOT NULL,"+
					"ITEM INTEGER,"+
					"USER INTEGER,"+
					"PRIMARY KEY (PKGSLOG),"+
					"FOREIGN KEY (USER)"+
					"REFERENCES GSUSERS(PKGSUSERS) ON UPDATE CASCADE ON DELETE SET NULL)");
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when creating gslog." + "\n" +ex);
			}
			catch(NullPointerException ex){
				log.severe("[GoldStandard] Could not connect to H2 database.");
			}
		}
	}
	public int countTransactions(String itemID){
		TIntArrayList ar = new TIntArrayList();
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
		SumCount countAll = new SumCount();
		ar.forEach(countAll);
		return countAll.getSum();
	}
	@Override
	public void addEntry(int amt, String usr, int item){
		int userId = playerData.get(usr).getId();
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gslog (amount,user,item,time) VALUES (?,?,?,?)");
				stmt.setInt(1, amt);
				stmt.setInt(2, userId);
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
		int userId = playerData.get(usr).getId();
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gslog (amount,user,item,time) VALUES (?,?,?,?)");
				stmt.setInt(1, amt);
				stmt.setInt(2, userId);
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
		Timestamp now = new Timestamp(System.currentTimeMillis());
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gsusers (username,lastBought,lastSold) VALUES (?,?,?)");
				stmt.setString(1, name);
				stmt.setTimestamp(2, now);
				stmt.setTimestamp(3, now);
				stmt.executeUpdate();

				stmt = conn.prepareStatement("SELECT * from gsusers WHERE username = ?");
				stmt.setString(1, name);
				rs = stmt.executeQuery();
				while (rs.next()){
					id = rs.getInt("pkgsusers");
				}
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when adding player "+ name + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
				SQLUtils.closeQuietly(rs);
			}
		}
		GSPlayer gsp = new GSPlayer(id, name);
		gsp.setLastBought(now.getTime());
		gsp.setLastSold(now.getTime());
		if (!playerData.containsKey(name))
			playerData.put(name, gsp);
		else
			log.severe("[GoldStandard] User "+name+" already exists!");
	}
	@Override
	public void storePlayer(String name) {
		if (!playerData.containsKey(name)){
			log.severe("[GoldStandard] Could not store user "+name+". Player was not loaded into memory!");
			return;
		}
		GSPlayer gsp = playerData.get(name);
		synchronized(CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("UPDATE gsusers SET buyItem=?, buyQty=?, sellItems=?, lastBought=?, lastSold=? WHERE pkgsusers=?");
				stmt.setInt(1, gsp.getBuyItem());
				stmt.setInt(2, gsp.getBuyQty());
				stmt.setString(3, gsp.getSellItems());
				stmt.setTimestamp(4, new Timestamp(gsp.getLastBought()));
				stmt.setTimestamp(5, new Timestamp(gsp.getLastSold()));
				stmt.setInt(6, gsp.getId());
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
			log.severe("[GoldStandard] Could not store user "+name+". Player was not loaded into memory!");
			return;
		}
		GSPlayer gsp = playerData.get(name);
		synchronized(CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("UPDATE gsusers SET buyItem=?, buyQty=?, sellItems=?, lastBought=?, lastSold=? WHERE pkgsusers=?");
				stmt.setInt(1, gsp.getBuyItem());
				stmt.setInt(2, gsp.getBuyQty());
				stmt.setString(3, gsp.getSellItems());
				stmt.setTimestamp(4, new Timestamp(gsp.getLastBought()));
				stmt.setTimestamp(5, new Timestamp(gsp.getLastSold()));
				stmt.setInt(6, gsp.getId());
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
