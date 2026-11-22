package com.example.pixietubfix.mixin;

import com.example.pixietubfix.PixieTubFix;
import com.llamalad7.mixinextras.injector.v2.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Raises the fall distance for players landing on Kaleidoscope Tavern's Pressing Tub.
 * Soft-checked by registry name, so there is no compile-time dependency on any other mod.
 */
@Mixin(Block.class)
public abstract class PixieTubMixin {

    @WrapMethod(method = "fallOn")
    private void pixietubfix$boostFallDistance(Level level, BlockState state, BlockPos pos,
                                               Entity entity, float fallDistance,
                                               Operation<Void> original) {
        if (entity instanceof Player player && PixieTubFix.isPressingTub(state)) {
            if (!level.isClientSide()) {
                PixieTubFix.markStomp(player);
            }
            fallDistance = Math.max(fallDistance, PixieTubFix.MIN_STOMP_DISTANCE);
        }
        original.call(level, state, pos, entity, fallDistance);
    }
}
