package me.melontini.tweaks.entity.vehicle.boats;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;


//Ctrl + c, Ctrl + v
public abstract class StorageBoatEntity extends BoatEntityWithBlock implements Inventory, NamedScreenHandlerFactory {
    public DefaultedList<ItemStack> inventory = DefaultedList.ofSize(36, ItemStack.EMPTY);
    @Nullable
    public Identifier lootTableId;
    public long lootSeed;

    public StorageBoatEntity(EntityType<? extends BoatEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.inventory) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        this.generateLoot(null);
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        this.generateLoot(null);
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        this.generateLoot(null);
        ItemStack itemStack = this.inventory.get(slot);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.inventory.set(slot, ItemStack.EMPTY);
            return itemStack;
        }
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.generateLoot(null);
        this.inventory.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }

    }

    @Override
    public void markDirty() {
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (player.isSneaking() || !this.getPassengerList().isEmpty()) {
            player.openHandledScreen(this);
        } else super.interact(player, hand);

        if (!player.world.isClient) {
            PiglinBrain.onGuardedBlockInteracted(player, true);
            return ActionResult.CONSUME;
        } else return ActionResult.SUCCESS;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (!this.isAlive()) {
            return false;
        } else {
            return !(player.squaredDistanceTo(this) > 64.0);
        }
    }

    @Override
    public void remove() {
        if (!this.world.isClient) {
            ItemScatterer.spawn(this.world, this, this);
        }

        super.remove();
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        if (this.lootTableId != null && playerEntity.isSpectator()) {
            return null;
        } else {
            this.generateLoot(playerInventory.player);
            return this.getScreenHandler(i, playerInventory);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.lootTableId != null) {
            nbt.putString("LootTable", this.lootTableId.toString());
            if (this.lootSeed != 0L) {
                nbt.putLong("LootTableSeed", this.lootSeed);
            }
        } else {
            Inventories.writeNbt(nbt, this.inventory);
        }

    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        if (nbt.contains("LootTable", 8)) {
            this.lootTableId = new Identifier(nbt.getString("LootTable"));
            this.lootSeed = nbt.getLong("LootTableSeed");
        } else {
            Inventories.readNbt(nbt, this.inventory);
        }

    }

    @Override
    public void clear() {
        this.generateLoot(null);
        this.inventory.clear();
    }

    public void setLootTable(Identifier id, long lootSeed) {
        this.lootTableId = id;
        this.lootSeed = lootSeed;
    }

    public void generateLoot(@Nullable PlayerEntity player) {
        if (this.lootTableId != null && this.world.getServer() != null) {
            LootTable lootTable = this.world.getServer().getLootManager().getTable(this.lootTableId);
            if (player instanceof ServerPlayerEntity) {
                Criteria.PLAYER_GENERATES_CONTAINER_LOOT.test((ServerPlayerEntity) player, this.lootTableId);
            }

            this.lootTableId = null;
            LootContext.Builder builder = new LootContext.Builder((ServerWorld) this.world).parameter(LootContextParameters.ORIGIN, this.getPos()).random(this.lootSeed);
            if (player != null) {
                builder.luck(player.getLuck()).parameter(LootContextParameters.THIS_ENTITY, player);
            }

            lootTable.supplyInventory(this, builder.build(LootContextTypes.CHEST));
        }

    }

    public abstract ScreenHandler getScreenHandler(int syncId, PlayerInventory playerInventory);

    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
