package com.bukkit.BallisticBuddha.GoldStandard;

import java.util.ArrayList;

import java.sql.*;
/**
 * Class for handling all MYSQL backend tasks
 */
public class GSDataMySQL extends GSData{
	
	public GSDataMySQL(){
		super();
		this.initSQL();
		this.createTable();
		transactions = this.countTransactions();
	}
	//thread locked to make all operations happen in a single connection
	private void initSQL(){
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
						"`pkgslog` int(32) unsigned NOT NULL AUTO_INCREMENT," +
						"`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP," +
						"`amount` int(10) NOT NULL DEFAULT '1'," +
						"`user` varchar(45) DEFAULT NULL," +
						"`item` int(10) unsigned DEFAULT NULL," +
						"PRIMARY KEY (`pkgslog`)" +
						") ENGINE=InnoDB DEFAULT CHARSET=latin1");
			}
			catch(SQLException ex){
				log.severe("Error when creating gslog." + "\n" +ex);
			}
		}
	}
	private int countTransactions(){
		ArrayList<Integer> ar = new ArrayList<Integer>();
		int toRet = 0;
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				stmt = conn.prepareStatement("SELECT amount from gslog");//TODO: add where clause to confirm the item "where (item = ?)"
				//stmt.setInt(1, gs.getItem());
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
	public void addEntry(int amt, String usr){
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gslog (amount,user,item) VALUES (?,?,?)");
				stmt.setInt(1, amt);
				stmt.setString(2, usr.toLowerCase());
				stmt.setInt(3, gs.getItem());
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
	@Override
	public void addEntryNI(int amt, String usr){
		synchronized (CalcLock){
			PreparedStatement stmt = null;
			try{
				stmt = conn.prepareStatement("INSERT INTO gslog (amount,user,item) VALUES (?,?,?)");
				stmt.setInt(1, amt);
				stmt.setString(2, usr.toLowerCase());
				stmt.setInt(3, gs.getItem());
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
	@Override
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
		this.transactions = countTransactions();
	}
	@Override
	public boolean needsCleaning(){
		synchronized(CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				stmt = conn.prepareStatement("SELECT pkgslog FROM gslog WHERE time < (NOW() - INTERVAL ? DAY)");
				stmt.setInt(1, this.duration);
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
}
