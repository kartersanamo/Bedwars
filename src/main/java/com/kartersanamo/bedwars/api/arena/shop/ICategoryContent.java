package com.kartersanamo.bedwars.api.arena.shop;

import java.util.List;

/**
 * Groups one or more tiers for a logical shop entry.
 */
public interface ICategoryContent {

    String getId();

    List<IContentTier> getTiers();
}
