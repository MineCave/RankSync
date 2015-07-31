package com.minecave.ranks.util;

import org.bukkit.entity.Player;

public class SQLFinalSet extends SQLAction {

	final String columnName;
	final String uuid;
	final String stat;
	final Object value;
	final Table table;
	
	public SQLFinalSet(Player player, String stat, Object value, Table table) {
		this(player.getUniqueId().toString(), stat, value, table);
	}
	
	public SQLFinalSet(String uuid, String stat, Object value, Table table) {
		this("uuid", uuid, stat, value, table);
	}
	
	public SQLFinalSet(String columnName, String uuid, String stat, Object value, Table table) {
		super();
		this.columnName = columnName;
		this.uuid = uuid;
		this.stat = stat;
		this.value = value;
		this.table = table;
	}
	
	@Override
	public final void run() {
		MySQL.getInstance().set(columnName, uuid, stat, value, table);
	}

}