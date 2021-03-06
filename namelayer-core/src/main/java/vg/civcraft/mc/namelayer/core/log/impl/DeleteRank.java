package vg.civcraft.mc.namelayer.core.log.impl;

import java.util.UUID;

import vg.civcraft.mc.namelayer.core.log.abstr.LoggedGroupActionPersistence;
import vg.civcraft.mc.namelayer.core.log.abstr.MemberRankChange;

public class DeleteRank extends MemberRankChange {
	
	public static final String ID = "DELETE_RANK";

	public DeleteRank(long time, UUID player, String rank) {
		super(time, player, rank);
	}

	@Override
	public String getIdentifier() {
		return ID;
	}
	
	public static DeleteRank load(LoggedGroupActionPersistence persist) {
		return new DeleteRank(persist.getTimeStamp(), persist.getPlayer(), persist.getRank());
	}
}
