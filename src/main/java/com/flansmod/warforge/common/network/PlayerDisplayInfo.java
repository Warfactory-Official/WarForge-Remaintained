package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;


import java.util.UUID;

public class PlayerDisplayInfo
{
    public String username = "";
    public UUID playerUuid = FactionIds.NONE;
    public Role role = Role.MEMBER;
}
