package vg.civcraft.mc.namelayer.zeus.rabbit.groupchanges;

import org.json.JSONObject;

import vg.civcraft.mc.namelayer.core.requests.GroupModifications;

public class AddLinkMessage extends GroupChangeMessage {

	private int targetGroupID;
	private int sourceRankID;
	private int targetRankID;

	public AddLinkMessage(int groupID, int sourceRankID, int targetGroupID, int targetRankID) {
		super(groupID);
		this.targetGroupID = targetGroupID;
		this.targetRankID = targetRankID;
		this.sourceRankID = sourceRankID;
	}

	@Override
	protected void fillJson(JSONObject json) {
		json.put("target_group_id", targetGroupID);
		json.put("source_rank_id", sourceRankID);
		json.put("target_rank_id", targetRankID);
	}

	@Override
	public String getIdentifier() {
		return GroupModifications.ADD_LINK_ID;
	}
}
