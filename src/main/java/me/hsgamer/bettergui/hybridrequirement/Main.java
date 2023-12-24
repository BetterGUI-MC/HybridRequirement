package me.hsgamer.bettergui.hybridrequirement;

import me.hsgamer.bettergui.builder.RequirementBuilder;
import me.hsgamer.hscore.expansion.common.Expansion;

public final class Main implements Expansion {
    @Override
    public void onEnable() {
        RequirementBuilder.INSTANCE.register(HybridRequirement::new, "hybrid");
    }
}
