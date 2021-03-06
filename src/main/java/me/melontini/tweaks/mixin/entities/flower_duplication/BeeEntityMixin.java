package me.melontini.tweaks.mixin.entities.flower_duplication;

import me.melontini.tweaks.Tweaks;
import me.melontini.tweaks.util.LogUtil;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.TallFlowerBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeeEntity.class)
public abstract class BeeEntityMixin extends AnimalEntity {

    @Shadow
    @Nullable BlockPos flowerPos;
    @Shadow
    private BeeEntity.PollinateGoal pollinateGoal;
    @Unique
    private int plantingCoolDown;

    protected BeeEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/AnimalEntity;tick()V", shift = At.Shift.AFTER), method = "tick")
    private void tick(CallbackInfo ci) {
        if (Tweaks.CONFIG.beeFlowerDuplication) {
            BeeEntity bee = (BeeEntity) (Object) this;
            var pollinateGoal = this.pollinateGoal;
            if (plantingCoolDown > 0) {
                --plantingCoolDown;
            }
            if (pollinateGoal != null) {
                if (pollinateGoal.isRunning() && pollinateGoal.completedPollination() && this.canPlant()) {
                    this.growFlower();
                    LogUtil.info(plantingCoolDown);
                    LogUtil.info("{} stopped pollinating flower at {}", bee, flowerPos);
                }
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    private void writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("MT-plantingCoolDown", this.plantingCoolDown);
    }

    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    private void readNbt(NbtCompound nbt, CallbackInfo ci) {
        this.plantingCoolDown = nbt.getInt("MT-plantingCoolDown");
    }

    @Unique
    private void growFlower() {
        if (this.flowerPos != null) {
            var flowerState = world.getBlockState(flowerPos);
            if (flowerState.getBlock() instanceof FlowerBlock flowerBlock) {
                plantingCoolDown = (int) Math.floor(Math.random() * (6490 - 3600) + 3600);
                for (int i = -2; i <= 2; i++) {
                    for (int b = -2; b <= 2; b++) {
                        for (int c = -2; c <= 2; c++) {
                            var pos = new BlockPos(flowerPos.getX() + i, flowerPos.getY() + b, flowerPos.getZ() + c);
                            var state = world.getBlockState(pos);
                            if (state.getBlock() instanceof AirBlock && flowerBlock.canPlaceAt(flowerState, world, pos)) {
                                if (world.getRandom().nextInt(12) == 0) {
                                    world.setBlockState(pos, flowerState);
                                }
                            }
                        }
                    }
                }
            } else if (flowerState.getBlock() instanceof TallFlowerBlock flowerBlock && Tweaks.CONFIG.beeTallFlowerDuplication) {
                plantingCoolDown = (int) Math.floor(Math.random() * (8000 - 3600) + 3600);
                for (int i = -1; i <= 1; i++) {
                    for (int b = -2; b <= 2; b++) {
                        for (int c = -1; c <= 1; c++) {
                            var pos = new BlockPos(flowerPos.getX() + i, flowerPos.getY() + b, flowerPos.getZ() + c);
                            var state = world.getBlockState(pos);
                            if (state.getBlock() instanceof AirBlock && flowerBlock.canPlaceAt(flowerState, world, pos)) {
                                if (world.getRandom().nextInt(6) == 0) {
                                    TallFlowerBlock.placeAt(world, flowerState, pos, Block.NOTIFY_LISTENERS);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Unique
    private boolean canPlant() {
        return this.plantingCoolDown == 0;
    }
}
