package com.bukkit.BallisticBuddha.GoldStandard.Data;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * Class for handling all H2SQL backend tasks
 */
public class GSDataH2 extends GSData {

	public GSDataH2(){
		super();
		this.initSQL();
		this.createTable();
		transactions = this.countTransactions(Integer.toString(gs.getBaseItem().getTypeId()));
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
	private void createTable(){
		synchronized (CalcLock){
			Statement stmt = null;
			try{
				stmt = conn.createStatement();
				stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `gslog` (" +
						"pkgslog bigint unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY," +
						"time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL," +
						"amount int DEFAULT '1' NOT NULL," +
						"user varchar(45) DEFAULT NULL," +
						"item int unsigned DEFAULT NULL," +
						")");
			}
			catch(SQLException ex){
				log.severe("Error when creating gslog." + "\n" +ex);
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
				Timestamp limit = new Timestamp(System.currentTimeMillis()-(86400000*duration));
				stmt = conn.prepareStatement("delete from gslog WHERE time < ?");
				stmt.setTimestamp(1, limit);
				int amtDeleted = stmt.executeUpdate();
				log.info("[GoldStandard] Cleared out "+ amtDeleted +" records older than "+this.duration + " days.");
			}
			catch(SQLException ex){
				log.warning("Error when clearing GoldStandard table" + "\n" +ex);
			}
			finally{
				SQLUtils.closeQuietly(stmt);
			}
		}
		this.transactions = countTransactions("*");
	}
	@Override
	public boolean needsCleaning(){
		synchronized(CalcLock){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				Timestamp limit = new Timestamp(System.currentTimeMillis()-(86400000*duration));
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
}
