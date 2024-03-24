package dev.shadowsoffire.apothic_enchanting.objects;

import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.api.IEnchantingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class TreasureShelfBlock extends TypedShelfBlock implements IEnchantingBlock {

    public TreasureShelfBlock(Properties props) {
        super(props, Ench.Particles.ENCHANT_SCULK);
    }

    @Override
    public boolean allowsTreasure(BlockState state, LevelReader world, BlockPos pos) {
        return true;
    }

    @Override
    public float getQuantaBonus(BlockState state, LevelReader world, BlockPos pos) {
        return -10F;
    }

    @Override
    public float getArcanaBonus(BlockState state, LevelReader world, BlockPos pos) {
        return 10F;
    }

}
