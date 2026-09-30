package com.flansmod.warforge.platform;

import com.flansmod.warforge.Tags;
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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers WarForge objects. Make one per module, add the objects in static fields, then call
 * {@link #register(IEventBus)} from the mod constructor.
 */
public final class WFRegistry {
    private final Map<ResourceKey<?>, DeferredRegister<?>> registers = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    private <T> DeferredRegister<T> registerFor(ResourceKey<? extends Registry<T>> registry) {
        return (DeferredRegister<T>) registers.computeIfAbsent(registry, key -> DeferredRegister.create(registry, Tags.MODID));
    }

    public <T, V extends T> RegHandle<V> register(ResourceKey<? extends Registry<T>> registry, String id, Supplier<V> factory) {
        RegistryObject<V> object = registerFor(registry).register(id, factory);
        return new Handle<>(object);
    }

    /** A block. The factory gets fresh properties (with the id set where the version needs it) to add to. */
    public <B extends Block> RegHandle<B> block(String id, Function<BlockBehaviour.Properties, B> factory) {
        return register(Registries.BLOCK, id, () -> factory.apply(BlockBehaviour.Properties.of()));
    }

    /** The item of a block. */
    public RegHandle<Item> blockItem(String id, RegHandle<? extends Block> block) {
        return register(Registries.ITEM, id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    @SafeVarargs
    public final <E extends BlockEntity> RegHandle<BlockEntityType<E>> blockEntity(String id, BlockEntityFactory<E> factory,
                                                                                  RegHandle<? extends Block>... blocks) {
        return register(Registries.BLOCK_ENTITY_TYPE, id, () -> BlockEntityType.Builder.of(factory::create,
                Arrays.stream(blocks).map(RegHandle::get).toArray(Block[]::new)).build(null));
    }

    public void register(IEventBus modBus) {
        registers.values().forEach(register -> register.register(modBus));
    }

    private record Handle<V>(RegistryObject<V> object) implements RegHandle<V> {
        @Override
        public V get() {
            return object.get();
        }

        @Override
        public Holder<?> holder() {
            return object.getHolder().orElseThrow();
        }
    }
}
