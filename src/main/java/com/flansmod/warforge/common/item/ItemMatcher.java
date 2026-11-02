package com.flansmod.warforge.common.item;

import com.flansmod.warforge.compat.Regs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;

import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.flansmod.warforge.compat.Ids;

/** Value-equal item or item-tag reference; records, so safe as map/set key. */
public sealed interface ItemMatcher permits ItemMatcher.OfItem, ItemMatcher.OfTag {
    Logger LOGGER = LogManager.getLogger("warforge");

    boolean matches(ItemStack stack);

    Ingredient toIngredient();

    /** Representative stack; tags resolve to their {@code index}-th member. EMPTY if unresolvable. */
    ItemStack toStack(int amount, int index);

    default ItemStack toStack(int amount) {
        return toStack(amount, 0);
    }

    default ItemStack toStack() {
        return toStack(1, 0);
    }

    boolean isTag();

    /** The registry/tag id string (for display + serialization). */
    String id();

    void write(FriendlyByteBuf buf);

    static ItemMatcher of(Item item) {
        return new OfItem(item);
    }

    static ItemMatcher of(TagKey<Item> tag) {
        return new OfTag(tag);
    }

    /** Resolve an item id; null if the id is malformed or no such item is registered. */
    @Nullable
    static ItemMatcher ofItem(String itemId) {
        if (itemId.indexOf(':') != itemId.lastIndexOf(':')) {
            LOGGER.error("Invalid item id '{}': expected namespace:path (1.12 meta suffixes are not supported); entry skipped", itemId);
            return null;
        }
        ResourceLocation rl = Ids.tryParse(itemId);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
            return null;
        }
        return new OfItem(Regs.get(BuiltInRegistries.ITEM, rl));
    }

    /** Build a tag matcher; null if the id is not a valid resource location. */
    @Nullable
    static ItemMatcher ofTag(String tagId) {
        ResourceLocation rl = Ids.tryParse(tagId);
        return rl == null ? null : new OfTag(TagKey.create(Registries.ITEM, rl));
    }

    /** Auto-detect: an existing item tag becomes a tag matcher, otherwise an item matcher. */
    @Nullable
    static ItemMatcher parse(String resource) {
        ResourceLocation rl = Ids.tryParse(resource);
        if (rl != null) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, rl);
            if (Regs.hasTag(BuiltInRegistries.ITEM, tag)) {
                return new OfTag(tag);
            }
        }
        return ofItem(resource);
    }

    @Nullable
    static ItemMatcher read(FriendlyByteBuf buf) {
        boolean tag = buf.readBoolean();
        String id = buf.readUtf();
        return tag ? ofTag(id) : ofItem(id);
    }

    record OfItem(Item item) implements ItemMatcher {
        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(item);
        }

        @Override
        public Ingredient toIngredient() {
            return Ingredient.of(item);
        }

        @Override
        public ItemStack toStack(int amount, int index) {
            return new ItemStack(item, amount);
        }

        @Override
        public boolean isTag() {
            return false;
        }

        @Override
        public String id() {
            return BuiltInRegistries.ITEM.getKey(item).toString();
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(false);
            buf.writeUtf(id());
        }
    }

    record OfTag(TagKey<Item> tag) implements ItemMatcher {
        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(tag);
        }

        @Override
        public Ingredient toIngredient() {
            return Regs.ingredient(tag);
        }

        @Override
        public ItemStack toStack(int amount, int index) {
            List<ItemStack> items = Regs.stacks(tag);
            if (index < 0 || index >= items.size()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = items.get(index).copy();
            stack.setCount(amount);
            return stack;
        }

        @Override
        public boolean isTag() {
            return true;
        }

        @Override
        public String id() {
            return tag.location().toString();
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(true);
            buf.writeUtf(id());
        }
    }
}
