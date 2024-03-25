package dev.shadowsoffire.apothic_enchanting.mixin;

import org.spongepowered.asm.mixin.Mixin;

import dev.shadowsoffire.apothic_enchanting.api.IEnchantingBlock;
import net.minecraft.world.level.block.Block;

@Mixin(value = Block.class, remap = false)
public abstract class BlockMixin implements IEnchantingBlock {

}
