package dev.shadowsoffire.apothic_enchanting;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableFloat;

import com.mojang.datafixers.util.Pair;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_enchanting.enchantments.ChainsawTask;
import dev.shadowsoffire.apothic_enchanting.enchantments.components.BerserkingComponent;
import dev.shadowsoffire.apothic_enchanting.enchantments.components.BoonComponent;
import dev.shadowsoffire.apothic_enchanting.enchantments.components.ReflectiveComponent;
import dev.shadowsoffire.apothic_enchanting.objects.ExtractionTomeItem;
import dev.shadowsoffire.apothic_enchanting.objects.ImprovedScrappingTomeItem;
import dev.shadowsoffire.apothic_enchanting.objects.ScrappingTomeItem;
import dev.shadowsoffire.apothic_enchanting.payloads.EnchantmentInfoPayload;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.util.RunnableReloader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ApothEnchEvents {

    private static final MethodHandle dropFromLootTable;
    static {
        Method m = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "dropFromLootTable", DamageSource.class, boolean.class);
        try {
            m.setAccessible(true);
            dropFromLootTable = MethodHandles.lookup().unreflect(m);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("LivingEntity#dropFromLootTable not located!");
        }
    }

    @SubscribeEvent
    public void anvilEvent(AnvilUpdateEvent e) {
        ItemStack left = e.getLeft();

        if (left.isEnchanted() && e.getRight().getItem() == Ench.Items.PRISMATIC_WEB.value()) {
            ItemStack stack = left.copy();
            ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(stack));

            enchants.removeIf(h -> h.is(EnchantmentTags.CURSE));
            EnchantmentHelper.setEnchantments(stack, enchants.toImmutable());

            e.setCost(30);
            e.setMaterialCost(1);
            e.setOutput(stack);
            return;
        }

        if (left.getCount() == 1 && (left.getItem() == Items.CHIPPED_ANVIL || left.getItem() == Items.DAMAGED_ANVIL) && e.getRight().is(Tags.Items.STORAGE_BLOCKS_IRON)) {
            Item target = left.getItem() == Items.CHIPPED_ANVIL ? Items.DAMAGED_ANVIL : Items.ANVIL; // Repair the anvil, chipped -> damaged, damaged -> normal
            ItemStack out = left.transmuteCopy(target);
            e.setOutput(out);
            e.setCost(5);
            e.setMaterialCost(1);
            return;
        }

        if (ScrappingTomeItem.updateAnvil(e)) return;
        if (ImprovedScrappingTomeItem.updateAnvil(e)) return;
        if (ExtractionTomeItem.updateAnvil(e)) return;
    }

    @SubscribeEvent
    public void repairEvent(AnvilRepairEvent e) {
        if (ExtractionTomeItem.updateRepair(e)) return;
    }

    /**
     * Event handler for the Scavenger and Spearfishing enchantments.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void drops(LivingDropsEvent e) throws Throwable {
        if (e.getSource().getEntity() instanceof Player p && !p.level().isClientSide()) {
            try {
                MutableFloat dropChance = new MutableFloat();
                EnchantmentHelper.runIterationOnItem(p.getWeaponItem(), (ench, level) -> {
                    ench.value().modifyDamageFilteredValue(Ench.EnchantEffects.EXTRA_LOOT_ROLL, (ServerLevel) p.level(), level, p.getWeaponItem(), p, e.getSource(), dropChance);
                });

                if (dropChance.floatValue() > 0 && p.level().random.nextFloat() <= dropChance.floatValue()) {
                    e.getEntity().captureDrops(new ArrayList<>());
                    dropFromLootTable.invoke(e.getEntity(), e.getSource(), true);
                    e.getDrops().addAll(e.getEntity().captureDrops(null));
                }
            }
            catch (Throwable t) {
                ApothicEnchanting.LOGGER.catching(t);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void dropsLowest(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof Player) && e.getSource().getEntity() instanceof Player p && !p.level().isClientSide()) {
            ItemStack stack = p.getWeaponItem();
            MutableFloat knowledge = new MutableFloat();
            EnchantmentHelper.runIterationOnItem(stack, (ench, level) -> {
                ench.value().modifyItemFilteredCount(Ench.EnchantEffects.DROPS_TO_XP, (ServerLevel) p.level(), level, stack, knowledge);
            });

            if (knowledge.floatValue() > 0) {
                int totalXp = 0;
                Iterator<ItemEntity> it = e.getDrops().iterator();
                while (it.hasNext()) {
                    ItemEntity i = it.next();
                    if (!i.getItem().is(Ench.Tags.CANNOT_BE_CONVERTED_TO_XP)) {
                        totalXp += i.getItem().getCount() * knowledge.floatValue();
                        it.remove();
                    }
                }

                Entity ded = e.getEntity();
                while (totalXp > 0) {
                    int i = ExperienceOrb.getExperienceValue(totalXp);
                    totalXp -= i;
                    p.level().addFreshEntity(new ExperienceOrb(p.level(), ded.getX(), ded.getY(), ded.getZ(), i));
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void healing(LivingHealEvent e) {
        if (e.getEntity().getType() == EntityType.ARMOR_STAND) return; // https://github.com/Shadows-of-Fire/Apotheosis/issues/636
        if (e.getEntity().level().isClientSide) return;
        if (e.getAmount() <= 0F) return;

        EnchantmentHelper.getRandomItemWith(Ench.EnchantEffects.REPAIR_WITH_HP, e.getEntity(), s -> s.isDamaged()).ifPresent(itemInUse -> {
            ItemStack stack = itemInUse.itemStack();
            MutableFloat duraPerHp = new MutableFloat();
            EnchantmentHelper.runIterationOnItem(stack, (ench, level) -> {
                ench.value().modifyItemFilteredCount(Ench.EnchantEffects.REPAIR_WITH_HP, (ServerLevel) e.getEntity().level(), level, stack, duraPerHp);
            });
            if (duraPerHp.floatValue() > 0) {
                float cost = 1F / duraPerHp.floatValue();
                int maxRestore = Math.min(Mth.floor(e.getAmount() / cost), stack.getDamageValue());
                e.setAmount(e.getAmount() - maxRestore * cost);
                stack.setDamageValue(stack.getDamageValue() - maxRestore);
                if (itemInUse.inSlot() != null && itemInUse.owner() != null) {
                    itemInUse.owner().setItemSlot(itemInUse.inSlot(), stack);
                }
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void block(LivingShieldBlockEvent e) {
        LivingEntity user = e.getEntity();
        Entity attacker = e.getDamageSource().getDirectEntity();
        ItemStack shield = user.getUseItem();
        Pair<ReflectiveComponent, Integer> reflect = EnchantmentHelper.getHighestLevel(shield, Ench.EnchantEffects.REFLECTIVE);
        if (reflect != null) {
            float chance = reflect.getFirst().procChance().calculate(reflect.getSecond());
            if (user.level().random.nextFloat() <= chance) {
                DamageSource src = user.level().damageSources().indirectMagic(user, user);
                if (attacker instanceof LivingEntity livingAttacker) {
                    float ratio = reflect.getFirst().reflectRatio().calculate(reflect.getSecond());
                    livingAttacker.hurt(src, ratio * e.getBlockedDamage());
                    shield.hurtAndBreak(10, user, LivingEntity.getSlotForHand(user.getUsedItemHand()));
                }
            }
        }
    }

    // @SubscribeEvent
    // public void looting(LootingLevelEvent e) {
    // DamageSource src = e.getDamageSource();
    // if (src != null && src.getDirectEntity() instanceof ThrownTrident trident) {
    // ItemStack triStack = trident.getPickupItemStackOrigin();
    // e.setLootingLevel(triStack.getEnchantmentLevel(Enchantments.MOB_LOOTING));
    // }
    // }

    /**
     * Event handler for the Stable Footing and Miner's Fervor enchants.
     */
    @SubscribeEvent
    public void breakSpeed(PlayerEvent.BreakSpeed e) {
        Player p = e.getEntity();
        if (!p.onGround() && e.getOriginalSpeed() < e.getNewSpeed() * 5) {

            MutableBoolean flag = new MutableBoolean(false);
            EnchantmentHelper.runIterationOnEquipment(p, (ench, level, item) -> {
                if (ench.value().effects().has(Ench.EnchantEffects.STABLE_FOOTING)) {
                    flag.setTrue();
                }
            });

            if (flag.getValue()) {
                e.setNewSpeed(e.getNewSpeed() * 5F);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void breakSpeedLow(PlayerEvent.BreakSpeed e) {
        Player p = e.getEntity();
        ItemStack stack = p.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }

        Pair<LevelBasedValue, Integer> fervor = EnchantmentHelper.getHighestLevel(stack, Ench.EnchantEffects.MINERS_FERVOR);
        if (fervor != null) {
            if (stack.getDestroySpeed(e.getState()) > 1.0F) {
                float hardness = e.getState().getDestroySpeed(p.level(), e.getPosition().orElse(BlockPos.ZERO));
                e.setNewSpeed(Math.min(29.9999F, fervor.getFirst().calculate(fervor.getSecond())) * hardness);
            }
        }
    }

    /**
     * Event handler for the Boon of the Earth enchant.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void breakSpeed(BlockEvent.BreakEvent e) {
        ChainsawTask.attemptChainsaw(e);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void blockDrops(BlockDropsEvent e) {
        BoonComponent.provideBenefits(e);
    }

    /**
     * Event handler for the Nature's Blessing enchantment.
     */
    @SubscribeEvent
    public void rightClick(PlayerInteractEvent.RightClickBlock e) {
        ItemStack s = e.getItemStack();
        Pair<LevelBasedValue, Integer> bonemealCost = EnchantmentHelper.getHighestLevel(s, Ench.EnchantEffects.BONEMEAL_CROPS);
        if (bonemealCost != null && !e.getEntity().isShiftKeyDown() && BoneMealItem.applyBonemeal(s.copy(), e.getLevel(), e.getPos(), e.getEntity())) {
            int cost = (int) bonemealCost.getFirst().calculate(bonemealCost.getSecond());
            if (cost > 0) {
                s.hurtAndBreak(cost, e.getEntity(), LivingEntity.getSlotForHand(e.getHand()));
            }
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void livingHurt(LivingDamageEvent.Post e) {
        BerserkingComponent.attemptToGoBerserk(e);
    }

    @SubscribeEvent
    public void reload(AddReloadListenerEvent e) {
        e.addListener(RunnableReloader.of(() -> {
            Configuration enchInfoConfig = new Configuration(ApothicAttributes.getConfigFile("enchantments"));
            enchInfoConfig.setTitle("Apotheosis Enchantment Information");
            enchInfoConfig.setComment("This file contains configurable data for each enchantment.\nThe names of each category correspond to the registry names of every loaded enchantment.");
            ApothicEnchanting.ENCHANTMENT_INFO.clear();

            e.getServerResources().getRegistryLookup().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(ench -> {
                EnchantmentInfo info = EnchantmentInfo.load(ench, enchInfoConfig);
                ApothicEnchanting.ENCHANTMENT_INFO.put(ench, info);
                for (int i = 1; i <= info.getMaxLevel(); i++) {
                    if (info.getMinPower(i) > info.getMaxPower(i)) {
                        ApothicEnchanting.LOGGER.warn("Enchantment {} has min/max power {}/{} at level {}, making this level unobtainable except by combination.",
                            ench.key().location(),
                            info.getMinPower(i),
                            info.getMaxPower(i), i);
                    }
                }
            });

            if (enchInfoConfig.hasChanged()) enchInfoConfig.save();
        }));
    }

    @SubscribeEvent
    public void stopped(ServerStoppedEvent e) {
        ApothicEnchanting.ENCHANTMENT_INFO.clear();
    }

    @SubscribeEvent
    public void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        // ApothicEnchanting.ENCHANTMENT_INFO.clear();
    }

    @SubscribeEvent
    public void sync(OnDatapackSyncEvent e) {
        e.getRelevantPlayers().forEach(p -> {
            PacketDistributor.sendToPlayer(p, new EnchantmentInfoPayload(ApothicEnchanting.ENCHANTMENT_INFO));
        });
    }

    @SubscribeEvent
    public void clamp(GetEnchantmentLevelEvent e) {
        ItemEnchantments.Mutable enchantments = e.getEnchantments();
        Iterator<Holder<Enchantment>> it = enchantments.keySet().iterator();
        while (it.hasNext()) {
            Holder<Enchantment> ench = it.next();
            EnchantmentInfo info = ApothicEnchanting.getEnchInfo(ench);
            if (info.levelCap() != -1 && enchantments.getLevel(ench) > info.levelCap()) {
                enchantments.set(ench, info.levelCap());
            }
        }
    }
}
