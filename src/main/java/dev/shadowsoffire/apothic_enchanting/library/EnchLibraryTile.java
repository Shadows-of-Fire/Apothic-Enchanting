package dev.shadowsoffire.apothic_enchanting.library;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;

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
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public abstract class EnchLibraryTile extends BlockEntity {

    private static final Codec<Map<String, Integer>> POINTS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    protected final Object2IntMap<Holder<Enchantment>> points = new Object2IntOpenHashMap<>();
    protected final Object2IntMap<Holder<Enchantment>> maxLevels = new Object2IntOpenHashMap<>();
    protected final Set<EnchLibraryContainer> activeContainers = new HashSet<>();
    protected final ResourceHandler<ItemResource> itemHandler = new EnchLibItemHandler();
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
        if (applyBookDeposit(book)) {
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            this.setChanged();
        }
    }

    /**
     * Core deposit math, separated from the side effects so both the legacy {@link #depositBook}
     * entry point and the transactional {@link EnchLibItemHandler#insert} path can share it.
     * Mutates {@link #points} and {@link #maxLevels} in place; returns {@code true} if any changes
     * were made (so callers know whether to fire the network dispatch / setChanged notification).
     */
    private boolean applyBookDeposit(ItemStack book) {
        ItemEnchantments enchs = EnchantmentHelper.getEnchantmentsForCrafting(book);
        if (enchs.isEmpty()) return false;
        for (Object2IntMap.Entry<Holder<Enchantment>> e : enchs.entrySet()) {
            int newPoints = Math.min(this.maxPoints, this.points.getInt(e.getKey()) + levelToPoints(e.getIntValue()));
            if (newPoints < 0) newPoints = this.maxPoints;
            this.points.put(e.getKey(), newPoints);
            this.maxLevels.put(e.getKey(), Math.min(this.getEnchantmentCap(e.getKey()), Math.max(this.maxLevels.getInt(e.getKey()), e.getIntValue())));
        }
        return true;
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

    private Map<String, Integer> toStringMap(Object2IntMap<Holder<Enchantment>> source) {
        Map<String, Integer> out = new HashMap<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> e : source.object2IntEntrySet()) {
            out.put(e.getKey().getKey().identifier().toString(), e.getIntValue());
        }
        return out;
    }

    private void fromStringMap(Map<String, Integer> source, Object2IntMap<Holder<Enchantment>> target, RegistryLookup<Enchantment> lookup) {
        target.clear();
        for (Map.Entry<String, Integer> e : source.entrySet()) {
            Optional<Holder.Reference<Enchantment>> ench = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, Identifier.tryParse(e.getKey())));
            if (ench.isEmpty()) continue;
            target.put(ench.get(), e.getValue().intValue());
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("points", POINTS_CODEC, toStringMap(this.points));
        output.store("levels", POINTS_CODEC, toStringMap(this.maxLevels));
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        RegistryLookup<Enchantment> lookup = input.lookup().lookupOrThrow(Registries.ENCHANTMENT);
        fromStringMap(input.read("points", POINTS_CODEC).orElse(Map.of()), this.points, lookup);
        fromStringMap(input.read("levels", POINTS_CODEC).orElse(Map.of()), this.maxLevels, lookup);
        // Invoked on both the chunk-sync path (via the default handleUpdateTag → loadWithComponents)
        // and the incremental packet path (ClientPacketListener.handleBlockEntityData → loadWithComponents).
        // activeContainers is empty during disk-load, so this is a no-op there.
        this.activeContainers.forEach(EnchLibraryContainer::onChanged);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("points", POINTS_CODEC.encodeStart(NbtOps.INSTANCE, toStringMap(this.points)).getOrThrow());
        tag.put("levels", POINTS_CODEC.encodeStart(NbtOps.INSTANCE, toStringMap(this.maxLevels)).getOrThrow());
        return tag;
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
        return Math.min(this.maxLevel, ApothicEnchanting.getEnchInfo(ench).getMaxLevel(ench));
    }

    public ResourceHandler<ItemResource> getItemHandler(Direction dir) {
        return this.itemHandler;
    }

    /**
     * Snapshot of the library's mutable point/level state, captured by {@link EnchLibItemHandler}
     * before any transactional deposit. Deep-copies both maps so reverting only needs to swap the
     * contents back in place.
     */
    private record LibrarySnapshot(Object2IntOpenHashMap<Holder<Enchantment>> points, Object2IntOpenHashMap<Holder<Enchantment>> maxLevels) {}

    /**
     * Write-only single-slot sink that deposits any inserted enchanted book into the library.
     * <p>
     * Implements proper {@link TransactionContext transaction} support via {@link SnapshotJournal}:
     * <ul>
     * <li>{@link #updateSnapshots} is called before each mutation so an aborted transaction can roll back.</li>
     * <li>{@link #createSnapshot} deep-copies {@code points} and {@code maxLevels} to preserve pre-insert state.</li>
     * <li>{@link #revertToSnapshot} clears both live maps and repopulates them from the snapshot.</li>
     * <li>{@link #onRootCommit} fires {@code setChanged} and the network dispatch — side effects that must only
     * happen after a root transaction successfully commits.</li>
     * </ul>
     */
    private class EnchLibItemHandler extends SnapshotJournal<LibrarySnapshot> implements ResourceHandler<ItemResource> {

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            // Must report the slot's general capacity when queried with an empty resource,
            // or ResourceHandlerUtil.isFull() short-circuits hopper insertion (it reads
            // amount=0, capacity=0 with getResource(index)=EMPTY and concludes the slot is full).
            if (index != 0) return 0;
            return resource.isEmpty() || resource.is(Items.ENCHANTED_BOOK) ? 1 : 0;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return index == 0 && resource.is(Items.ENCHANTED_BOOK);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index != 0 || amount <= 0 || !resource.is(Items.ENCHANTED_BOOK)) return 0;
            ItemStack book = resource.toStack(1);
            // Pre-flight the deposit on a throwaway snapshot to decide whether we actually change state.
            // This avoids opening a SnapshotJournal entry for no-op inserts (e.g. empty enchantment list).
            ItemEnchantments enchs = EnchantmentHelper.getEnchantmentsForCrafting(book);
            if (enchs.isEmpty()) return 0;
            updateSnapshots(transaction);
            applyBookDeposit(book);
            return 1;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        protected LibrarySnapshot createSnapshot() {
            return new LibrarySnapshot(
                new Object2IntOpenHashMap<>(EnchLibraryTile.this.points),
                new Object2IntOpenHashMap<>(EnchLibraryTile.this.maxLevels));
        }

        @Override
        protected void revertToSnapshot(LibrarySnapshot snapshot) {
            EnchLibraryTile.this.points.clear();
            EnchLibraryTile.this.points.putAll(snapshot.points());
            EnchLibraryTile.this.maxLevels.clear();
            EnchLibraryTile.this.maxLevels.putAll(snapshot.maxLevels());
        }

        @Override
        protected void onRootCommit(LibrarySnapshot originalState) {
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(EnchLibraryTile.this);
            EnchLibraryTile.this.setChanged();
        }

    }

    public static class BasicLibraryTile extends EnchLibraryTile {

        public BasicLibraryTile(BlockPos pos, BlockState state) {
            super(Tiles.LIBRARY, pos, state, 16);
        }

    }

    public static class EnderLibraryTile extends EnchLibraryTile {

        public EnderLibraryTile(BlockPos pos, BlockState state) {
            super(Tiles.ENDER_LIBRARY, pos, state, 31);
        }

    }

}
