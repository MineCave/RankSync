package com.minecave.ranks.util;

public class SQLNewPlayer extends SQLAction {

	
	final String uuid;
	final Table table;
	final boolean special;
	final String playerUUID;
	
	public SQLNewPlayer(String uuid, Table table) {
		super();
		this.uuid = uuid;
		this.table = table;
		special = false;
		playerUUID = "";
	}
	
	public SQLNewPlayer(String uuid, String playerUUID, Table table) {
		super();
		this.uuid = uuid;
		this.table = table;
		special = true;
		this.playerUUID = playerUUID;
	}
	
	@Override
	public final void run() {
		if (special)
			MySQL.getInstance().makeNewPlayerReplace(uuid, playerUUID, table);
		else
			MySQL.getInstance().makeNewPlayerReplace(uuid, table);
		_done();
	}
}