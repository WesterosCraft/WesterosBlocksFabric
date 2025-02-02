package com.westerosblocks.block.custom;

import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.block.ModBlocks;
import com.westerosblocks.block.WesterosBlockDef;
import com.westerosblocks.block.WesterosBlockFactory;
import com.westerosblocks.block.WesterosBlockLifecycle;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

public class WCPaneBlock extends PaneBlock implements WesterosBlockLifecycle {

    public static class Factory extends WesterosBlockFactory {
        @Override
        public Block buildBlockClass(WesterosBlockDef def) {
            AbstractBlock.Settings settings = def.makeBlockSettings().nonOpaque();
            String t = def.getType();
            boolean doUnconnect = false;
            if (t != null) {
                String[] toks = t.split(",");
                for (String tok : toks) {
                	String[] parts = tok.split(":");
                    if (parts[0].equals("unconnect")) {
                        doUnconnect = true;
                        tempUNCONNECT = UNCONNECT;
                        break;
                    }
                }
            }
            Block blk = new WCPaneBlock(settings, def, doUnconnect);
            return def.registerRenderType(ModBlocks.registerBlock(def.blockName, blk), false, true);
        }
    }
    
    public static final BooleanProperty UNCONNECT = BooleanProperty.of("unconnect");
    protected static BooleanProperty tempUNCONNECT;

    private WesterosBlockDef def;
    private boolean legacy_model;
    private boolean bars_model;
    public final boolean unconnect;

    protected WCPaneBlock(AbstractBlock.Settings settings, WesterosBlockDef def, boolean doUnconnect) {
        super(settings);
        this.def = def;
        this.unconnect = doUnconnect;
        String t = def.getType();
        if (t != null) {
            String[] toks = t.split(",");
            for (String tok : toks) {
                if (tok.equals("legacy-model")) {
                    legacy_model = true;
                }
                if (tok.equals("bars-model")) {
                    bars_model = true;
                }
            }
        }
        if (doUnconnect) {
            this.setDefaultState(this.getDefaultState().
            		with(NORTH, Boolean.FALSE).
                    with(EAST, Boolean.FALSE).
                    with(SOUTH, Boolean.FALSE).
                    with(WEST, Boolean.FALSE).
                    with(WATERLOGGED, Boolean.FALSE).
                    with(UNCONNECT, Boolean.FALSE));
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        if (tempUNCONNECT != null) {
            builder.add(tempUNCONNECT);
    		tempUNCONNECT = null;
        }
        super.appendProperties(builder);
    }

    @Override
    public BlockState getStateForNeighborUpdate(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            WorldAccess world,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (unconnect && state.get(UNCONNECT)) {
            if (state.get(WATERLOGGED)) {
                world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
            }
            return state;
    	}
    	return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    public boolean isLegacyModel() {
    	return legacy_model;
    }

    public boolean isBarsModel() {
        return bars_model;
    }

    @Override
    public WesterosBlockDef getWBDefinition() {
        return def;
    }
    
    private static final String[] TAGS = {  };
    @Override
    public String[] getBlockTags() {
    	return TAGS;
    }    

}
