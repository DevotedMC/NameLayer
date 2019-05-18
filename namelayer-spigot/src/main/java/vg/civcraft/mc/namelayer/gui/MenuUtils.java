package vg.civcraft.mc.namelayer.gui;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import vg.civcraft.mc.civmodcore.itemHandling.ISUtils;
import vg.civcraft.mc.namelayer.NameLayerPlugin;

public class MenuUtils {
	public static ItemStack toggleButton(boolean initState, String name, boolean canModify) {
		ItemStack is = new ItemStack(Material.INK_SACK);
		if (initState) {
			is.setDurability((short) 10); //dye green
			if (canModify) {
			ISUtils.addLore(is, ChatColor.GOLD + "Currently turned on", ChatColor.AQUA + "Click to turn off");
			}
		}
		else {
			is.setDurability((short) 1); //dye red
			if  (canModify) {
			ISUtils.addLore(is, ChatColor.GOLD + "Currently turned off", ChatColor.AQUA + "Click to turn on");
			}
		}
		if (!canModify) {
			ISUtils.addLore(is, ChatColor.RED + "You don't have permission to", ChatColor.RED + "modify this setting");
		}
		ISUtils.setName(is, name);
		return is;
	}
	
	public static ItemStack getPlayerSkull(UUID uuid) {
		return null; // TODO?
	}
	
	/**
	 * Doesn't work
	 */
	public static List <String> splitLore(String lore) {
		System.out.println("Splitting " + lore);
		LinkedList <String> splitLore = new LinkedList<String>();
		int maxLineLength = 50;
		StringBuilder sb = new StringBuilder();
		String [] split = lore.split(" ");
		for(int i = 0; i < split.length; i++) {
			String word = split [i];
			if ((sb.length() + word.length()) > maxLineLength) {
				//max line length reached
				if (sb.length() == 0) {
					//if empty, the word alone fills the line length so put it in anyway
					sb.append(word);
				}
				else {
					//include word in next run
					i--;
				}
				//add finished line
				splitLore.add(sb.toString());
				sb = new StringBuilder();
			}
			else {
				//just append, line not full yet
				sb.append(" ");
				sb.append(word);
			}
		}
		return splitLore;
	}

	public static boolean guiRateLimit(Player p, String cmd, boolean isTab) {	
		return NameLayerPlugin.rateLimit(((OfflinePlayer) p).getUniqueId(), cmd, isTab);
	}

	public static void guiLimitPlayer(Player p, String cmd, boolean isTab) {
		String limitMsg = NameLayerPlugin.rateLimitMessage(cmd, isTab);
		if (limitMsg != null && !"".equals(limitMsg.trim())) {
			p.sendMessage(new StringBuilder().append(ChatColor.RED).append(limitMsg).toString());
		}
	}
}
