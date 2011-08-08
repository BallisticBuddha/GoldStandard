package com.bukkit.BallisticBuddha.GoldStandard.Data;

import gnu.trove.list.array.TIntArrayList;

import java.sql.*;

import com.bukkit.BallisticBuddha.GoldStandard.GSPlayer;

/**
 * Class for handling all MYSQL backend tasks
 */
public class GSDataMySQL extends GSData{
	
	public GSDataMySQL(){
		super();
		this.initSQL();
		this.createTables();
		this.countAllItems();
	}
	//thread locked to make all operations happen in a single connection
	private void initSQL(){
		synchronized(CalcLock){
			try{
				Class.forName("com.mysql.jdbc.Driver");
				String sqlurl = "jdbc:mysql://"+config.getString("MySQL.hostname","localhost")+":"+
					config.getString("MySQL.port","3306")+"/"+config.getString("MySQL.database","minecraft");
				conn = DriverManager.getConnection(sqlurl, config.getString("MySQL.username"), config.getString("MySQL.password"));
			}
		    catch (SQLException ex){
		      log.severe("[GoldStandard] An error occurred initiating the connection" + "\n" +ex);
		    } 
		    catch (ClassNotFoundException ex) {
		    	log.severe("[GoldStandard] *sigh* Another MySQL connectivity error" + "\n" +ex);
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
  						"`sellItems` VARCHAR(160) NOT NULL DEFAULT '' ,"+
  						"`lastBought` TIMESTAMP NOT NULL DEFAULT '2010-01-01 00:00:01',"+
  						"`lastSold` TIMESTAMP NOT NULL DEFAULT '2010-01-01 00:00:01',"+
  						"PRIMARY KEY (`pkgsusers`),"+
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
				log.info("[GoldStandard] Created database tables.");
				}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when creating database tables." + "\n" +ex);
			}
			catch(NullPointerException ex){
				log.severe("[GoldStandard] Could not connect to MySQL server.");
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
				log.severe("[GoldStandard] Error retrieving transactions from gslog" + "\n" +ex);
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
				stmt = conn.prepareStatement("INSERT INTO gslog (amount,user,item) VALUES (?,?,?)");
				stmt.setInt(1, amt);
				stmt.setInt(2, userId);
				stmt.setInt(3, item);
				stmt.execute();
				this.transValues.adjustValue(item, amt);
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when adding GoldStandard entry" + "\n" +ex);
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
				stmt = conn.prepareStatement("INSERT INTO gslog (amount,user,item) VALUES (?,?,?)");
				stmt.setInt(1, amt);
				stmt.setInt(2, userId);
				stmt.setInt(3, item);
				stmt.execute();
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when adding GoldStandard entry" + "\n" +ex);
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
				int amtDeleted = stmt.executeUpdate("delete from gslog");
				this.setAllToZero();
				log.info("[GoldStandard] Cleared history of "+ amtDeleted +" records by admin command.");
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when clearing GoldStandard table" + "\n" +ex);
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
				stmt = conn.prepareStatement("delete from gslog WHERE time < (NOW() - INTERVAL ? DAY)");
				stmt.setInt(1, getDuration());
				int amtDeleted = stmt.executeUpdate();
				log.info("[GoldStandard] Cleared out "+ amtDeleted +" records older than "+ getDuration() + " days.");
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when clearing GoldStandard table" + "\n" +ex);
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
				stmt = conn.prepareStatement("SELECT pkgslog FROM gslog WHERE time < (NOW() - INTERVAL ? DAY)");
				stmt.setInt(1, getDuration());
				rs = stmt.executeQuery();
				if(rs.next())
					return true;
			}
			catch(SQLException ex){
				log.severe("[GoldStandard] Error when querying GoldStandard table" + "\n" +ex);
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
		TIntArrayList ar = new TIntArrayList();
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
				log.severe("[GoldStandard] Error retrieving transactions from gslog" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(rs);
				SQLUtils.closeQuietly(stmt);
			}
		}
		while (ar.iterator().hasNext()){
			toRet += ar.iterator().next();
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
					GSPlayer gsp = new GSPlayer(rs.getInt("pkgsusers"),pname,gs);
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
			playerData.remove(name);
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
						GSPlayer gsp = new GSPlayer(rs.getInt("pkgsusers"),pname,gs);
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
		GSPlayer gsp = new GSPlayer(id, name,gs);
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
