package com.westerosblocks.block.custom;

import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.block.WesterosBlockDef;
import com.westerosblocks.block.WesterosBlockFactory;
import com.westerosblocks.block.WesterosBlockLifecycle;
import net.minecraft.block.*;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class WCFenceBlock extends FenceBlock implements WesterosBlockLifecycle {

    public static class Factory extends WesterosBlockFactory {
        @Override
        public Block buildBlockClass(WesterosBlockDef def) {
            AbstractBlock.Settings settings = def.makeProperties();
			// See if we have a state property
			WesterosBlockDef.StateProperty state = def.buildStateProperty();
			if (state != null) {
				tempSTATE = state;
			}
            // Process types
            String t = def.getType();
            Boolean doUnconnect = null;
            if (t != null) {
                String[] toks = t.split(",");
                for (String tok : toks) {
                	String[] parts = tok.split(":");
                    // See if we have unconnect
                    if (parts[0].equals("unconnect")) {
                    	doUnconnect = "true".equals(parts[1]);
                    	tempUNCONNECT = UNCONNECT;
                    }
                }
            }
            Block blk = new WCFenceBlock(settings, def, doUnconnect);
            return def.registerRenderType(blk, false, false);
        }
    };
    
    public static final BooleanProperty UNCONNECT = BooleanProperty.create("unconnect");
    protected static BooleanProperty tempUNCONNECT;

    public final boolean unconnect;
    public final Boolean unconnectDef;

	protected static WesterosBlockDef.StateProperty tempSTATE;
	protected WesterosBlockDef.StateProperty STATE;

	protected boolean toggleOnUse = false;
    
    private WesterosBlockDef def;

    protected WCFenceBlock(AbstractBlock.Settings settings, WesterosBlockDef def, Boolean doUnconnect) {
        super(settings);
        this.def = def;

		String t = def.getType();
		if (t != null) {
				String[] toks = t.split(",");
				for (String tok : toks) {
						if (tok.equals("toggleOnUse")) {
								toggleOnUse = true;
						}
				}
		}

        unconnect = (doUnconnect != null);
        unconnectDef = doUnconnect;
        BlockState defbs = this.stateDefinition.any()
                            .setValue(NORTH, Boolean.valueOf(false))
                            .setValue(EAST, Boolean.valueOf(false))
                            .setValue(SOUTH, Boolean.valueOf(false))
                            .setValue(WEST, Boolean.valueOf(false))
                            .setValue(WATERLOGGED, Boolean.valueOf(false));
        if (unconnect) {
            defbs = defbs.setValue(UNCONNECT, unconnectDef);
        }
		if (STATE != null) {
			defbs = defbs.setValue(STATE, STATE.defValue);
		}
        this.registerDefaultState(defbs);
    }

    @Override
    public WesterosBlockDef getWBDefinition() {
        return def;
    }    
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateDefinition) {
    	if (tempUNCONNECT != null) {
    		stateDefinition.add(tempUNCONNECT);
    		tempUNCONNECT = null;
    	}
		if (tempSTATE != null) {
			STATE = tempSTATE;
			tempSTATE = null;
		}
		if (STATE != null) {
			stateDefinition.add(STATE);
		}
    	super.createBlockStateDefinition(stateDefinition);
    }

    @Override  
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    	if (unconnect && unconnectDef) {
    		return this.defaultBlockState();
    	}
    	return super.getStateForPlacement(ctx);
    }
    

    @Override  
    public BlockState updateShape(BlockState state, Direction dir, BlockState nstate, LevelAccessor world, BlockPos pos, BlockPos pos2) {
    	if (unconnect && state.get(UNCONNECT)) {
            if (state.get(WATERLOGGED)) {
                world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
            }
            return state;
    	}
    	return super.updateShape(state, dir, nstate, world, pos, pos);
    }

    @Override  
    public boolean connectsTo(BlockState p_53330_, boolean p_53331_, Direction p_53332_) {
        Block block = p_53330_.getBlock();
        boolean flag = this.isSameFence(p_53330_) && ((!p_53330_.hasProperty(UNCONNECT)) || (!p_53330_.get(UNCONNECT)));
        boolean flag1 = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(p_53330_, p_53332_);
        return !isExceptionForConnection(p_53330_) && p_53331_ || flag || flag1;
    }

    private boolean isSameFence(BlockState p_153255_) {
        return p_153255_.is(BlockTags.FENCES) && p_153255_.is(BlockTags.WOODEN_FENCES) == this.defaultBlockState().is(BlockTags.WOODEN_FENCES);
    }


    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (this.toggleOnUse && (this.STATE != null) && player.isCreative() && player.getMainHandItem().isEmpty()) {
            state = state.cycle(this.STATE);
            level.setBlock(pos, state, 10);
            level.levelEvent(player, 1006, pos, 0);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        else {
            return InteractionResult.PASS;
        }
    }


    private static String[] TAGS = { "fences" };
    private static String[] TAGS2 = { "fences", "wooden_fences" };
    @Override
    public String[] getBlockTags() {
        return def.getMaterial() == AuxMaterial.WOOD ? TAGS2 : TAGS;
    }    
}
