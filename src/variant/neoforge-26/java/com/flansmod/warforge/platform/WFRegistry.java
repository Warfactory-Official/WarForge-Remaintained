package com.flansmod.warforge.platform;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.compat.Ids;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class WFRegistry {
    private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    private <T> DeferredRegister<T> registerFor(ResourceKey<? extends Registry<T>> registry) {
        return (DeferredRegister<T>) registers.computeIfAbsent(registry, key -> DeferredRegister.create(registry, Tags.MODID));
    }

    public <T, V extends T> RegHandle<V> register(ResourceKey<? extends Registry<T>> registry, String id, Supplier<V> factory) {
        DeferredHolder<T, V> holder = registerFor(registry).register(id, factory);
        return new Handle<>(holder);
    }

    public <B extends Block> RegHandle<B> block(String id, Function<BlockBehaviour.Properties, B> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Ids.of(Tags.MODID, id));
        return register(Registries.BLOCK, id, () -> factory.apply(BlockBehaviour.Properties.of().setId(key)));
    }

    public RegHandle<Item> blockItem(String id, RegHandle<? extends Block> block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Ids.of(Tags.MODID, id));
        return register(Registries.ITEM, id,
                () -> new BlockItem(block.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    @SafeVarargs
    public final <E extends BlockEntity> RegHandle<BlockEntityType<E>> blockEntity(String id, BlockEntityFactory<E> factory,
                                                                                  RegHandle<? extends Block>... blocks) {
        return register(Registries.BLOCK_ENTITY_TYPE, id, () -> {
            Set<Block> validBlocks = Arrays.stream(blocks).map(RegHandle::get).collect(Collectors.toUnmodifiableSet());
            return new BlockEntityType<>(factory::create, validBlocks);
        });
    }

    public void register(IEventBus modBus) {
        registers.values().forEach(register -> register.register(modBus));
    }

    private record Handle<T, V extends T>(DeferredHolder<T, V> deferred) implements RegHandle<V> {
        @Override
        public V get() {
            return deferred.get();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Holder<V> holder() {
            return (Holder<V>) deferred;
        }
    }
}
