package com.westerosblocks.block.custom;

import com.westerosblocks.block.ModBlocks;
import com.westerosblocks.block.ModBlock;
import com.westerosblocks.block.ModBlockFactory;
import com.westerosblocks.block.ModBlockLifecycle;
import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import java.util.List;

public class WCFanBlock extends Block implements ModBlockLifecycle {
    private final Block wallBlock;
    public static class Factory extends ModBlockFactory {
        @Override
        public Block buildBlockClass(ModBlock def) {
            AbstractBlock.Settings fanSettings = def.makeBlockSettings().noCollision().breakInstantly();
            AbstractBlock.Settings wallFanSettings = def.makeBlockSettings().noCollision().breakInstantly();

            Block wallFanBlock = new WCWallFanBlock(wallFanSettings, def);
            Block fanBlock = new WCFanBlock(fanSettings, def, wallFanBlock);

            def.registerRenderType(ModBlocks.registerBlock(def.blockName + "_wall", wallFanBlock), false, false);
            ModBlocks.getCustomBlocksByName().put(def.blockName + "_wall", wallFanBlock);

            return def.registerRenderType(ModBlocks.registerBlock(def.blockName, fanBlock), false, false);
        }
    }

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private final ModBlock def;
    private final boolean allowUnsupported;
    private static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);

    protected WCFanBlock(AbstractBlock.Settings settings, ModBlock def, Block wallFanBlock) {
        super(settings);
        this.def = def;
        this.wallBlock = wallFanBlock;
        boolean allowUnsupported = false;
        String type = def.getType();
        if (type != null) {
            String[] tokens = type.split(",");
            for (String token : tokens) {
                if (token.equals("allow-unsupported")) {
                    allowUnsupported = true;
                    break;
                }
            }
        }
        this.allowUnsupported = allowUnsupported;

        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(WATERLOGGED, false));
    }

    @Override
    public ModBlock getWBDefinition() {
        return def;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = getDefaultState();
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();

        // Check for wall placement first
        for (Direction direction : ctx.getPlacementDirections()) {
            if (direction.getAxis().isHorizontal()) {
                Direction opposite = direction.getOpposite();
                // Check if we can place on the wall
                BlockPos attachPos = pos.offset(opposite);
                if (world.getBlockState(attachPos).isSideSolidFullSquare(world, attachPos, direction)) {
                    return this.wallBlock.getDefaultState()
                            .with(WCWallFanBlock.FACING, direction)
                            .with(WCWallFanBlock.WATERLOGGED, fluidState.isIn(FluidTags.WATER));
                }
            }
        }

        // If not on wall, place normal floor fan
        return state.with(WATERLOGGED, fluidState.isIn(FluidTags.WATER));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canPlaceAt(state, world, pos)) {
            return Blocks.AIR.getDefaultState();
        }

        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (allowUnsupported) return true;
        BlockPos blockBelow = pos.down();
        return world.getBlockState(blockBelow).isSideSolidFullSquare(world, blockBelow, Direction.UP);
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return !state.getFluidState().isEmpty();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
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

    private static final String[] TAGS = {"fans"};

    @Override
    public String[] getBlockTags() {
        return TAGS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        addCustomTooltip(tooltip);
        super.appendTooltip(stack, context, tooltip, options);
    }
}
