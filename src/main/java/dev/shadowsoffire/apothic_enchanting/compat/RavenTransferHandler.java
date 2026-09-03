package dev.shadowsoffire.apothic_enchanting.compat;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.payloads.SetRavenStatsPayload;
import dev.shadowsoffire.apothic_enchanting.table.EnchantingStatRegistry;
import dev.shadowsoffire.apothic_enchanting.table.RavenEnchantmentMenu;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class RavenTransferHandler implements IRecipeTransferHandler<RavenEnchantmentMenu, InfusionRecipe> {

    private final IRecipeTransferHandler<RavenEnchantmentMenu, InfusionRecipe> transferHandler;

    public RavenTransferHandler(IRecipeTransferHandlerHelper transferHelper) {
        IRecipeTransferInfo<RavenEnchantmentMenu, InfusionRecipe> transferInfo = transferHelper.createBasicRecipeTransferInfo(
            RavenEnchantmentMenu.class,
            Ench.Menus.RAVEN_ENCHANTING_TABLE,
            InfusionRecipeCategory.TYPE,
            0, 1,
            2, 36);
        this.transferHandler = transferHelper.createUnregisteredRecipeTransferHandler(transferInfo);
    }

    @Override
    public Class<? extends RavenEnchantmentMenu> getContainerClass() {
        return RavenEnchantmentMenu.class;
    }

    @Override
    public Optional<MenuType<RavenEnchantmentMenu>> getMenuType() {
        return Optional.of(Ench.Menus.RAVEN_ENCHANTING_TABLE);
    }

    @Override
    public IRecipeType<InfusionRecipe> getRecipeType() {
        return InfusionRecipeCategory.TYPE;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(RavenEnchantmentMenu container, InfusionRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        IRecipeTransferError error = this.transferHandler.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, doTransfer);
        if (error != null) {
            return error;
        }

        if (doTransfer) {
            EnchantingStatRegistry.Stats requirements = recipe.getRequirements();
            int eterna = Mth.ceil(requirements.eterna());
            int quanta = Mth.ceil(requirements.quanta());
            int arcana = Mth.ceil(requirements.arcana());

            container.setRavenStats(eterna, quanta, arcana);
            ClientPacketDistributor.sendToServer(new SetRavenStatsPayload(eterna, quanta, arcana));
            ApothicEnchanting.LOGGER.info("soup {}", player.level().isClientSide());
        }

        return null;
    }
}
