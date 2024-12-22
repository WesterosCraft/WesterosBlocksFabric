package com.westerosblocks.block.custom;

import com.westerosblocks.block.WesterosBlockDef;
import com.westerosblocks.block.WesterosBlockLifecycle;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

public class WCWallTorchBlock extends WallTorchBlock implements WesterosBlockLifecycle {
    private WesterosBlockDef def;
    private boolean allow_unsupported = false;
    private boolean no_particle = false;

    private static SimpleParticleType getParticle(String typeStr) {
        if (typeStr != null && typeStr.contains("no-particle")) {
            return new SimpleParticleType(false);
        }
        return ParticleTypes.FLAME;
    }

    protected WCWallTorchBlock(AbstractBlock.Settings settings, WesterosBlockDef def) {
        super(WCWallTorchBlock.getParticle(def.getType()), settings);
        this.def = def;
        String t = def.getType();
        if (t != null) {
            String[] toks = t.split(",");
            for (String tok : toks) {
                if (tok.equals("allow-unsupported")) {
                    allow_unsupported = true;
                }
                else if (tok.equals("no-particle")) {
                    no_particle = true;
                }
            }
        }
    }

    @Override
    public WesterosBlockDef getWBDefinition() {
        return def;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (!this.no_particle) super.animateTick(state, level, pos, rnd);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    	if (this.allow_unsupported) return true;
        return super.canPlaceAt(state, world, pos);
    }

    private static String[] TAGS = { "wall_post_override" };
    @Override
    public String[] getBlockTags() {
    	return TAGS;
    }
}
