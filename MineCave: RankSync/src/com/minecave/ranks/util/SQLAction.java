package com.minecave.ranks.util;

import org.bukkit.scheduler.BukkitRunnable;

import com.minecave.ranks.RankSync;

public abstract class SQLAction implements Runnable {
	
	boolean done = false;
	
	public SQLAction() {
		final SQLAction action = this;
		new BukkitRunnable() {
			public void run() {
				action.run();
			}
		}.runTaskAsynchronously(RankSync.p);
	}
	
	public final boolean isDone() {
		return done;
	}
	
	final void _done() {
		done = true;
		new BukkitRunnable() {
			public void run() {
				done();
			}
		}.runTask(RankSync.p);
	}
	
	protected void done() {
		
	}

}