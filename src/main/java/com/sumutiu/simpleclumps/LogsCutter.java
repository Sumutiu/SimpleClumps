package com.sumutiu.simpleclumps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class LogsCutter {
    public static void init(ServerLevel world, Player player, BlockPos pos, BlockState state) {
        if (SimpleClumpsConfig.getEnableTreeCutter()) {
            if (world.isClientSide())
                return;
            if (player.isShiftKeyDown())
                return;
            ItemStack tool = player.getMainHandItem();
            if (!tool.is(ItemTags.AXES))
                return;
            if (!state.is(BlockTags.LOGS))
                return;
            Set<BlockPos> logs = Collect_Logs(world, pos);
            if (!Is_Tree(logs, world))
                return;
            for (BlockPos log : logs) {
                BlockState logState = world.getBlockState(log);
                logState.getBlock().playerDestroy(world, player, log, logState, null, tool);
                world.removeBlock(log, false);
                if (tool.isDamageableItem())
                    tool.hurtAndBreak(1, player, player.getUsedItemHand());
            }
        }
    }

    private static Set<BlockPos> Collect_Logs(ServerLevel world, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!visited.contains(next) && world
                        .getBlockState(next).is(BlockTags.LOGS)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    private static boolean Is_Tree(Set<BlockPos> logs, ServerLevel world) {
        if (logs.isEmpty())
            return false;
        int maxY = logs.stream().mapToInt(Vec3i::getY).max().orElse(0);
        BlockPos top = logs.stream().filter(p -> (p.getY() == maxY)).findFirst().orElse(null);
        if (top == null)
            return false;
        int leaves = 0;
        for (Direction dir : Direction.values()) {
            if (world.getBlockState(top.relative(dir))
                    .is(BlockTags.LEAVES))
                leaves++;
        }
        return (leaves >= 2);
    }
}
