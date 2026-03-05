package com.kartersanamo.bedwars.upgrades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-team state for diamond upgrades and traps.
 * Sharpness: single tier (bought once). Protection: 0–4. Haste: 0–2. Forge: 0–4. Heal Pool / Dragon Buff: boolean.
 * Traps: queue of up to 3; cost 1 / 2 / 4 diamonds for 1st / 2nd / 3rd.
 */
public final class TeamUpgradeState {

    private boolean sharpness;
    private int protection; // 0-4
    private int haste;       // 0-2
    private int forge;       // 0=none, 1=iron, 2=golden, 3=emerald, 4=molten
    private boolean healPool;
    private boolean dragonBuff;
    private final List<TrapType> trapQueue = new ArrayList<>(3);

    public boolean hasSharpness() {
        return sharpness;
    }

    public void setSharpness(final boolean sharpness) {
        this.sharpness = sharpness;
    }

    public int getProtection() {
        return protection;
    }

    public void setProtection(final int protection) {
        this.protection = Math.max(0, Math.min(4, protection));
    }

    public int getHaste() {
        return haste;
    }

    public void setHaste(final int haste) {
        this.haste = Math.max(0, Math.min(2, haste));
    }

    public int getForge() {
        return forge;
    }

    public void setForge(final int forge) {
        this.forge = Math.max(0, Math.min(4, forge));
    }

    public boolean hasHealPool() {
        return healPool;
    }

    public void setHealPool(final boolean healPool) {
        this.healPool = healPool;
    }

    public boolean hasDragonBuff() {
        return dragonBuff;
    }

    public void setDragonBuff(final boolean dragonBuff) {
        this.dragonBuff = dragonBuff;
    }

    public List<TrapType> getTrapQueue() {
        return Collections.unmodifiableList(new ArrayList<>(trapQueue));
    }

    public boolean addTrap(final TrapType trap) {
        if (trapQueue.size() >= 3) {
            return false;
        }
        trapQueue.add(trap);
        return true;
    }

    /** Removes and returns the first queued trap, or null if none. */
    public TrapType consumeNextTrap() {
        return trapQueue.isEmpty() ? null : trapQueue.remove(0);
    }

    public int getTrapCostForNextPurchase() {
        return switch (trapQueue.size()) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 4;
            default -> -1;
        };
    }

    public void reset() {
        sharpness = false;
        protection = 0;
        haste = 0;
        forge = 0;
        healPool = false;
        dragonBuff = false;
        trapQueue.clear();
    }
}
