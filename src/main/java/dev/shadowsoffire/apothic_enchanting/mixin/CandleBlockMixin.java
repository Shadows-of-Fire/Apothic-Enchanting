package dev.shadowsoffire.apothic_enchanting.mixin;

import org.spongepowered.asm.mixin.Mixin;

import dev.shadowsoffire.apothic_enchanting.api.IEnchantingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = CandleBlock.class, remap = false)
public abstract class CandleBlockMixin extends AbstractCandleBlock implements IEnchantingBlock {

    protected CandleBlockMixin(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public float getArcanaBonus(BlockState state, LevelReader world, BlockPos pos) {
        return 1.25F * state.getValue(CandleBlock.CANDLES).intValue();
    }

}
