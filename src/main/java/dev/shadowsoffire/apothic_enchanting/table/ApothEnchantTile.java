package dev.shadowsoffire.apothic_enchanting.table;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// TODO: Reduce to mixin or attachment?
public class ApothEnchantTile extends EnchantmentTableBlockEntity {

    protected ItemStackHandler inv = new ItemStackHandler(1){
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Tags.Items.ENCHANTING_FUELS);
        };
    };

    public ApothEnchantTile(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", this.inv.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.inv.deserializeNBT(tag.getCompound("inventory"));
    }

    public IItemHandler getItemHandler(Direction dir) {
        return this.inv;
    }

}
