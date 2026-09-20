package com.createtiers.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Optional source-block identity carried by Create kinetics that temporarily
 * replace another kinetic block, such as belt pulleys replacing shafts.
 */
public interface IReplacementSourceBlockEntity {

    ResourceLocation getCreateTiersReplacementSourceBlockId();

    void setCreateTiersReplacementSourceBlockId(ResourceLocation id);

    void clearCreateTiersReplacementSourceBlockId();
}
