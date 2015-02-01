package com.minecave.ranks.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import com.minecave.ranks.RankSync;

public class MySQL {
	private static MySQL instance;
	private Connection connection;
	private RankSync plugin;

	public MySQL(RankSync plugin) {
		if (instance != null)
			return;
		this.plugin = plugin;
		instance = this;
	}

	/**
	 * This method is for getting a value, such as money, kills, etc., that is linked with a specific player's uuid.
	 * @param uuid the uuid of the player
	 * @param query the name of the column
	 * @param table the {@link com.minecave.ranks.util.Table Table} in which the information is stored
	 * @return the value associated with query
	 */
	public Object get(String uuid, String query, Table table) {
		ResultSet rs = null;
		Statement statement = null;
		boolean b;
		Object stat = null;
		int tryTime = 0;
		executeQuery: if (true) {
			try {
				if (connection == null || connection.isClosed())
					openConnection();
				statement = connection.createStatement();
				b = statement.execute("SELECT * FROM " + table + " WHERE uuid = '" + uuid + "' LIMIT 1");
				if (b) {
					rs = statement.getResultSet();
					while (rs.next()) {
						String uuidStat = rs.getString(1);
						if (!uuidStat.equals(uuid))
							continue;
						stat = rs.getObject(query);
					}
				}
			} catch (SQLException e) {
				if (tryTime == 1) {
					openConnection();
					break executeQuery;
				}
				Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
			} finally {
				try {
					if (rs != null)
						rs.close();
				} catch (SQLException e) {
					Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
				}
			}
		}
		return stat;
	}

	/**
	 * This method is for setting a value, such as money, kills, etc., that is linked with a specific player's uuid.
	 * @param uuid the uuid of the player
	 * @param stat the statistic that is being set (for example "coins" or "money" or "kills")
	 * @param value the value of the stat being set
	 * @param table the {@link com.minecave.ranks.util.Table Table} in which the information is stored
	 */
	public void set(String uuid, String stat, Object value, Table table) {
		PreparedStatement preparedStatement = null;
		int tryTime = 0;
		executeQuery: if (true) {
			tryTime++;
			try {
				if (connection == null || connection.isClosed())
					openConnection();
				preparedStatement = connection.prepareStatement("UPDATE " + table + " SET " + stat + " = ? WHERE uuid = ?");
				preparedStatement.setObject(1, value);
				preparedStatement.setString(2, uuid);
				preparedStatement.execute();
			} catch (SQLException e) {
				if (tryTime == 1) {
					openConnection();
					break executeQuery;
				}
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
	}

	public void makeNewPlayer(String uuid, Table table) {
		makeNewPlayer(uuid, table.getDelimitedValues().replaceAll("<uuid>", "'" + uuid + "'"), table);
	}
	
	public void makeNewPlayer(String uuid, String values, Table table) {
		Statement statement = null;
		ResultSet rs = null;
		int tryTime = 0;
		executeQuery: if (true) {
			try {
				if (connection == null || connection.isClosed())
					openConnection();
				statement = connection.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " (" + table.getNamesWithTypes() + ")");
				rs = statement.executeQuery("SELECT * FROM " + table + " WHERE uuid = '" + uuid + "' LIMIT 1");
				if (!rs.next())
					statement.execute("INSERT INTO " + table + " (" + table.getStatNames() + ") VALUES (" + values + ")");
			} catch (SQLException e) {
				if (tryTime == 1) {
					openConnection();
					break executeQuery;
				}
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
	}
	
	private void makeDatabase(String database) {
		Statement statement = null;
		if (true) {
			try {
				if (connection == null || connection.isClosed())
					openConnection();
				statement = connection.createStatement();
				statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + database);
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
	}

	private void openConnection() {
		try {  if (connection != null) connection.close(); } catch (SQLException e) {}
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String host = plugin.getConfig().getString("host");
			String port = plugin.getConfig().getString("port");
			String database = plugin.getConfig().getString("database");
			String user = plugin.getConfig().getString("user");
			String pass = plugin.getConfig().getString("pass");
			connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port, user, pass);
			makeDatabase(database);
			connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, user, pass);
		} catch (ClassNotFoundException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		}
	}

	public void closeConnection() {
		if (connection == null)
			return;
		try {
			connection.close();
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Error with MySQL: " + e.getMessage());
		}
		connection = null;
	}
	
	public static MySQL getInstance() {
		return instance;
	}
}