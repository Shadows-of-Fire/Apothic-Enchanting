package dev.shadowsoffire.apothic_enchanting.library;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.Ench.Tiles;
import dev.shadowsoffire.placebo.network.VanillaPacketDispatcher;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public abstract class EnchLibraryTile extends BlockEntity {

    protected final Object2IntMap<Holder<Enchantment>> points = new Object2IntOpenHashMap<>();
    protected final Object2IntMap<Holder<Enchantment>> maxLevels = new Object2IntOpenHashMap<>();
    protected final Set<EnchLibraryContainer> activeContainers = new HashSet<>();
    protected final IItemHandler itemHandler = new EnchLibItemHandler();
    protected final int maxLevel;
    protected final int maxPoints;

    public EnchLibraryTile(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxLevel) {
        super(type, pos, state);
        this.maxLevel = maxLevel;
        this.maxPoints = levelToPoints(maxLevel);
    }

    /**
     * Inserts a book into this library.
     * Handles the updating of the points and max levels maps.
     * Extra enchantment levels that cannot be voided will be destroyed.
     *
     * @param book An enchanted book
     */
    public void depositBook(ItemStack book) {
        if (book.getItem() != Items.ENCHANTED_BOOK) return;
        ItemEnchantments enchs = EnchantmentHelper.getEnchantmentsForCrafting(book);

        for (Object2IntMap.Entry<Holder<Enchantment>> e : enchs.entrySet()) {
            int newPoints = Math.min(this.maxPoints, this.points.getInt(e.getKey()) + levelToPoints(e.getIntValue()));
            if (newPoints < 0) newPoints = this.maxPoints;
            this.points.put(e.getKey(), newPoints);
            this.maxLevels.put(e.getKey(), Math.min(this.getEnchantmentCap(e.getKey()), Math.max(this.maxLevels.getInt(e.getKey()), e.getIntValue())));
        }

        if (enchs.size() > 0) {
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        }

        this.setChanged();
    }

    /**
     * Sets the level on the provided itemstack to the requested level.
     * Does nothing if the operation is impossible.
     * Decrements point values equal to the amount of points required to jump between the current level and the requested level.
     */
    public void extractEnchant(ItemStack stack, Holder<Enchantment> ench, int level) {
        int curLvl = EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(ench);
        if (stack.isEmpty() || !this.canExtract(ench, level, curLvl) || level == curLvl) return;
        ItemEnchantments.Mutable enchs = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(stack));
        enchs.set(ench, level);
        EnchantmentHelper.setEnchantments(stack, enchs.toImmutable());
        this.points.put(ench, Math.max(0, this.points.getInt(ench) - levelToPoints(level) + levelToPoints(curLvl))); // Safety, should never be below zero anyway.

        if (!this.level.isClientSide()) {
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        }

        this.setChanged();
    }

    /**
     * Checks if this level of an enchantment can be extracted from this library, given the current level of the enchantment on the item.
     *
     * @param ench         The enchantment being extracted
     * @param level        The desired target level
     * @param currentLevel The current level of this enchantment on the item being applied to.
     * @return If this level of this enchantment can be extracted.
     */
    public boolean canExtract(Holder<Enchantment> ench, int level, int currentLevel) {
        return this.getMax(ench) >= level && this.points.getInt(ench) >= levelToPoints(level) - levelToPoints(currentLevel);
    }

    /**
     * Converts an enchantment level into the corresponding point value.
     *
     * @param level The level to convert.
     * @return 2^(level - 1)
     */
    public static int levelToPoints(int level) {
        return (int) Math.pow(2, level - 1);
    }

    public void saveEnchData(CompoundTag tag) {
        CompoundTag points = new CompoundTag();
        for (Object2IntMap.Entry<Holder<Enchantment>> e : this.points.object2IntEntrySet()) {
            points.putInt(e.getKey().getKey().location().toString(), e.getIntValue());
        }
        tag.put("points", points);

        CompoundTag levels = new CompoundTag();
        for (Object2IntMap.Entry<Holder<Enchantment>> e : this.maxLevels.object2IntEntrySet()) {
            levels.putInt(e.getKey().getKey().location().toString(), e.getIntValue());
        }
        tag.put("levels", levels);
    }

    public void loadEnchData(CompoundTag tag, RegistryLookup<Enchantment> lookup) {
        CompoundTag points = tag.getCompound("points");
        for (String s : points.getAllKeys()) {
            Optional<Holder.Reference<Enchantment>> ench = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.tryParse(s)));
            if (ench.isEmpty()) {
                continue;
            }

            this.points.put(ench.get(), points.getInt(s));
        }

        CompoundTag levels = tag.getCompound("levels");
        for (String s : levels.getAllKeys()) {
            Optional<Holder.Reference<Enchantment>> ench = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.tryParse(s)));
            if (ench.isEmpty()) {
                continue;
            }

            this.maxLevels.put(ench.get(), levels.getInt(s));
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        saveEnchData(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        loadEnchData(tag, regs.lookupOrThrow(Registries.ENCHANTMENT));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveEnchData(tag);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        loadEnchData(tag, registries.lookupOrThrow(Registries.ENCHANTMENT));
        this.activeContainers.forEach(EnchLibraryContainer::onChanged);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public Object2IntMap<Holder<Enchantment>> getPointsMap() {
        return this.points;
    }

    public Object2IntMap<Holder<Enchantment>> getLevelsMap() {
        return this.maxLevels;
    }

    public void addListener(EnchLibraryContainer ctr) {
        this.activeContainers.add(ctr);
    }

    public void removeListener(EnchLibraryContainer ctr) {
        this.activeContainers.remove(ctr);
    }

    public int getMax(Holder<Enchantment> ench) {
        return Math.min(this.getEnchantmentCap(ench), this.maxLevels.getInt(ench));
    }

    public int getEnchantmentCap(Holder<Enchantment> ench) {
        return Math.min(this.maxLevel, ApothicEnchanting.getEnchInfo(ench).getMaxLevel());
    }

    public IItemHandler getItemHandler(Direction dir) {
        return this.itemHandler;
    }

    private class EnchLibItemHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.getItem() != Items.ENCHANTED_BOOK || stack.getCount() > 1) return stack;
            else if (!simulate) {
                EnchLibraryTile.this.depositBook(stack);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.getItem() == Items.ENCHANTED_BOOK;
        }

    }

    public static class BasicLibraryTile extends EnchLibraryTile {

        public BasicLibraryTile(BlockPos pos, BlockState state) {
            super(Tiles.LIBRARY.get(), pos, state, 16);
        }

    }

    public static class EnderLibraryTile extends EnchLibraryTile {

        public EnderLibraryTile(BlockPos pos, BlockState state) {
            super(Tiles.ENDER_LIBRARY.get(), pos, state, 31);
        }

    }

}
