package dev.shadowsoffire.apothic_enchanting.compat;

import java.util.List;

import com.google.common.collect.ImmutableList;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import dev.shadowsoffire.apothic_enchanting.util.TooltipUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;

@JeiPlugin
public class EnchJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ApothicEnchanting.loc("enchantment");
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        Registry<Enchantment> enchants = Minecraft.getInstance().level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> randomEnch = enchants.getRandomElementOf(EnchantmentTags.IN_ENCHANTING_TABLE, Minecraft.getInstance().level.random).orElse(enchants.getAny().get());
        Holder<Enchantment> randomCurse = enchants.getRandomElementOf(EnchantmentTags.CURSE, Minecraft.getInstance().level.random).orElse(enchants.getAny().get());

        ItemStack enchDiaSword = new ItemStack(Items.DIAMOND_SWORD);
        enchDiaSword.enchant(randomEnch, 1);

        ItemStack cursedDiaSword = new ItemStack(Items.DIAMOND_SWORD);
        cursedDiaSword.enchant(randomCurse, 1);

        ItemStack enchBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchDiaSword.enchant(randomEnch, 1);

        IVanillaRecipeFactory factory = reg.getVanillaRecipeFactory();

        reg.addRecipes(RecipeTypes.ANVIL, ImmutableList.of(
            factory.createAnvilRecipe(
                cursedDiaSword,
                ImmutableList.of(new ItemStack(Ench.Items.PRISMATIC_WEB)),
                ImmutableList.of(new ItemStack(Items.DIAMOND_SWORD)),
                ApothicEnchanting.loc("prismatic_cobweb")),
            factory.createAnvilRecipe(
                enchDiaSword,
                ImmutableList.of(new ItemStack(Ench.Items.SCRAP_TOME)),
                ImmutableList.of(enchBook),
                ApothicEnchanting.loc("scrap_tome")),
            factory.createAnvilRecipe(
                new ItemStack(Blocks.DAMAGED_ANVIL),
                ImmutableList.of(new ItemStack(Blocks.IRON_BLOCK)),
                ImmutableList.of(new ItemStack(Blocks.ANVIL)),
                ApothicEnchanting.loc("anvil_repair"))));

        reg.addIngredientInfo(new ItemStack(Items.ENCHANTING_TABLE), VanillaTypes.ITEM_STACK, TooltipUtil.lang("info", "enchanting"));
        reg.addIngredientInfo(new ItemStack(Ench.Items.LIBRARY), VanillaTypes.ITEM_STACK, TooltipUtil.lang("info", "library"));
        List<InfusionRecipe> recipes = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(Ench.RecipeTypes.INFUSION)
            .stream()
            .map(RecipeHolder::value)
            .sorted((r1, r2) -> Float.compare(r1.getRequirements().eterna(), r2.getRequirements().eterna()))
            .toList();
        reg.addRecipes(InfusionRecipeCategory.TYPE, recipes);
    }
    
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(
            new RavenTransferHandler(reg.getTransferHelper()),
            InfusionRecipeCategory.TYPE
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(Blocks.ENCHANTING_TABLE), InfusionRecipeCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(Ench.Blocks.APOTHIC_ENCHANTING_TABLE.value()), InfusionRecipeCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(Ench.Blocks.RAVEN_ENCHANTING_TABLE.value()), InfusionRecipeCategory.TYPE);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new InfusionRecipeCategory(reg.getJeiHelpers().getGuiHelper()));
    }

}
