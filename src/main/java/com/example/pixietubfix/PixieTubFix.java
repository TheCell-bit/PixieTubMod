package com.example.pixietubfix;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PixieTubFix implements ModInitializer {
    public static final String MOD_ID = "pixietubfix";

    /** Minimum fall distance reported to the Pressing Tub when a player lands in it. */
    public static final float MIN_STOMP_DISTANCE = 1.5f;

    private static final Map<UUID, Long> LAST_STOMP = new ConcurrentHashMap<>();
    private static final Set<UUID> AIRBORNE = ConcurrentHashMap.newKeySet();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkLanding(player);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUUID();
            AIRBORNE.remove(id);
            LAST_STOMP.remove(id);
        });
    }

    /** Soft check by registry ID: kaleidoscope* namespace, path containing pressing_tub. */
    public static boolean isPressingTub(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().startsWith("kaleidoscope") && id.getPath().contains("pressing_tub");
    }

    /** Called by the mixin (server side) so the tick fallback doesn't trigger a second stomp. */
    public static void markStomp(net.minecraft.world.entity.player.Player player) {
        LAST_STOMP.put(player.getUUID(), player.level().getGameTime());
    }

    /**
     * Fallback: vanilla skips Block#fallOn entirely when fallDistance is 0 (fall-immune origins).
     * Detect the air -> ground transition on the server and call fallOn manually.
     */
    private static void checkLanding(ServerPlayer player) {
        UUID id = player.getUUID();

        if (!player.onGround()) {
            AIRBORNE.add(id);
            return;
        }
        if (!AIRBORNE.remove(id)) {
            return; // wasn't airborne, so this isn't a landing
        }

        Level level = player.level();
        long now = level.getGameTime();
        Long last = LAST_STOMP.get(id);
        if (last != null && now - last <= 1) {
            return; // the vanilla path (via the mixin) already handled this landing
        }

        for (BlockPos pos : new BlockPos[]{player.getOnPos(), player.blockPosition()}) {
            BlockState state = level.getBlockState(pos);
            if (isPressingTub(state)) {
                LAST_STOMP.put(id, now);
                state.getBlock().fallOn(level, state, pos, player, MIN_STOMP_DISTANCE);
                return;
            }
        }
    }
}
