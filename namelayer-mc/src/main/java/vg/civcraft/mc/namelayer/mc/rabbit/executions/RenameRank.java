package vg.civcraft.mc.namelayer.mc.rabbit.executions;

import org.json.JSONObject;

import vg.civcraft.mc.namelayer.core.Group;
import vg.civcraft.mc.namelayer.core.GroupRank;
import vg.civcraft.mc.namelayer.core.IllegalGroupStateException;
import vg.civcraft.mc.namelayer.core.requests.GroupModifications;

public class RenameRank extends AbstractGroupModificationHandler {

	@Override
	protected void handle(Group group, JSONObject data) {
		int rankID = data.getInt("rank_id");
		GroupRank rank = group.getGroupRankHandler().getRank(rankID);
		if (rank == null) {
			throw new IllegalGroupStateException();
		}
		String newName = data.getString("new_name");
		getGroupTracker().renameRank(group, rank, newName);
	}

	@Override
	public String getIdentifier() {
		return GroupModifications.RENAME_RANK_ID;
	}

}
