package dev.shadowsoffire.apothic_enchanting.table;

import javax.annotation.Nullable;

import dev.shadowsoffire.apothic_enchanting.api.IEnchantingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ApothEnchantBlock extends EnchantmentTableBlock {

    public ApothEnchantBlock(Block.Properties props) {
        super(props);
    }

    @Override
    @Nullable
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        BlockEntity tileentity = world.getBlockEntity(pos);
        if (tileentity instanceof ApothEnchantTile) {
            Component itextcomponent = ((Nameable) tileentity).getDisplayName();
            return new SimpleMenuProvider((id, inventory, player) -> new ApothEnchantmentMenu(id, inventory, ContainerLevelAccess.create(world, pos), (ApothEnchantTile) tileentity), itextcomponent);
        }
        else {
            return null;
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ApothEnchantTile(pPos, pState);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity tileentity = world.getBlockEntity(pos);
            if (tileentity instanceof ApothEnchantTile) {
                Block.popResource(world, pos, ((ApothEnchantTile) tileentity).inv.getStackInSlot(0));
                world.removeBlockEntity(pos);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        for (BlockPos offset : BOOKSHELF_OFFSETS) {

            BlockState shelfState = level.getBlockState(pos.offset(offset));
            ((IEnchantingBlock) shelfState.getBlock()).spawnTableParticle(shelfState, level, rand, pos, offset);
        }

    }

}
