package com.kartersanamo.bedwars.slimejumps;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.List;

public class SlimeJumpManager implements Listener {
    private List<SlimeJump> slimeJumps = new ArrayList<>();

    public void registerJump(SlimeJump jump) {
        slimeJumps.add(jump);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        for (SlimeJump jump : slimeJumps) {
            jump.handlePlayerMove(event);
        }
    }
}
