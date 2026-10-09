package com.sumutiu.simpleclumps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class LogsCutter {

    // Bigger "trees" are left alone: protects large log builds and keeps the search short
    private static final int MAX_LOGS = 256;

    // Breaking the other logs fires the block break event again; this stops it from starting another tree
    private static boolean cutting = false;

    public static void init(ServerLevel world, ServerPlayer player, BlockPos pos, BlockState state) {
        if (cutting || !SimpleClumpsConfig.getEnableTreeCutter()) {
            return;
        }
        if (player.isShiftKeyDown()) {
            return;
        }
        if (!player.getMainHandItem().is(ItemTags.AXES)) {
            return;
        }
        if (!state.is(BlockTags.LOGS)) {
            return;
        }
        List<BlockPos> logs = Collect_Logs(world, pos);
        if (logs == null || !Is_Tree(logs, world)) {
            return;
        }

        cutting = true;
        try {
            for (BlockPos log : logs) {
                // Stop when the axe breaks
                if (!player.getMainHandItem().is(ItemTags.AXES)) {
                    break;
                }
                // Skip logs that are gone, protected by spawn protection or outside the world border
                if (!world.getBlockState(log).is(BlockTags.LOGS) || !world.mayInteract(player, log)) {
                    continue;
                }
                // Broken the same way as when the player breaks it: tool damage, drops (none in creative),
                // stats, adventure mode rules, and protection mods that use the block break events
                player.gameMode.destroyBlock(log);
            }
        } finally {
            cutting = false;
        }
    }

    // The logs connected to the broken block (also diagonally, for branches), nearest first.
    // The broken block itself is not included. Returns null if there are more than MAX_LOGS.
    private static List<BlockPos> Collect_Logs(ServerLevel world, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> logs = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (BlockPos near : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                BlockPos next = near.immutable();
                // Never load chunks for this
                if (!visited.add(next) || !world.isLoaded(next) || !world.getBlockState(next).is(BlockTags.LOGS)) {
                    continue;
                }
                if (logs.size() >= MAX_LOGS) {
                    return null;
                }
                logs.add(next);
                queue.add(next);
            }
        }
        return logs;
    }

    // A tree has leaves that grew there (not placed by a player) next to its top logs
    private static boolean Is_Tree(List<BlockPos> logs, ServerLevel world) {
        if (logs.isEmpty()) {
            return false;
        }
        int maxY = logs.stream().mapToInt(Vec3i::getY).max().orElse(0);
        Set<BlockPos> leaves = new HashSet<>();
        for (BlockPos log : logs) {
            if (log.getY() != maxY) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = log.relative(dir);
                if (world.isLoaded(next) && isNaturalLeaves(world.getBlockState(next))) {
                    leaves.add(next);
                }
            }
        }
        return (leaves.size() >= 2);
    }

    // Leaves placed by a player are persistent (they never decay)
    private static boolean isNaturalLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES)
                && state.hasProperty(BlockStateProperties.PERSISTENT)
                && !state.getValue(BlockStateProperties.PERSISTENT);
    }
}
