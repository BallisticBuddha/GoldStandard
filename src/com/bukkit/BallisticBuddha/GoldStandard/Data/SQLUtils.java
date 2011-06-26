package com.bukkit.BallisticBuddha.GoldStandard.Data;

import java.sql.*;
import java.util.logging.Logger;

public class SQLUtils 
{
	private static Logger log = Logger.getLogger("Minecraft");

  public static void closeQuietly(Connection connection)
  {
    try{
      if (connection != null)
        connection.close();
    }
    catch (SQLException e){
      log.severe("An error occurred closing connection." + "\n" +e);
    }
  }
  public static void closeQuietly(Statement statement)
  {
    try{
      if (statement!= null)
        statement.close();
    }
    catch (SQLException e){
      log.severe("An error occurred closing statement." + "\n" +e);
    }
  }
  public static void closeQuietly(ResultSet resultSet)
  {
    try{
      if (resultSet!= null)
        resultSet.close();
    }
    catch (SQLException e){
      log.severe("An error occurred closing result set." + "\n" +e);
    }
  }
}
