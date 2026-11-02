package com.flansmod.warforge.client.journeymap;

import com.flansmod.warforge.Tags;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;

import javax.annotation.ParametersAreNonnullByDefault;

/** The mod's single JourneyMap plugin: PluginHelper keys plugins by mod id, so a second one is never initialised. */
@ParametersAreNonnullByDefault
@JourneyMapPlugin(apiVersion = IClientAPI.API_VERSION)
public class WarForgeJourneyMapPlugin implements IClientPlugin {

    private final ClaimMapLayer claims = new ClaimMapLayer();
    private final VeinMapLayer veins = new VeinMapLayer();

    @Override
    public String getModId() {
        return Tags.MODID;
    }

    @Override
    public void initialize(final IClientAPI jmClientApi) {
        this.claims.attach(jmClientApi);
        this.veins.attach(jmClientApi);
    }
}
