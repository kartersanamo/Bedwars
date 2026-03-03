package com.kartersanamo.bedwars.shop.main;

import com.kartersanamo.bedwars.api.arena.shop.ICategoryContent;
import com.kartersanamo.bedwars.api.arena.shop.IContentTier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CategoryContent implements ICategoryContent {

    private final String id;
    private final List<IContentTier> tiers = new ArrayList<>();

    public CategoryContent(final String id, final List<IContentTier> tiers) {
        this.id = id;
        if (tiers != null) {
            this.tiers.addAll(tiers);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<IContentTier> getTiers() {
        return Collections.unmodifiableList(tiers);
    }
}
