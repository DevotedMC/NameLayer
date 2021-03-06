package vg.civcraft.mc.namelayer.mc.util;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.maxopoly.artemis.NameAPI;

import vg.civcraft.mc.namelayer.mc.NameLayerPlugin;

public final class MsgUtils {

	private MsgUtils() {
	}

	public static void sendNoPermissionMsg(UUID player, String perm, String groupName) {
		sendMsg(player,
				String.format("%sTo do this you need the permission %s%s %sfor the group %s%s which you do not have",
						ChatColor.RED, ChatColor.YELLOW, perm, ChatColor.RED, groupName, ChatColor.RED));
	}

	public static void sendGroupNotExistMsg(UUID player, String groupName) {
		sendMsg(player, String.format("%sThe group %s does not exist", ChatColor.RED, groupName));
	}

	public static void sendRankNotExistMsg(UUID player, String groupName, String rankName) {
		sendMsg(player,
				String.format(
						"%sThe rank %s does not exist for the group %s or you do not permission to blacklist on it",
						ChatColor.RED, rankName, groupName));
	}

	public static void sendPlayerNotExistMsg(UUID receiver, String playerNotExisting) {
		sendMsg(receiver, String.format("%sThe player %s%s has never played before", ChatColor.RED, playerNotExisting,
				ChatColor.RED));
	}

	public static void sendMsg(UUID player, String msg) {
		Bukkit.getScheduler().runTask(NameLayerPlugin.getInstance(), () -> {
			if (player.equals(NameAPI.CONSOLE_UUID)) {
				NameLayerPlugin.getInstance().getLogger().info(msg);
				return;
			}
			Player p = Bukkit.getPlayer(player);
			if (p != null) {
				p.sendMessage(msg);
			}
		});
	}

	public static void sendMsg(CommandSender receiver, String msg) {
		Bukkit.getScheduler().runTask(NameLayerPlugin.getInstance(), () -> {
			receiver.sendMessage(msg);
		});
	}

}
