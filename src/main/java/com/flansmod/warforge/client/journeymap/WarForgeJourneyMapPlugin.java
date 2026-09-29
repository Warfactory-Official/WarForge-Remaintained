package com.flansmod.warforge.client.journeymap;

import com.flansmod.warforge.Tags;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * WarForge's single JourneyMap plugin, which then attaches every map layer the mod draws.
 *
 * <p>There can only be one per mod, and that is not a style preference. {@code PluginHelper} keeps its
 * client plugins in a map keyed by {@code getModId()}, so a second class annotated with
 * {@link JourneyMapPlugin} and returning the same id displaces the first. It is discovered, logged as
 * found, and then never initialised: the claim and vein layers used to be two plugins, and only the
 * claim layer was ever alive. Nothing said so.</p>
 */
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
