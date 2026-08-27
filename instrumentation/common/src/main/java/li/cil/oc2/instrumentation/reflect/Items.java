/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.reflect;

import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.container.ItemHandlerUtils;
import li.cil.oc2.common.vm.AbstractVMItemStackHandlers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Items {
    public static Results.Outcome run(final Object root, final List<String> tokens, final ReflectContext ctx) {
        final String action = tokens.isEmpty() ? "list" : tokens.getFirst();
        return switch (action) {
            case "list" -> Results.Outcome.success(list(handlers(root, at(tokens, 1), ctx)));
            case "insert" -> insert(handlers(root, at(tokens, 3), ctx), stack(tokens), count(tokens));
            case "extract" -> extract(handlers(root, at(tokens, 3), ctx), stack(tokens), count(tokens));
            default -> throw new IllegalArgumentException("expected list, insert or extract, got " + action);
        };
    }

    // --------------------------------------------------------------------- //

    private static Results.Slots list(final Map<String, ItemHandler> handlers) {
        final List<Results.Handler> result = new ArrayList<>();
        handlers.forEach((name, handler) -> {
            final List<Results.Slot> slots = new ArrayList<>();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                final ItemStack stack = handler.getStackInSlot(slot);
                slots.add(new Results.Slot(slot,
                    stack.isEmpty() ? null : String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())),
                    stack.getCount()));
            }
            result.add(new Results.Handler(name, slots));
        });
        return new Results.Slots(result);
    }

    private static Results.Outcome insert(final Map<String, ItemHandler> handlers, final ItemStack stack, final int count) {
        final String item = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        ItemStack remaining = stack.copyWithCount(count);
        for (final Map.Entry<String, ItemHandler> entry : handlers.entrySet()) {
            remaining = ItemHandlerUtils.insertItemStack(entry.getValue(), remaining, false);
            if (remaining.getCount() < count) {
                return Results.Outcome.success(new Results.Inserted(
                    item, count, count - remaining.getCount(), remaining.getCount(), entry.getKey()));
            }
        }
        return Results.Outcome.error(new Results.Rejected(item, count, List.copyOf(handlers.keySet())));
    }

    private static Results.Outcome extract(final Map<String, ItemHandler> handlers, final ItemStack stack, final int count) {
        final String item = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        int taken = 0;
        String from = null;
        for (final Map.Entry<String, ItemHandler> entry : handlers.entrySet()) {
            final ItemHandler handler = entry.getValue();
            for (int slot = 0; slot < handler.getSlots() && taken < count; slot++) {
                if (ItemStack.isSameItem(handler.getStackInSlot(slot), stack)) {
                    taken += handler.extractItem(slot, count - taken, false).getCount();
                    from = entry.getKey();
                }
            }
            if (taken >= count) {
                break;
            }
        }
        final Results.Extracted extracted = new Results.Extracted(item, count, taken, from);
        return taken > 0 ? Results.Outcome.success(extracted) : Results.Outcome.error(extracted);
    }

    private static Map<String, ItemHandler> handlers(final Object root, @Nullable final String path,
                                                     final ReflectContext ctx) {
        final Map<String, ItemHandler> result = new LinkedHashMap<>();
        if (path != null) {
            result.put(path, unwrap(Reflect.resolve(root, path, ctx), path));
            return result;
        }

        for (final Field field : FieldUtils.getAllFieldsList(root.getClass())) {
            if (Modifier.isStatic(field.getModifiers())
                || !(ItemHandler.class.isAssignableFrom(field.getType())
                || AbstractVMItemStackHandlers.class.isAssignableFrom(field.getType()))) {
                continue;
            }
            try {
                field.setAccessible(true);
                final Object value = field.get(root);
                if (value != null) {
                    result.putIfAbsent(field.getName(), unwrap(value, field.getName()));
                }
            } catch (final IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return result;
    }

    private static ItemHandler unwrap(@Nullable final Object value, final String name) {
        if (value instanceof final AbstractVMItemStackHandlers handlers) {
            return handlers.combinedItemHandlers;
        }
        if (value instanceof final ItemHandler handler) {
            return handler;
        }
        throw new IllegalArgumentException(name + " is not an item handler");
    }

    private static ItemStack stack(final List<String> tokens) {
        if (tokens.size() < 2) {
            throw new IllegalArgumentException("missing item id");
        }
        final Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(tokens.get(1)))
            .orElseThrow(() -> new IllegalArgumentException("no such item: " + tokens.get(1)));
        return new ItemStack(item);
    }

    private static int count(final List<String> tokens) {
        return tokens.size() > 2 ? Integer.parseInt(tokens.get(2)) : 1;
    }

    @Nullable
    private static String at(final List<String> tokens, final int index) {
        return index < tokens.size() ? tokens.get(index) : null;
    }

    // --------------------------------------------------------------------- //

    private Items() {
    }
}
