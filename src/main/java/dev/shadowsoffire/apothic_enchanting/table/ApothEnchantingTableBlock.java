package dev.shadowsoffire.apothic_enchanting.table;

import javax.annotation.Nullable;

import dev.shadowsoffire.apothic_enchanting.api.EnchantmentStatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class ApothEnchantingTableBlock extends EnchantingTableBlock {

    public ApothEnchantingTableBlock(Block.Properties props) {
        super(props);
    }

    @Override
    @Nullable
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        BlockEntity tileentity = world.getBlockEntity(pos);
        if (tileentity instanceof EnchantingTableBlockEntity) {
            Component itextcomponent = ((Nameable) tileentity).getDisplayName();
            return new SimpleMenuProvider((id, inventory, player) -> new ApothEnchantmentMenu(id, inventory, ContainerLevelAccess.create(world, pos), tileentity.getData(EnchantmentTableItemHandler.TYPE)), itextcomponent);
        }
        else {
            return null;
        }
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity tileentity = world.getBlockEntity(pos);
            if (tileentity instanceof EnchantingTableBlockEntity) {
                ItemStack fuel = tileentity.getData(EnchantmentTableItemHandler.TYPE).getStackInSlot(0);
                Block.popResource(world, pos, fuel);
                world.removeBlockEntity(pos);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        for (BlockPos offset : BOOKSHELF_OFFSETS) {
            BlockState shelfState = level.getBlockState(pos.offset(offset));
            ((EnchantmentStatBlock) shelfState.getBlock()).spawnTableParticle(shelfState, level, rand, pos, offset);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ApothEnchantingTableBlockEntity(pos, state);
    }

    public static IItemHandler getItemHandler(EnchantingTableBlockEntity be, Direction dir) {
        return be.getData(EnchantmentTableItemHandler.TYPE);
    }

}
