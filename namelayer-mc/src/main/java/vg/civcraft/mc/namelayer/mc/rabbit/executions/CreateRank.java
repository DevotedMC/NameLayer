package vg.civcraft.mc.namelayer.mc.rabbit.executions;

import org.json.JSONObject;

import vg.civcraft.mc.namelayer.core.Group;
import vg.civcraft.mc.namelayer.core.GroupRank;
import vg.civcraft.mc.namelayer.core.requests.GroupModifications;

public class CreateRank extends AbstractGroupModificationHandler {

	@Override
	protected void handle(Group group, JSONObject data) {
		int rankId = data.getInt("rank_id");
		String rankName = data.getString("rank_name");
		int parentId = data.getInt("parent_id");
		GroupRank parent = group.getGroupRankHandler().getRank(parentId);
		group.getGroupRankHandler().createNewRank(new GroupRank(rankName, rankId, parent));
	}

	@Override
	public String getIdentifier() {
		return GroupModifications.CREATE_RANK_ID;
	}

}
