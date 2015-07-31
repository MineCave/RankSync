package com.minecave.ranks.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import com.google.common.collect.Maps;
import com.minecave.ranks.RankSync;

public class MySQL {
	private static MySQL instance;
	private RankSync plugin;
	private Connection connection;

	public MySQL(RankSync plugin) {
		this.plugin = plugin;
        
		String host = plugin.getConfig().getString("host");
		String port = plugin.getConfig().getString("port");
		String database = plugin.getConfig().getString("database");
		String user = plugin.getConfig().getString("user");
		String pass = plugin.getConfig().getString("pass");
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			makeDatabase(database, DriverManager.getConnection("jdbc:mysql://" + host + ":" + port, user, pass));
		} catch (SQLException | ClassNotFoundException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		}
	    
		openConnection();
		
		instance = this;
	}
	
	public Object get(String uuid, String query, Table table) throws RuntimeException {
		Object stat = null;
		Connection connection = getConnection();
		ResultSet rs = null;
		Statement statement = null;
		boolean b;
		try {
			statement = connection.createStatement();
			b = statement.execute("SELECT * FROM " + table + " WHERE uuid = '" + uuid + "' LIMIT 1");
			if (b) {
				rs = statement.getResultSet();
				while (rs.next()) {
					String uuidStat = rs.getString("uuid");
					if (!uuidStat.equals(uuid))
						continue;
					stat = rs.getObject(query);
				}
			}
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
			}
		}
		return stat;
	}
	
	// Only works if there's a player_uuid column (like in weapons and market)
	public Map<Integer, Map<String, Object>> getAll(String playerUuid, String[] queries, Table table) throws RuntimeException {
		Map<Integer, Map<String, Object>> stat = Maps.newHashMap();
		Connection connection = getConnection();
		ResultSet rs = null;
		Statement statement = null;
		boolean b;
		try {
			statement = connection.createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " (" + table.getNamesWithTypes() + ")");
			if (playerUuid.equals("*"))
				b = statement.execute("SELECT * FROM " + table);
			else
				b = statement.execute("SELECT * FROM " + table + " WHERE player_uuid = '" + playerUuid + "'");
			if (b) {
				rs = statement.getResultSet();
				while (rs.next()) {
					Map<String, Object> inner = Maps.newHashMap();
					for (String query : queries) {
						inner.put(query, rs.getObject(query));
					}
					stat.put(rs.getRow(), inner);
				}
			}
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
			}
		}
		return stat;
	}
	
	public void set(String uuid, String stat, Object value, Table table) {
		set("uuid", uuid, stat, value, table);
	}
	
	public void set(String columnName, String columnValue, String stat, Object value, Table table) {
		Connection connection = getConnection();
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = connection.prepareStatement("UPDATE " + table + " SET " + stat + " = ? WHERE " + columnName + " = ?");
			preparedStatement.setObject(1, value);
			preparedStatement.setString(2, columnValue);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		} finally {
			try {
				if (preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
			}
		}
	}
	
	public void addColumn(String column, String type, String defaultValue, Table table) {
		Connection connection = getConnection();
		Statement statement = null;
		try {
			statement = connection.createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " (" + table.getNamesWithTypes() + ")");
			statement.execute(
					"ALTER TABLE " + table.toString() +
					" ADD COLUMN " + column + " " + type +
					" DEFAULT " + defaultValue);
		} catch (SQLException e) {
			if (e.getErrorCode() == 1060) // Duplicate column
				return;
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		} finally {
			try {
				if (statement != null)
					statement.close();
			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
			}
		}
	}
	
	public void makeNewPlayerReplace(String id, String uuid, Table table) {
		makeNewPlayer(table.getDelimitedValues(id, uuid), table);
	}
	
	public void makeNewPlayerReplace(String uuid, Table table) {
		makeNewPlayer(table.getDelimitedValues(uuid), table);
	}
	
	public void makeNewPlayer(String values, Table table) {
		Connection connection = getConnection();
		Statement statement = null;
		ResultSet rs = null;
		try {
			statement = connection.createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " (" + table.getNamesWithTypes() + ")");
			statement.execute("INSERT IGNORE INTO " + table + " (" + table.getStatNames() + ") VALUES (" + values + ")");
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (statement != null)
					statement.close();
			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
			}
		}
	}
	
	public void delPlayer(String uuid, Table table) {
		Connection connection = getConnection();
		Statement statement = null;
		try {
			statement = connection.createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " (" + table.getNamesWithTypes() + ")");
			statement.execute("DELETE FROM " + table + " WHERE uuid = '" + uuid + "'");
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		} finally {
			try {
				if (statement != null)
					statement.close();
			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
			}
		}
	}
	
	private void makeDatabase(String database, Connection connection) {
		Statement statement = null;
		if (true) {
			try {
				statement = connection.createStatement();
				statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + database);
			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
			} finally {
				try {
					if (statement != null)
						statement.close();
					if (connection != null)
						connection.close();
				} catch (SQLException e) {
					Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
				}
			}
		}
	}
	
	public void openConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String host = plugin.getConfig().getString("host");
			String port = plugin.getConfig().getString("port");
			String database = plugin.getConfig().getString("database");
			String user = plugin.getConfig().getString("user");
			String pass = plugin.getConfig().getString("pass");
			connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, user, pass);
		} catch (SQLException | ClassNotFoundException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		}
	}
	
	public void closeConnection() {
		try {
			if (connection == null || connection.isClosed())
				return;
			getConnection().close();
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		}
	}
	
	public Connection getConnection() {
		try {
			if (connection == null || connection.isClosed())
				openConnection();
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		}
		
		return connection;
	}
	
	public static MySQL getInstance() {
		return instance;
	}
}