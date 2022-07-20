package me.melontini.tweaks.mixin.world.crop_temperature;

import me.melontini.tweaks.Tweaks;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.block.BambooBlock.STAGE;

@Mixin(BambooBlock.class)
public abstract class BambooBlockMixin extends Block implements Fertilizable {
    public BambooBlockMixin(Settings settings) {
        super(settings);
    }
    @Inject(at = @At("HEAD"), method = "randomTick", cancellable = true)
    public void mTweaks$randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        BambooBlock block = (BambooBlock) (Object) this;
        if (Tweaks.CONFIG.cropsGrowSlowerInCold) {
            if (state.get(STAGE) == 0) {
                float temp = world.getBiome(pos).value().getTemperature();
                var data = Tweaks.PLANT_DATA.get(Registry.BLOCK.getId(block));
                if (data != null) {
                    var rand = temp < 1.0D ? (25 / (18.5 * (temp + 0.2))) : (25 / (18.5 / (temp - 0.2)));
                    if (temp >= data.min && temp <= data.max) {
                        if (world.getRandom().nextInt(3) == 0 && world.isAir(pos.up()) && world.getBaseLightLevel(pos.up(), 0) >= 9) {
                            int bambooCount = block.countBambooBelow(world, pos) + 1;
                            if (bambooCount < 16) {
                                block.updateLeaves(state, world, pos, random, bambooCount);
                            }
                        }
                    } else if (temp > data.max && temp <= data.aMax) {
                        if (world.getRandom().nextInt((int) rand) == 0 && world.isAir(pos.up()) && world.getBaseLightLevel(pos.up(), 0) >= 9) {
                            int bambooCount = block.countBambooBelow(world, pos) + 1;
                            if (bambooCount < 16) {
                                block.updateLeaves(state, world, pos, random, bambooCount);
                            }
                        }
                    } else if (temp < data.min && temp >= data.aMin) {
                        if (world.getRandom().nextInt((int) rand) == 0 && world.isAir(pos.up()) && world.getBaseLightLevel(pos.up(), 0) >= 9) {
                            int bambooCount = block.countBambooBelow(world, pos) + 1;
                            if (bambooCount < 16) {
                                block.updateLeaves(state, world, pos, random, bambooCount);
                            }
                        }
                    }
                } else {
                    if (temp > 0 && temp < 0.6) {
                        //LogUtil.info("cold " + temp);
                        if (world.getRandom().nextInt((int) (25 / (18.5 * (temp + 0.2)))) == 0 && world.isAir(pos.up()) && world.getBaseLightLevel(pos.up(), 0) >= 9) {
                            int bambooCount = block.countBambooBelow(world, pos) + 1;
                            if (bambooCount < 16) {
                                block.updateLeaves(state, world, pos, random, bambooCount);
                            }
                        }
                    } else if (temp >= 0.6) {
                        //LogUtil.info("normal " + temp);
                        if (world.getRandom().nextInt(3) == 0 && world.isAir(pos.up()) && world.getBaseLightLevel(pos.up(), 0) >= 9) {
                            int bambooCount = block.countBambooBelow(world, pos) + 1;
                            if (bambooCount < 16) {
                                block.updateLeaves(state, world, pos, random, bambooCount);
                            }
                        }
                    }
                }
            }
            ci.cancel();
        }
    }
}
