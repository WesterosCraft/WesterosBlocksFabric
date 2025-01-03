package com.westerosblocks.block.custom;

import com.westerosblocks.block.ModBlocks;
import com.westerosblocks.block.WesterosBlockDef;
import com.westerosblocks.block.WesterosBlockFactory;
import com.westerosblocks.block.WesterosBlockLifecycle;
import net.minecraft.block.*;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class WCPlantBlock extends Block implements WesterosBlockLifecycle {

    public static class Factory extends WesterosBlockFactory {
        @Override
        public Block buildBlockClass(WesterosBlockDef def) {
            AbstractBlock.Settings settings = def.makeBlockSettings().noCollision().breakInstantly();
            // See if we have a state property
            WesterosBlockDef.StateProperty state = def.buildStateProperty();
            if (state != null) {
                tempSTATE = state;
            }
            String t = def.getType();
            if ((t != null) && (t.contains(WesterosBlockDef.LAYER_SENSITIVE))) {
                tempLAYERS = Properties.LAYERS;
            }
            Block blk = new WCPlantBlock(settings, def);
            return def.registerRenderType(ModBlocks.registerBlock(def.blockName, blk), false, false);
        }
    }

    // Support waterlogged on these blocks
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    private final WesterosBlockDef def;
    protected static WesterosBlockDef.StateProperty tempSTATE;
    protected static IntProperty tempLAYERS;
    protected WesterosBlockDef.StateProperty STATE;
    protected IntProperty LAYERS;
    protected boolean toggleOnUse = false;
    public boolean layerSensitive = false;

    public static final VoxelShape[] SHAPE_BY_LAYER = new VoxelShape[]{
        VoxelShapes.empty(),
        VoxelShapes.cuboid(0.0D, -14.0D, 0.0D, 16.0D, 2.0D, 16.0D),
        VoxelShapes.cuboid(0.0D, -12.0D, 0.0D, 16.0D, 4.0D, 16.0D),
        VoxelShapes.cuboid(0.0D, -10.0D, 0.0D, 16.0D, 6.0D, 16.0D),
        VoxelShapes.cuboid(0.0D, -8.0D, 0.0D, 16.0D, 8.0D, 16.0D),
        VoxelShapes.cuboid(0.0D, -6.0D, 0.0D, 16.0D, 10.0D, 16.0D),
        VoxelShapes.cuboid(0.0D, -4.0D, 0.0D, 16.0D, 12.0D, 16.0D),
        VoxelShapes.cuboid(0.0D, -2.0D, 0.0D, 16.0D, 14.0D, 16.0D),
        VoxelShapes.cuboid(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)
    };

    protected WCPlantBlock(AbstractBlock.Settings settings, WesterosBlockDef def) {
        super(settings);
        this.def = def;
        String t = def.getType();
        if (t != null) {
            String[] toks = t.split(",");
            for (String tok : toks) {
                if (tok.equals("toggleOnUse")) {
                    toggleOnUse = true;
                    break;
                }
            }
        }
        BlockState defbs = this.getDefaultState().with(WATERLOGGED, Boolean.FALSE);
        if (STATE != null) {
            defbs = defbs.with(STATE, STATE.defValue);
        }
        if (LAYERS != null) {
            defbs = defbs.with(LAYERS, 8);
        }
        setDefaultState(defbs);
    }

    @Override
    public WesterosBlockDef getWBDefinition() {
        return def;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState bs = super.getPlacementState(ctx);
        if (bs == null) return null;
        FluidState fluidstate = ctx.getWorld().getFluidState(ctx.getBlockPos());
        bs = bs.with(WATERLOGGED, fluidstate.isIn(FluidTags.WATER));

        if (STATE != null) {
            bs = bs.with(STATE, STATE.defValue);
        }

        if (LAYERS != null) {
            BlockState below = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(Direction.DOWN));
            if ((below != null) && (below.contains(Properties.LAYERS))) {
                Block blk = below.getBlock();
                Integer layer = below.get(Properties.LAYERS);
                // See if soft layer
                if ((blk instanceof SnowBlock) || ((blk instanceof WCLayerBlock) && ((WCLayerBlock) blk).softLayer)) {
                    layer = (layer > 2) ? Integer.valueOf(layer - 2) : Integer.valueOf(1);
                }
                bs = bs.with(LAYERS, layer);
            } else if ((below != null) && (below.getBlock() instanceof SlabBlock)) {
                SlabType slabtype = below.get(Properties.SLAB_TYPE);
                if (slabtype == SlabType.BOTTOM) bs = bs.with(LAYERS, 4);
            }
        }

        return bs;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return switch (type) {
            case LAND -> false;
            case WATER -> state.getFluidState().isIn(FluidTags.WATER);
            case AIR -> false;
            default -> false;
        };
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        if (tempSTATE != null) {
            STATE = tempSTATE;
            tempSTATE = null;
        }
        if (tempLAYERS != null) {
            LAYERS = tempLAYERS;
            tempLAYERS = null;
        }
        if (STATE != null) {
            builder.add(STATE);
        }
        if (LAYERS != null) {
            builder.add(LAYERS);
        }
        builder.add(WATERLOGGED);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        Hand hand = player.getActiveHand();
        if (this.toggleOnUse && (this.STATE != null) && player.isCreative() && player.getStackInHand(hand).isEmpty()) {
            state = state.cycle(this.STATE);
            world.setBlockState(pos, state, 10);
            world.syncWorldEvent(player, 1006, pos, 0);
            return ActionResult.success(world.isClient);
        } else {
            return ActionResult.PASS;
        }
    }

//     TODO: not sure if required anymore
//	@Override
//	public BlockState getPlant(BlockGetter world, BlockPos pos) {
//		BlockState state = world.getBlockState(pos);
//      	if (state.getBlock() != this) return defaultBlockState();
//      	return state;
//	}

    @Override
    protected boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    // TODO shape is big
//    @Override
//    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
//        if (LAYERS != null) {
//            return SHAPE_BY_LAYER[state.get(LAYERS)];
//        } else {
//            return VoxelShapes.fullCube();
//        }
//    }

    private static final String[] TAGS = {"flowers"};

    @Override
    public String[] getBlockTags() {
        return TAGS;
    }

}
