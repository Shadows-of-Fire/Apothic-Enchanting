package dev.shadowsoffire.apothic_enchanting.util;

import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import dev.shadowsoffire.placebo.color.GradientColor;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.UsernameCache;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;

public class MiscUtil {

    /**
     * Gets the experience cost when enchanting at a particular slot. This computes the true xp cost as if you had exactly as many levels as the level cost.
     * <p>
     * For a slot S and level L, the costs are the following:<br>
     * S == 0 -> cost = XP(L)<br>
     * S == 1 -> cost = XP(L) + XP(L-1)<br>
     * S == 2 -> cost = XP(L) + XP(L-1) + XP(L-2)
     * <p>
     * And so on and so forth, if there were ever to be more than three slots.
     *
     * @param level The level of the slot
     * @param slot  The slot index
     * @return The cost, in experience points, of buying the enchantment in a particular slot.
     */
    public static int getExpCostForSlot(int level, int slot) {
        int cost = 0;
        for (int i = 0; i <= slot; i++) {
            cost += EnchantmentUtils.getExperienceForLevel(level - i);
        }
        return cost - 1; // Eating exactly the amount will put you one point below the level, so offset by one here.
    }

    /**
     * Since {@link GradientColor} goes 1:1 through the entire array, if we have a unidirectional gradient, we need to make it wrap around.
     * <p>
     * This is done by making a reversed copy and concatenating them together.
     *
     * @param data The original unidirectional gradient data.
     * @return A cyclical gradient.
     */
    public static int[] doubleUpGradient(int[] data) {
        int[] out = new int[data.length * 2];
        System.arraycopy(data, 0, out, 0, data.length);
        for (int i = data.length - 1; i >= 0; i--) {
            out[data.length * 2 - 1 - i] = data[i];
        }
        return out;
    }

    /**
     * Checks if the affix is still on cooldown, if a cooldown was set via {@link #startCooldown(ResourceLocation, int, LivingEntity)}
     */
    public static boolean isOnCooldown(ResourceLocation id, int cooldown, LivingEntity entity) {
        long lastApplied = entity.getPersistentData().getLong("apothic_enchanting.cooldown." + id.toString());
        return lastApplied != 0 && lastApplied + cooldown >= entity.level().getGameTime();
    }

    /**
     * Records the current time as a cooldown tracker. Used in conjunction with {@link #isOnCooldown(ResourceLocation, int, LivingEntity)}
     */
    public static void startCooldown(ResourceLocation id, LivingEntity entity) {
        entity.getPersistentData().putLong("apothic_enchanting.cooldown." + id.toString(), entity.level().getGameTime());
    }

    /**
     * Vanilla Copy: {@link PlayerInteractionManager#tryHarvestBlock} <br>
     * Attempts to harvest a block as if the player with the given uuid
     * harvested it while holding the passed item.
     *
     * @param world    The world the block is in.
     * @param pos      The position of the block.
     * @param mainhand The main hand item that the player is supposibly holding.
     * @param source   The UUID of the breaking player.
     * @return If the block was successfully broken.
     */
    public static boolean breakExtraBlock(ServerLevel world, BlockPos pos, ItemStack mainhand, @Nullable UUID source) {
        BlockState blockstate = world.getBlockState(pos);
        FakePlayer player;
        if (source != null) {
            player = FakePlayerFactory.get(world, new GameProfile(source, UsernameCache.getLastKnownUsername(source)));
            Player realPlayer = world.getPlayerByUUID(source);
            if (realPlayer != null) player.setPos(realPlayer.position());
        }
        else player = FakePlayerFactory.getMinecraft(world);
        player.getInventory().items.set(player.getInventory().selected, mainhand);
        // player.setPos(pos.getX(), pos.getY(), pos.getZ());

        if (blockstate.getDestroySpeed(world, pos) < 0 || !blockstate.canHarvestBlock(world, pos, player)) return false;

        GameType type = player.getAbilities().instabuild ? GameType.CREATIVE : GameType.SURVIVAL;
        int exp = CommonHooks.onBlockBreakEvent(world, type, player, pos);
        if (exp == -1) {
            return false;
        }
        else {
            BlockEntity tileentity = world.getBlockEntity(pos);
            Block block = blockstate.getBlock();
            if ((block instanceof CommandBlock || block instanceof StructureBlock || block instanceof JigsawBlock) && !player.canUseGameMasterBlocks()) {
                world.sendBlockUpdated(pos, blockstate, blockstate, 3);
                return false;
            }
            else if (player.getMainHandItem().onBlockStartBreak(pos, player)) {
                return false;
            }
            else if (player.blockActionRestricted(world, pos, type)) {
                return false;
            }
            else {
                if (player.getAbilities().instabuild) {
                    removeBlock(world, player, pos, false);
                    return true;
                }
                else {
                    ItemStack itemstack = player.getMainHandItem();
                    ItemStack itemstack1 = itemstack.copy();
                    boolean canHarvest = blockstate.canHarvestBlock(world, pos, player);
                    itemstack.mineBlock(world, blockstate, pos, player);
                    if (itemstack.isEmpty() && !itemstack1.isEmpty()) EventHooks.onPlayerDestroyItem(player, itemstack1, InteractionHand.MAIN_HAND);
                    boolean removed = removeBlock(world, player, pos, canHarvest);

                    if (removed && canHarvest) {
                        block.playerDestroy(world, player, pos, blockstate, tileentity, itemstack1);
                    }

                    if (removed && exp > 0) blockstate.getBlock().popExperience(world, pos, exp);

                    return true;
                }
            }
        }
    }

    /**
     * Vanilla Copy: {@link PlayerInteractionManager#removeBlock}
     *
     * @param world      The world
     * @param player     The removing player
     * @param pos        The block location
     * @param canHarvest If the player can actually harvest this block.
     * @return If the block was actually removed.
     */
    public static boolean removeBlock(ServerLevel world, ServerPlayer player, BlockPos pos, boolean canHarvest) {
        BlockState state = world.getBlockState(pos);
        boolean removed = state.onDestroyedByPlayer(world, pos, player, canHarvest, world.getFluidState(pos));
        if (removed) state.getBlock().destroy(world, pos, state);
        return removed;
    }

}
