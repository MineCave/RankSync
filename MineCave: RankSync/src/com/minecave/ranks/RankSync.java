package com.minecave.ranks;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.google.common.collect.Lists;
import com.minecave.ranks.util.FileUtils;
import com.minecave.ranks.util.MySQL;
import com.minecave.ranks.util.SQLGet;
import com.minecave.ranks.util.SQLNewPlayer;
import com.minecave.ranks.util.SQLSet;
import com.minecave.ranks.util.Table;

public class RankSync extends JavaPlugin implements Listener {
	
	public static RankSync p;
	
	private FileUtils files;
	private Table table;
	
	private List<String> toSync;
	
	@Override
	public void onEnable() {
		p = this;
		files = new FileUtils(this);
		if (MySQL.getInstance() == null)
			new MySQL(this);
		
		table = Table.getTable("RANKS");
		
		makeRankList();
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent event) {
		String[] args = event.getMessage().split(" ");
		String command = args[0];
		if (!command.equalsIgnoreCase("/pex") && !command.equalsIgnoreCase("pex") || args.length < 6)
			return;
		
		String operation = args[4];
		String rank = args[5];
		
		if (operation.equalsIgnoreCase("remove"))
			removeRank(args[2], rank);
		else if (operation.equalsIgnoreCase("set"))
			setRank(args[2], rank);
	}
	
	@EventHandler
	public void onJoin(final PlayerJoinEvent event) {
		new SQLNewPlayer(event.getPlayer().getUniqueId().toString(), table) {
			@Override
			protected void done() {
				updateGroups(event.getPlayer());
			}
		};
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		onLeave(event.getPlayer());
	}
	
	@EventHandler
	public void onLeave(PlayerKickEvent event) {
		onLeave(event.getPlayer());
	}
	
	public void onLeave(Player player) {
		syncGroups(player);
	}
	
	private void updateGroups(final Player player) {
		new SQLGet(player, "groups", table) {
			@SuppressWarnings("deprecation")
			@Override
			protected void done() {
				String groupsString = (String) result;
				if (groupsString == null)
					return;
				
				String[] groups = groupsString.split(";");
				
				if (groups.length == 0)
					return;
				PermissionUser user = PermissionsEx.getUser(player);
				List<String> currentGroups = Lists.newArrayList();
				for (PermissionGroup group : user.getGroups())
					currentGroups.add(group.getName().toLowerCase());
				
				for (String group : groups) {
					if (!currentGroups.contains(group.toLowerCase()))
						user.addGroup(group);
				}
			}
		};
	}
	
	private void syncGroups(final Player player) {
		new SQLGet(player, "groups", table) {
			@Override
			protected void done() {
				String groups = result == null ? "" : (String) result;
				for (String group : getGroupsToGet(player)) {
					if (!groups.toLowerCase().contains(group.toLowerCase()))
						groups = groups.isEmpty() ? groups.concat(group) : groups.concat(";").concat(group);
				}
				if (groups.startsWith(";"))
					groups = groups.replaceFirst(";", "");
				
				new SQLSet(player, "groups", groups, table);
			}
		};
	}
	
	private List<String> getGroupsToGet(Player player) {
		return getGroupsToGet(player.getName());
	}
	
	@SuppressWarnings("deprecation")
	private List<String> getGroupsToGet(String player) {
		List<String> newGet = Lists.newArrayList();
		
		PermissionUser user = PermissionsEx.getUser(player);
		for (PermissionGroup group : user.getGroups())
			if (toSync.contains(group.getName().toLowerCase()))
				newGet.add(group.getName());
		
		return newGet;
	}
	
	@SuppressWarnings("deprecation")
	private void removeRank(final String player, final String rank) {
		final String uuid = Bukkit.getOfflinePlayer(player).getUniqueId().toString();
		
		new SQLGet(uuid, "groups", table) {
			@Override
			protected void done() {
				String groups = result == null ? "" : (String) result;
				for (String group : getGroupsToGet(player))
					if (!groups.toLowerCase().contains(group.toLowerCase()))
						groups += ";".concat(group);
				
				if (groups.toLowerCase().contains(rank.toLowerCase()))
					groups = groups.replaceAll("(?i);" + rank, "");
				
				new SQLSet(uuid, "groups", groups, table);
			}
		};
	}
	
	@SuppressWarnings("deprecation")
	private void setRank(final String player, final String rank) {
		String uuid = Bukkit.getOfflinePlayer(player).getUniqueId().toString();
		new SQLSet(uuid, "groups", rank, table);
	}
	
	private void makeRankList() {
		toSync = Lists.newArrayList();
		
		for (String s : files.getConfig().getStringList("ranks to sync")) toSync.add(s.toLowerCase());
	}
	
	@Override
	public FileConfiguration getConfig() {
		return files.getConfig();
	}

	@Override
	public void reloadConfig() {
		files.reloadConfig();
	}

	@Override
	public void saveConfig() {
		files.saveConfig();
	}
	
	public FileUtils getFiles() {
		return files;
	}
}