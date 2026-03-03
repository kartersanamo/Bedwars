package com.kartersanamo.bedwars.shop.main;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShopCategory {

    private final String id;
    private final ItemStack icon;
    private final List<CategoryContent> contents = new ArrayList<>();

    public ShopCategory(final String id, final ItemStack icon) {
        this.id = id;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public ItemStack getIcon() {
        return icon.clone();
    }

    public List<CategoryContent> getContents() {
        return Collections.unmodifiableList(contents);
    }

    public void addContent(final CategoryContent content) {
        contents.add(content);
    }
}
