package vg.civcraft.mc.namelayer.mc.util;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civmodcore.playersettings.impl.DisplayLocationSetting;
import vg.civcraft.mc.civmodcore.scoreboard.bottom.BottomLine;
import vg.civcraft.mc.civmodcore.scoreboard.bottom.BottomLineAPI;
import vg.civcraft.mc.civmodcore.scoreboard.side.CivScoreBoard;
import vg.civcraft.mc.civmodcore.scoreboard.side.ScoreBoardAPI;
import vg.civcraft.mc.namelayer.mc.NameLayerPlugin;

public class NLScoreBoard {
	private final BottomLine chatBottomLine;
	private final CivScoreBoard chatBoard;
	private final NameLayerSettingManager settingMan;

	public NLScoreBoard() {
		this.chatBoard = ScoreBoardAPI.createBoard("CivChatDisplay");
		this.chatBottomLine = BottomLineAPI.createBottomLine("CivChatDisplay", 3);
		this.settingMan = NameLayerPlugin.getInstance().getSettingsManager();
	}

	/** Updates the scoreboard to display the players currently in use private message channel or chat group
	 *
	 * @param p player to update scoreboard for
	 */
	public void updateScoreboardHUD(Player p) {
		if (!settingMan.getShowChatGroup(p.getUniqueId())) {
			chatBoard.hide(p);
			chatBottomLine.removePlayer(p);
		} else {
			DisplayLocationSetting locSetting = settingMan.getChatGroupLocation();
			CivChat2Manager chatman = CivChat2.getInstance().getManager();
			String text;
			if (chatman.getPrivateChannel(p) != null ) {
				text = ChatColor.GOLD + "Messaging " + ChatColor.LIGHT_PURPLE + Bukkit.getPlayer(chatman.getPrivateChannel(p)).getName();
			} else if (chatman.getGroupChannel(p) != null) {
				text = ChatColor.GOLD + "Chat Group " + ChatColor.LIGHT_PURPLE + chatman.getGroupChannel(p).getName();
			} else {
				chatBoard.hide(p);
				chatBottomLine.removePlayer(p);
				return;
			}
			if (locSetting.showOnActionbar(p.getUniqueId())) {
				chatBottomLine.updatePlayer(p, text);
			}
			if (locSetting.showOnSidebar(p.getUniqueId())) {
				chatBoard.set(p, text);
			}
		}
	}

}
