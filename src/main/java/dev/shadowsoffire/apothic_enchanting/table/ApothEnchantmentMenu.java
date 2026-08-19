package dev.shadowsoffire.apothic_enchanting.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.payloads.CluePayload;
import dev.shadowsoffire.apothic_enchanting.payloads.StatsPayload;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import dev.shadowsoffire.apothic_enchanting.util.MiscUtil;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

@SuppressWarnings("deprecation")
public class ApothEnchantmentMenu extends EnchantmentMenu {

    private static final Identifier VANILLA_BOOK_GUI_TEXTURE = Identifier.withDefaultNamespace("textures/entity/enchantment/enchanting_table_book.png");

    protected EnchantmentTableStats stats = EnchantmentTableStats.INVALID;
    protected final Player player;
    protected final BlockPos pos;

    public ApothEnchantmentMenu(int id, Inventory inv, BlockPos pos) {
        super(id, inv, ContainerLevelAccess.NULL);
        this.player = inv.player;
        this.pos = pos;
        this.slots.clear();
        this.addSecretSlot(new Slot(this.enchantSlots, 0, 15, 47){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSecretSlot(new Slot(this.enchantSlots, 1, 35, 47){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Tags.Items.ENCHANTING_FUELS);
            }
        });
        this.initCommon(inv);
    }

    public ApothEnchantmentMenu(int id, Inventory inv, ContainerLevelAccess wPos, EnchantmentTableItemHandler teInv, BlockPos pos) {
        super(id, inv, wPos);
        this.player = inv.player;
        this.pos = pos;
        this.slots.clear();
        this.addSecretSlot(new Slot(this.enchantSlots, 0, 15, 47){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSecretSlot(new ResourceHandlerSlot(teInv, teInv::set, 0, 35, 47){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Tags.Items.ENCHANTING_FUELS);
            }
        });
        this.initCommon(inv);
    }

    protected Slot addSecretSlot(Slot pSlot) {
        pSlot.index = this.slots.size();
        this.slots.add(pSlot);
        return pSlot;
    }

    private void initCommon(Inventory inv) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSecretSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + 31));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSecretSlot(new Slot(inv, k, 8 + k * 18, 142 + 31));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= this.costs.length) {
            Util.logAndPauseIfInIde(player.getName() + " pressed invalid button id: " + id);
            return false;
        }

        int slot = id;
        int level = this.costs[slot];
        ItemStack toEnchant = this.enchantSlots.getItem(0);
        ItemStack lapis = this.getSlot(1).getItem();
        int cost = slot + 1;
        if ((lapis.isEmpty() || lapis.getCount() < cost) && !player.getAbilities().instabuild) return false;

        if (this.costs[slot] <= 0 || toEnchant.isEmpty() || (player.experienceLevel < cost || player.experienceLevel < this.costs[slot]) && !player.getAbilities().instabuild) return false;

        this.access.execute((world, pos) -> {
            float eterna = this.stats.eterna(player);
            float quanta = this.stats.quanta();
            float arcana = this.stats.arcana();
            boolean stable = this.stats.stable();
            List<EnchantmentInstance> list = this.getEnchantmentList(toEnchant, slot, this.costs[slot]);
            if (!list.isEmpty()) {
                EnchantmentUtils.chargeExperience(player, MiscUtil.getExpCostForSlot(level, slot));
                player.onEnchantmentPerformed(toEnchant, 0); // Pass zero here instead of the cost so no experience is taken, but the method is still called for tracking reasons.
                if (list.get(0).enchantment().is(Ench.Enchantments.INFUSION)) {
                    InfusionRecipe match = InfusionRecipe.findMatch(world, toEnchant, eterna, quanta, arcana);
                    if (match != null) this.enchantSlots.setItem(0, match.assemble(toEnchant, eterna, quanta, arcana));
                    else return;
                }
                else {
                    this.enchantSlots.setItem(0, toEnchant.getItem().applyEnchantments(toEnchant, list));
                }

                if (!player.getAbilities().instabuild) {
                    lapis.shrink(cost);
                    this.getSlot(1).set(lapis);
                }

                player.awardStat(Stats.ENCHANT_ITEM);
                if (player instanceof ServerPlayer sp) {
                    Ench.Triggers.ENCHANTED.trigger(sp, this.enchantSlots.getItem(0), level, eterna, quanta, arcana, stable);
                    CriteriaTriggers.ENCHANTED_ITEM.trigger(sp, this.enchantSlots.getItem(0), level);
                }

                this.enchantSlots.setChanged();
                this.enchantmentSeed.set(player.getEnchantmentSeed());
                this.slotsChanged(this.enchantSlots);
                world.playSound((Player) null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.1F + 0.9F);
            }

        });
        return true;
    }

    /**
     * Callback for when the crafting matrix is changed.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void slotsChanged(Container inventoryIn) {
        this.access.evaluate((world, pos) -> {
            if (inventoryIn == this.enchantSlots) {
                ItemStack toEnchant = inventoryIn.getItem(0);
                this.gatherStats();
                InfusionRecipe match = InfusionRecipe.findItemMatch(world, toEnchant);
                if (toEnchant.getCount() == 1 && (match != null || toEnchant.isEnchantable() && isEnchantableEnough(toEnchant))) {
                    float eterna = this.stats.eterna(this.player);
                    if (eterna < 1.5) eterna = 1.5F; // Allow for enchanting with no bookshelves as vanilla does
                    this.random.setSeed(this.enchantmentSeed.get());

                    for (int slot = 0; slot < 3; ++slot) {
                        this.costs[slot] = ApothEnchantmentHelper.getEnchantmentCost(this.random, slot, eterna, toEnchant);
                        this.enchantClue[slot] = -1;
                        this.levelClue[slot] = -1;

                        if (this.costs[slot] < slot + 1) {
                            this.costs[slot]++;
                        }
                        this.costs[slot] = EventHooks.onEnchantmentLevelSet(world, pos, slot, Math.round(eterna), toEnchant, this.costs[slot]);
                    }

                    // Clamp slot 2's cost to the matched infusion recipe's eterna requirement. Without this, a high-eterna
                    // table (e.g. 100) charges the full slot cost for an infusion that only required, say, 50.
                    InfusionRecipe infusionMatch = InfusionRecipe.findMatch(world, toEnchant, this.stats.eterna(this.player), this.stats.quanta(), this.stats.arcana());
                    if (infusionMatch != null) {
                        int recipeLevel = Math.max((int) Math.ceil(infusionMatch.getRequirements().eterna()), 3);
                        this.costs[2] = Math.min(this.costs[2], recipeLevel);
                    }

                    for (int slot = 0; slot < 3; ++slot) {
                        if (this.costs[slot] > 0) {
                            List<EnchantmentInstance> list = this.getEnchantmentList(toEnchant, slot, this.costs[slot]);

                            if (list != null && !list.isEmpty()) {
                                EnchantmentInstance enchantmentdata = list.remove(this.random.nextInt(list.size()));
                                this.enchantClue[slot] = this.player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getId(enchantmentdata.enchantment().value());
                                this.levelClue[slot] = enchantmentdata.level();
                                int clues = this.stats.clues();
                                List<EnchantmentInstance> clueList = new ArrayList<>();
                                if (clues-- > 0) clueList.add(enchantmentdata);
                                while (clues-- > 0 && !list.isEmpty()) {
                                    clueList.add(list.remove(this.random.nextInt(list.size())));
                                }
                                PacketDistributor.sendToPlayer((ServerPlayer) this.player, new CluePayload(slot, clueList, list.isEmpty()));
                            }
                        }
                    }

                    this.broadcastChanges();
                }
                else {
                    for (int i = 0; i < 3; ++i) {
                        this.costs[i] = 0;
                        this.enchantClue[i] = -1;
                        this.levelClue[i] = -1;
                    }
                    this.stats = EnchantmentTableStats.INVALID;
                    PacketDistributor.sendToPlayer((ServerPlayer) this.player, new StatsPayload(this.stats));
                }
            }
            return this;
        });
    }

    public void gatherStats() {
        this.access.evaluate((world, pos) -> {
            this.stats = EnchantmentTableStats.gatherStats(world, pos);
            PacketDistributor.sendToPlayer((ServerPlayer) this.player, new StatsPayload(this.stats));
            return this;
        }).orElse(this);
    }

    public void setStats(EnchantmentTableStats stats) {
        this.stats = stats;
    }

    @Override
    public MenuType<?> getType() {
        return Ench.Menus.ENCHANTING_TABLE;
    }

    public Identifier getBookGuiTexture() {
        if (this.pos != null && this.player.level().getBlockState(this.pos).getBlock() instanceof BookTexturedTable b) {
            return b.getBookGuiTexture();
        }
        return VANILLA_BOOK_GUI_TEXTURE;
    }

    /**
     * Allows subclasses of {@link EnchantingTableBlock} to work instead of only {@link Blocks#ENCHANTING_TABLE}.
     */
    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate(
            (level, pos) -> level.getBlockState(pos).getBlock() instanceof EnchantingTableBlock
                && player.isWithinBlockInteractionRange(pos, 4.0),
            true);
    }

    private List<EnchantmentInstance> getEnchantmentList(ItemStack stack, int enchantSlot, int level) {
        this.random.setSeed(this.enchantmentSeed.get() + enchantSlot);
        List<EnchantmentInstance> list = ApothEnchantmentHelper.selectEnchantment(this.random, stack, level, this.stats, this.player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT));
        InfusionRecipe match = this.access.evaluate((world, pos) -> Optional.ofNullable(InfusionRecipe.findMatch(world, stack, this.stats.eterna(this.player), this.stats.quanta(), this.stats.arcana()))).get().orElse(null);
        if (enchantSlot == 2 && match != null) {
            list.clear();
            list.add(new EnchantmentInstance(this.player.level().holderOrThrow(Ench.Enchantments.INFUSION), 1));
        }
        return list;
    }

    /**
     * An item can be enchanted if it is not enchanted, or all the enchantments on it are curses.
     * <p>
     * Uses {@link EnchantmentHelper#getEnchantmentsForCrafting} (which reads {@code STORED_ENCHANTMENTS}
     * for enchanted books and {@code ENCHANTMENTS} otherwise) rather than {@link ItemStack#isEnchanted},
     * because {@code isEnchanted} only inspects the active component and reports enchanted books as empty.
     */
    public static boolean isEnchantableEnough(ItemStack stack) {
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchants.isEmpty()) return true;
        return enchants.keySet().stream().allMatch(h -> h.is(EnchantmentTags.CURSE));
    }

}
