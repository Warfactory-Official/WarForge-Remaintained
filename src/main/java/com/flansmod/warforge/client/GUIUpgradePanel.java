package com.flansmod.warforge.client;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.IngredientDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionUpgradeGuiData;
import com.flansmod.warforge.common.network.PacketRequestUpgrade;
import com.flansmod.warforge.server.StackComparable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreIngredient;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.flansmod.warforge.common.WarForgeMod.NETWORK;
import static com.flansmod.warforge.common.WarForgeMod.UPGRADE_HANDLER;

public class GUIUpgradePanel {
    public static final int WIDTH = 332;
    public static final int HEIGHT = 292;
    private static final int CONTENT_LEFT = 12;
    private static final int HEADER_Y = 12;
    private static final int BODY_Y = 54;
    private static final int ACTIONS_Y = 252;

    public static ModularScreen createGui(UUID factionID, String factionName, int level, int color, boolean outrankingOfficer) {
        FactionUpgradeGuiData data = new FactionUpgradeGuiData(Minecraft.getMinecraft().player, factionID);
        data.factionId = factionID;
        data.factionName = factionName;
        data.level = level;
        data.color = color;
        data.outrankingOfficer = outrankingOfficer;
        return new ModularScreen(buildPanel(data));
    }

    public static ModularPanel buildPanel(FactionUpgradeGuiData data) {
        UUID factionID = data.factionId;
        String factionName = data.factionName;
        int level = data.level;
        int color = data.color;
        boolean outrankingOfficer = data.outrankingOfficer;
        ListWidget list = new ListWidget<>()
                .scrollDirection(GuiAxis.Y)
//                .keepScrollBarInArea(true)
                .background(GuiTextures.SLOT_ITEM)
                .widthRel(0.98f)
                .height(30 * 6 + 10);


        if (UPGRADE_HANDLER.getRequirementsFor(level + 1) == null) {
            return createNoMoreLevelsPanel();
        }

        // Sorted by quantity
        Map<StackComparable, Integer> requirements = UPGRADE_HANDLER.getRequirementsFor(level + 1)
                .entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        EntityPlayer player = Minecraft.getMinecraft().player;
        AtomicBoolean requirementPassed = new AtomicBoolean(true);
        List<ItemStack> inventory = player.inventory.mainInventory;

        List<StackComparable.StackComparableResult> results = requirements.entrySet().stream()
                .map(entry -> {
                    StackComparable sc = entry.getKey();
                    int required = entry.getValue();

                    int count = inventory.stream()
                            .filter(stack -> !stack.isEmpty() && sc.equals(stack))
                            .mapToInt(ItemStack::getCount)
                            .sum();

                    if (count < required)
                        requirementPassed.set(false);

                    return new StackComparable.StackComparableResult(sc, count, required);
                })
                .collect(Collectors.toList());

        AtomicInteger index = new AtomicInteger(0);
        results.forEach((comparableResult) -> {
            Ingredient ingredient;
            String displayName;

            // tries to get oredict ingredient, then tries to get raw item as ingredient
            if (comparableResult.compared.getOredict() != null) {
                ingredient = new OreIngredient(comparableResult.compared.getOredict());
                displayName = humanizeOreDictName(comparableResult.compared.getOredict());
            } else if (comparableResult.compared.getRegistryName() != null) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(comparableResult.compared.getRegistryName()));
                if (item == null || item == Items.AIR) {
                    WarForgeMod.LOGGER.error("Could not find item: " + comparableResult.compared.getRegistryName() + ". Skipping...");
                    return;
                }
                int meta = comparableResult.compared.getMeta() == -1 ? 0 : comparableResult.compared.getMeta();
                ItemStack stack = new ItemStack(item, 1, meta);
                ingredient = Ingredient.fromStacks(stack);
                displayName = stack.getDisplayName();
            } else {
                WarForgeMod.LOGGER.error("Malformed StackComparable: \n" + comparableResult + "\nSkipping...");
                return;
            }


            list.addChild(createRequirementRow(ingredient, displayName, comparableResult.required, comparableResult.has), index.getAndIncrement());
        });


        ModularPanel panel = ModularPanel.defaultPanel("citadel_upgrade_panel")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);

        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(0, 0, WIDTH, 40, 0xFF171B1F, 0xFF0D1013)).size(WIDTH, 40));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, BODY_Y, WIDTH - CONTENT_LEFT * 2, 188, 0xEE20262B, 0xEE11161A)).size(WIDTH - CONTENT_LEFT * 2, 188).pos(CONTENT_LEFT, BODY_Y));
        panel.child(new IDrawable.DrawableWidget(sectionBackdrop(CONTENT_LEFT, ACTIONS_Y, WIDTH - CONTENT_LEFT * 2, 28, 0xEE20262B, 0xEE11161A)).size(WIDTH - CONTENT_LEFT * 2, 28).pos(CONTENT_LEFT, ACTIONS_Y));
        panel.child(new IDrawable.DrawableWidget(colorStripe(0xFF000000 | (color & 0x00FFFFFF), 0, 0, 6, HEIGHT)).size(6, HEIGHT));
        panel.child(ButtonWidget.panelCloseButton().pos(WIDTH - 18, 8));

        Widget prefix = IKey.str("Citadel Upgrade")
                .asWidget()
                .pos(CONTENT_LEFT, HEADER_Y)
                .color(0xFFFFFF)
                .shadow(true)
                .style(TextFormatting.BOLD)
                .scale(1.15f);

        Widget factionNamePlate = IKey.str(factionName)
                .asWidget()
                .color(color)
                .shadow(true)
                .pos(CONTENT_LEFT, HEADER_Y + 15)
                .style(TextFormatting.BOLD)
                .scale(1.0f);

        panel.child(prefix);
        panel.child(factionNamePlate);
        panel.child(IKey.str("Requirements to advance from level " + level + " to level " + (level + 1)).asWidget()
                .pos(CONTENT_LEFT + 10, BODY_Y + 8)
                .color(0xB8BDC3));

        ButtonWidget<?> closeButton = new ButtonWidget<>()
                .size(94, 18)
                .overlay(IKey.str("Close").color(0xFFFFFF))
                .background(GuiTextures.MC_BUTTON)
                .hoverBackground(GuiTextures.MC_BUTTON_HOVERED)
                .onMousePressed(button -> {
                    panel.closeIfOpen();
                    return true;
                })
                .pos(CONTENT_LEFT + 8, ACTIONS_Y + 5);

        ButtonWidget<?> upgradeButton = new ButtonWidget<>()
                .size(104, 18)
                .overlay(IKey.str("Upgrade").color(requirementPassed.get() && outrankingOfficer ? 0xFFFFFF : 0x8A8A8A))
                .hoverBackground(new DynamicDrawable(() -> (requirementPassed.get() && outrankingOfficer) ?
                        GuiTextures.MC_BUTTON_HOVERED :
                        GuiTextures.MC_BUTTON_DISABLED))
                .background(new DynamicDrawable(() -> (requirementPassed.get() && outrankingOfficer) ?
                        GuiTextures.MC_BUTTON :
                        GuiTextures.MC_BUTTON_DISABLED))
                .onMousePressed(button -> {
                    if (!(requirementPassed.get() && outrankingOfficer)) return false;
                    PacketRequestUpgrade packet = new PacketRequestUpgrade();
                    packet.factionID = factionID;
                    NETWORK.sendToServer(packet);
                    panel.closeIfOpen();

                    return true;
                })
                .pos(WIDTH - CONTENT_LEFT - 112, ACTIONS_Y + 5);

        if (!outrankingOfficer || !requirementPassed.get())
            upgradeButton.tooltip(richTooltip -> {
                richTooltip.addLine(IKey.str("You don't meet following requirements:").style(TextFormatting.BOLD, TextFormatting.RED));
                if (!outrankingOfficer) richTooltip.addLine(IKey.str("- You need to be officer of or higher!"));
                if (!requirementPassed.get()) richTooltip.addLine(IKey.str("- You don't have the materials required!"));
            });


        UITexture ARROW = UITexture.builder()
                .location(ModularUI.ID, "gui/widgets/progress_bar_arrow")
                .imageSize(20, 40)
                .subAreaXYWH(0, 20, 20, 20)
                .build();

        Widget newLevel = IKey.str("Level " + level + " -> " + (level + 1)).asWidget()
                .color(0xFFE19A)
                .shadow(true)
                .height(16);

        Widget claimIncrease = new Row()
                .child(IKey.str("" + UPGRADE_HANDLER.getClaimLimitForLevel(level))
                        .asWidget()
                        .scale(1.3f)
                        .shadow(true)
                        .color(0xFF5555)

                )
                .child(new IDrawable.DrawableWidget(ARROW)
                        .height(8)
                        .width(10)
                        .margin(4, 0)
                )
                .child(IKey.str("" + UPGRADE_HANDLER.getClaimLimitForLevel(level + 1))
                        .asWidget()
                        .shadow(true)
                        .scale(1.3f)
                        .style(TextFormatting.BOLD)
                        .color(0x55FF55)
                )
                .mainAxisAlignment(Alignment.MainAxis.CENTER)
                .expanded();

        Widget insuranceIncrease = new Row()
                .child(IKey.str("" + UPGRADE_HANDLER.getInsuranceSlotsForLevel(level))
                        .asWidget()
                        .scale(1.15f)
                        .shadow(true)
                        .color(0x6CB6FF))
                .child(new IDrawable.DrawableWidget(ARROW)
                        .height(8)
                        .width(10)
                        .margin(4, 0))
                .child(IKey.str("" + UPGRADE_HANDLER.getInsuranceSlotsForLevel(level + 1))
                        .asWidget()
                        .shadow(true)
                        .scale(1.15f)
                        .style(TextFormatting.BOLD)
                        .color(0x8BFFB0))
                .mainAxisAlignment(Alignment.MainAxis.CENTER)
                .expanded();

        Widget progression = new Column()
                .child(newLevel)
                .child(IKey.str("Claims").asWidget().color(0xC7CCD1).shadow(true))
                .child(claimIncrease)
                .child(IKey.str("Insurance").asWidget().color(0xC7CCD1).shadow(true))
                .child(insuranceIncrease)
                .size(120, 54)
                .pos(WIDTH - CONTENT_LEFT - 132, BODY_Y + 18);

        list.pos(CONTENT_LEFT + 8, BODY_Y + 34);
        list.width(WIDTH - CONTENT_LEFT * 2 - 146);
        list.height(140);

        panel.child(list);
        panel.child(progression);
        panel.child(closeButton);
        panel.child(upgradeButton);

        return panel;

    }

    private static ModularPanel createNoMoreLevelsPanel() {
        ModularPanel panel = ModularPanel.defaultPanel("no_more_levels_panel", 220, 76);
        return panel
                .child(new IDrawable.DrawableWidget(sectionBackdrop(0, 0, 220, 76, 0xFF171B1F, 0xFF0D1013)).size(220, 76))
                .child(new IDrawable.DrawableWidget(colorStripe(0xFF9F7A34, 0, 0, 6, 76)).size(6, 76))
                .child(IKey.str("Your citadel is at it's max level!").asWidget()
                        .pos(16, 16)
                        .shadow(true)
                        .style(TextFormatting.WHITE)
                        .height(16)
                )
                .child(new ButtonWidget<>()
                        .size(96, 18)
                        .overlay(IKey.str("Close").color(0xFFFFFF))
                        .background(GuiTextures.MC_BUTTON)
                        .hoverBackground(GuiTextures.MC_BUTTON_HOVERED)
                        .onMousePressed(button -> {
                            panel.closeIfOpen();
                            return true;
                        })
                        .pos(16, 42)
                )
        ;

    }


    public static String humanizeOreDictName(String oredict) {
        String name = oredict.startsWith("any") ? oredict.substring(3) : oredict;

        List<String> words = new ArrayList<>();
        Matcher m = Pattern.compile("([A-Z]?[a-z]+|[A-Z]+(?![a-z]))").matcher(name);
        while (m.find()) {
            words.add(m.group());
        }

        if (words.isEmpty()) {
            return "Any " + oredict;
        }

        Set<String> types = new HashSet<>(Arrays.asList(
                "ingot", "dust", "plate", "nugget", "ore", "gem", "block", "gear",
                "rod", "wire", "bolt", "screw", "foil", "tiny", "small", "cell",
                "dye", "plastic", "circuit"
        ));

        List<String> capitalized = new ArrayList<>();
        for (String word : words) {
            if (word.length() == 1) {
                capitalized.add(word.toUpperCase());
            } else {
                capitalized.add(Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase());
            }
        }

        String first = words.get(0).toLowerCase();
        if (types.contains(first)) {
            String typeWord = capitalized.remove(0);
            capitalized.add(typeWord);
        }

        return "Any " + String.join(" ", capitalized);

    }

    private static Row createRequirementRow(Ingredient ingredient, String displayName, int count, int has) {
        UITexture CHECK = UITexture.builder()
                .location(ModularUI.ID, "gui/widgets/toggle_config")
                .imageSize(14, 28)
                .subAreaXYWH(2, 16, 10, 10)
                .build();
        return (Row) new Row()
                .child(new IDrawable.DrawableWidget(new IngredientDrawable(ingredient))
                        .size(20, 20)
                        .align(Alignment.CenterLeft)
                )
                .child(IKey.str(displayName).asWidget()
                        .align(Alignment.CenterLeft)
                        .left(20 + 5 + 2)
                        .width(120)
                        .color(0xFFFFFF)
                        .shadow(true)
                )
                .child(IKey.str(has + "/" + count).asWidget()
                        .align(Alignment.CenterLeft)
                        .left(20 + 5 + 2 + 120)
                        .color(has >= count ? 0x55FF55 : 0xFF5555)
                        .shadow(true)
                        .scale(1.5f)
                )
                .child(new IDrawable.DrawableWidget(CHECK)
                        .align(Alignment.CenterRight)
                        .setEnabledIf(x -> has >= count)
                        .size(20, 20)
                        .right(5)
                )
                .paddingLeft(5)
                .paddingRight(5)
                .margin(2, 2)
                .height(30)
                .background(buttonFill(0xFF262D33))
                ;
    }

    private static IDrawable sectionBackdrop(int x, int y, int width, int height, int fillColor, int borderColor) {
        return new IDrawable() {
            @Override
            public void draw(GuiContext context, int drawX, int drawY, int drawWidth, int drawHeight, WidgetTheme theme) {
                Gui.drawRect(drawX, drawY, drawX + width, drawY + height, fillColor);
                Gui.drawRect(drawX, drawY, drawX + width, drawY + 1, borderColor);
                Gui.drawRect(drawX, drawY + height - 1, drawX + width, drawY + height, borderColor);
                Gui.drawRect(drawX, drawY, drawX + 1, drawY + height, borderColor);
                Gui.drawRect(drawX + width - 1, drawY, drawX + width, drawY + height, borderColor);
            }
        };
    }

    private static IDrawable colorStripe(int color, int x, int y, int width, int height) {
        return new IDrawable() {
            @Override
            public void draw(GuiContext context, int drawX, int drawY, int drawWidth, int drawHeight, WidgetTheme theme) {
                Gui.drawRect(drawX, drawY, drawX + width, drawY + height, color);
            }
        };
    }

    private static IDrawable buttonFill(int color) {
        return (context, x, y, width, height, theme) -> {
            Gui.drawRect(x, y, x + width, y + height, color);
            Gui.drawRect(x, y, x + width, y + 1, 0xFF0F0F0F);
            Gui.drawRect(x, y + height - 1, x + width, y + height, 0xFF0F0F0F);
            Gui.drawRect(x, y, x + 1, y + height, 0xFF0F0F0F);
            Gui.drawRect(x + width - 1, y, x + width, y + height, 0xFF0F0F0F);
        };
    }


}
