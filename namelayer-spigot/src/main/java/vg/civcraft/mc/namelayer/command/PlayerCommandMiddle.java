package vg.civcraft.mc.namelayer.command;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.group.Group;

public abstract class PlayerCommandMiddle extends PlayerCommand{

	public PlayerCommandMiddle(String name) {
		super(name);
	}

	protected GroupManager gm = NameAPI.getGroupManager();
	
	protected boolean groupIsNull(CommandSender sender, String groupname, Group group) {
	    if (group == null) {
	        sender.sendMessage(String.format(
	                "%sThe group \"%s\" does not exist.", 
	                ChatColor.RED, groupname));
	        return true;
	    }
	    return false;
	}

	private static Map<String, Map<UUID, Long>> rateLimiter = new ConcurrentHashMap<String, Map<UUID, Long>>();
	private static UUID GLOB = new UUID(0L, 0L);
	protected static boolean rateLimit(UUID player, String cmd, boolean global, long minDelay) {
		boolean ret = false;
		if (global) {
			ret = rateLimit(GLOB, cmd, false, minDelay);
			if (ret) {
				return ret;
			}
		}
		long rn = System.currentTimeMillis();
		Map<UUID, Long> rL = rateLimiter.get(cmd);
		if (rL == null) {
			rL = new ConcurrentHashMap<UUID, Long>();
			rateLimiter.put(cmd, rL);
		}

		Long ln = rL.get(player);
		if (ln != null && ln >= (rn - minDelay)) {
			ret = true;
		}
		rL.put(player, rn);
			
		return ret;
	}
}
