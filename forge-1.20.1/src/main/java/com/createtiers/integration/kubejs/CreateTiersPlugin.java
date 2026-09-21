package com.createtiers.integration.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;

/**
 * KubeJS integration for Create Tiers.
 * Tier registration must run from startup_scripts so definitions exist before block registration freezes.
 */
public class CreateTiersPlugin extends KubeJSPlugin {

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("CreateTiers", CreateTiersBinding.class);
    }
}
