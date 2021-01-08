package vg.civcraft.mc.namelayer.zeus.rabbit.groupchanges;

import java.util.UUID;

import org.json.JSONObject;

import vg.civcraft.mc.namelayer.core.requests.GroupModifications;

public class RemoveInviteMessage extends GroupMemberModifyMessage {

	public RemoveInviteMessage(int groupID, UUID player) {
		super(groupID, player);
	}

	@Override
	public String getIdentifier() {
		return GroupModifications.REMOVE_INVITE_ID;
	}

	@Override
	protected void fillJson(JSONObject json) {
		//nothing needed
	}

}
