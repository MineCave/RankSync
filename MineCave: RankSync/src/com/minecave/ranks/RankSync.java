package com.minecave.ranks;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.minecave.ranks.util.FileUtils;
import com.minecave.ranks.util.MySQL;
import com.minecave.ranks.util.SQLGet;
import com.minecave.ranks.util.SQLNewPlayer;
import com.minecave.ranks.util.SQLSet;
import com.minecave.ranks.util.Table;

public class RankSync extends JavaPlugin implements Listener {
	
	public static RankSync p;
	
	private boolean debug;
	
	private FileUtils files;
	private Table table;
	
	private List<String> toSync;
	private Map<String, Map<String, Integer>> ladders;
	
	@Override
	public void onEnable() {
		p = this;
		debug = false;
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
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("ranksyncdebug")) {
			if (!sender.hasPermission("ranksyncdebug.toggle"))
				return true;
			
			debug = !debug;
			sender.sendMessage("Debug mode is now set to " + debug + ".");
		}
		return true;
	}
	
	@EventHandler
	public void onJoin(final PlayerLoginEvent event) {
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
				
				if (groups.length == 0) {
					debug("Player " + player.getName() + " had no ranks in MySQL. Ignoring player completely...");
					return;
				}
				
				PermissionUser user = PermissionsEx.getUser(player);
				List<String> currentGroups = Lists.newArrayList();
				for (PermissionGroup group : user.getGroups())
					currentGroups.add(group.getName().toLowerCase());
				
				for (String group : groups) {
					if (!currentGroups.contains(group.toLowerCase())) {
						debug("[updateGroups (login)] Player " + player.getName() +
								" did not have group " + group +
								" in-game, but had it in MySQL. Adding in-game...");
						user.addGroup(group);
						currentGroups.add(group.toLowerCase());
					} else {
						debug("[updateGroups (login)] Player " + player.getName() +
								" already had group " + group +
								" in-game and in MySQL. Ignoring...");
					}
				}
				removeGroupsInLadder(player, user, currentGroups);
			}
		};
	}
	
	private void removeGroupsInLadder(Player player, PermissionUser user, List<String> groups) {
		for (Map.Entry<String, Map<String, Integer>> ladderMapEntry : ladders.entrySet()) {
			String ladderName = ladderMapEntry.getKey();
			Map<String, Integer> ladderMap = ladderMapEntry.getValue();
			List<String> possiblyRemove = groups;
			
			int highestPriority = 0;
			List<String> removeFromPossiblyRemovePre = Lists.newArrayList();
			for (String possiblyRemoveString : possiblyRemove) {
				if (!ladderMap.containsKey(possiblyRemoveString))
					removeFromPossiblyRemovePre.add(possiblyRemoveString);
			}
			for (String toRemove : removeFromPossiblyRemovePre) {
				possiblyRemove.remove(toRemove);
			}
			
			for (Map.Entry<String, Integer> ladder : ladderMap.entrySet()) {
				if (groups.contains(ladder.getKey().toLowerCase())) {
					highestPriority = ladder.getValue();
					debug("Set highest priority to " + highestPriority + player.getName()
							+ " because of " + ladder.getKey().toLowerCase() + ". Removing lower priority ranks...");
					
					List<String> removeFromPossiblyRemove = Lists.newArrayList();
					for (String possiblyRemoveString : possiblyRemove) {
						if (ladderMap.get(possiblyRemoveString) < highestPriority) {
							user.removeGroup(possiblyRemoveString);
							removeFromPossiblyRemove.add(possiblyRemoveString);
							debug("Removed " + possiblyRemoveString + " from player " + player.getName()
									+ " because he/she has a higher rank in the same ladder (" + ladderName + ")");
						}
					}
					for (String toRemove : removeFromPossiblyRemove) {
						possiblyRemove.remove(toRemove);
					}
				}
			}
		}
	}
	
	private void syncGroups(final Player player) {
		new SQLGet(player, "groups", table) {
			@Override
			protected void done() {
				String groups = result == null ? "" : (String) result;
				for (String group : getGroupsToGet(player)) {
					if (!groups.contains(group)) {
						debug("[syncGroups] Player " + player.getName() +
								" did not have the group " + group +
								" in MySQL, but had it in-game. Adding to MySQL...");
						groups = groups.isEmpty() ? groups.concat(group) : groups.concat(";").concat(group);
					} else {
						debug("[syncGroups] Player " + player.getName() +
								" already had the group " + group +
								" in-game and in MySQL. Ignoring...");
					}
				}
				if (groups.startsWith(";"))
					groups = groups.replaceFirst(";", "");
				
				debug("[syncGroups] Final task: setting groups in MySQL to '" + groups + "'.");
				
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
		for (String group : user.getGroupsNames()) {
			if (toSync.contains(group.toLowerCase())) {
				newGet.add(group.toLowerCase());
				debug("[getGroupsToGet] Player " + player +
						" has group " + group + " and it's on the to-sync list.");
			} else {
				debug("[getGroupsToGet] Player " + player +
						" has group " + group + ", but it's not on the to-sync list.");
			}
		}
		
		return newGet;
	}
	
	@SuppressWarnings("deprecation")
	private void removeRank(final String player, final String rank) {
		final String uuid = Bukkit.getOfflinePlayer(player).getUniqueId().toString();
		final PermissionUser user = PermissionsEx.getUser(player);
		
		new SQLGet(uuid, "groups", table) {
			@Override
			protected void done() {
				String groups = result == null ? "" : (String) result;
				
				debug("[removeRank] Groups for player " + player + " before attempting to remove: " + groups);
				
				if (groups.toLowerCase().contains(rank.toLowerCase()))
					groups = groups.replaceAll("(?i);" + rank, "");
				
				
				debug("[removeRank] Groups for player " + player + " after attempting to remove: " + groups);
				
				new SQLSet(uuid, "groups", groups, table);
			}
		};
		
		List<String> groups = Lists.newArrayList(user.getGroupsNames());
		if (groups.remove(rank))
			groups.remove(rank);
		user.setGroups(groups.toArray(new String[0]));
	}
	
	@SuppressWarnings("deprecation")
	private void setRank(final String player, final String rank) {
		String uuid = Bukkit.getOfflinePlayer(player).getUniqueId().toString();
		
		debug("[setRank] Setting the groups of player " + player + " to " + rank + ".");
		
		new SQLSet(uuid, "groups", rank, table);
	}
	
	private void makeRankList() {
		toSync = Lists.newArrayList();
		
		for (String rankName : files.getConfig().getStringList("ranks to sync")) {
			toSync.add(rankName.toLowerCase());
			debug("[makeRankList] Adding " + rankName.toLowerCase() + " to the list of ranks to sync");
		}
		
		ladders = Maps.newHashMap();
		
		for (String ladderName : files.getConfig().getConfigurationSection("ladders").getKeys(false)) {
			ConfigurationSection ladderSection = files.getConfig().getConfigurationSection("ladders." + ladderName);
			Map<String, Integer> ladderMap = Maps.newHashMap();
			for (String rankName : ladderSection.getKeys(false)) {
				int priority = ladderSection.getInt(rankName);
				ladderMap.put(rankName, priority);
				debug("[makeRankList] Adding " + ladderName.toLowerCase() + " to ladder " + ladderName + " with priority " + priority);	
			}
			ladders.put(ladderName, ladderMap);
		}
	}
	
	private void debug(String debugMessage) {
		if (debug)
			getLogger().log(Level.INFO, "RankSync Debug: " + debugMessage);
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