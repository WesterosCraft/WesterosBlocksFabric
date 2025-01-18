package com.westerosblocks.datagen.models;

import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.block.WesterosBlockDef;
import com.westerosblocks.block.WesterosBlockStateRecord;
import com.westerosblocks.block.custom.WCWallBlock;
import com.westerosblocks.datagen.ModelExport;
import net.minecraft.block.Block;
import net.minecraft.block.enums.WallShape;
import net.minecraft.data.client.*;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

import java.util.*;

public class WallBlockModelHandler extends ModelExport {
    private final BlockStateModelGenerator generator;
    private final Block block;
    private final WesterosBlockDef def;
    private final WCWallBlock wallBlock;

    private static final ModelPart[] PARTS = {
            // Post
            ModelPart.of("post", "true", null, null, null, null, null, null),
            // North low
            ModelPart.of("side", null, "low", null, null, null, true, null),
            // North tall
            ModelPart.of("side_tall", null, "tall", null, null, null, true, null),
            // East low
            ModelPart.of("side", null, null, null, "low", null, true, 90),
            // East tall
            ModelPart.of("side_tall", null, null, null, "tall", null, true, 90),
            // South low
            ModelPart.of("side", null, null, "low", null, null, true, 180),
            // South tall
            ModelPart.of("side_tall", null, null, "tall", null, null, true, 180),
            // West low
            ModelPart.of("side", null, null, null, null, "low", true, 270),
            // West tall
            ModelPart.of("side_tall", null, null, null, null, "tall", true, 270)
    };

    public WallBlockModelHandler(BlockStateModelGenerator generator, Block block, WesterosBlockDef def) {
        super(generator, block, def);
        this.generator = generator;
        this.block = block;
        this.def = def;
        this.wallBlock = (block instanceof WCWallBlock) ? (WCWallBlock) block : null;
    }


    public void generateBlockStateModels() {
        MultipartBlockStateSupplier stateSupplier = MultipartBlockStateSupplier.create(block);

        for (WesterosBlockStateRecord sr : def.states) {
            boolean justBase = sr.stateID == null;
            Set<String> stateIDs = justBase ? null : Collections.singleton(sr.stateID);

            // Generate models if not a custom model
            if (!sr.isCustomModel()) {
                for (int setIdx = 0; setIdx < sr.getRandomTextureSetCount(); setIdx++) {
                    generateWallModels(generator, sr, setIdx);
                }
            }

            // Generate variants for each wall part
            for (ModelPart part : PARTS) {
                for (int setIdx = 0; setIdx < sr.getRandomTextureSetCount(); setIdx++) {
                    WesterosBlockDef.RandomTextureSet set = sr.getRandomTextureSet(setIdx);

                    // Create variant for this part
                    BlockStateVariant variant = BlockStateVariant.create();
                    Identifier modelId = getModelId(part.modExt(), setIdx, sr);
                    variant.put(VariantSettings.MODEL, modelId);

                    if (set.weight != null) {
                        variant.put(VariantSettings.WEIGHT, set.weight);
                    }
                    if (part.uvlock() != null) {
                        variant.put(VariantSettings.UVLOCK, part.uvlock());
                    }
                    if (part.y() != null) {
                        variant.put(VariantSettings.Y, getRotation(part.y()));
                    }

                    // Create base condition
                    When.PropertyCondition baseCondition = part.condition();

                    // Handle state IDs if present
                    if (stateIDs != null) {
                        for (String stateID : stateIDs) {
                            When.PropertyCondition stateCondition = When.create();
                            if (block.getStateManager().getProperty("state") != null) {
                                stateCondition.set(
                                        (WesterosBlockDef.StateProperty) block.getStateManager().getProperty("state"),
                                        stateID
                                );
                            }

                            // Add to supplier with combined conditions
                            stateSupplier.with(
                                    When.allOf(baseCondition, stateCondition),
                                    variant
                            );
                        }
                    } else {
                        // Add to supplier with just base condition
                        stateSupplier.with(baseCondition, variant);
                    }
                }
            }
        }

        // Register the multipart state supplier
        generator.blockStateCollector.accept(stateSupplier);
    }

    private void generateWallModels(BlockStateModelGenerator generator, WesterosBlockStateRecord sr, int setIdx) {
        boolean isTinted = sr.isTinted();
        boolean hasOverlay = sr.getOverlayTextureByIndex(0) != null;
        WesterosBlockDef.RandomTextureSet set = sr.getRandomTextureSet(setIdx);

        generateWallModelVariant("post", set, sr, setIdx, isTinted, hasOverlay);
        generateWallModelVariant("side", set, sr, setIdx, isTinted, hasOverlay);
        generateWallModelVariant("side_tall", set, sr, setIdx, isTinted, hasOverlay);
    }

    private void generateWallModelVariant(String variant, WesterosBlockDef.RandomTextureSet set,
                                          WesterosBlockStateRecord sr, int setIdx, boolean isTinted, boolean hasOverlay) {

        TextureMap textureMap = new TextureMap()
                .put(TextureKey.BOTTOM, createBlockIdentifier(set.getTextureByIndex(0)))
                .put(TextureKey.TOP, createBlockIdentifier(set.getTextureByIndex(1)))
                .put(TextureKey.SIDE, createBlockIdentifier(set.getTextureByIndex(2)))
                .put(TextureKey.PARTICLE, createBlockIdentifier(set.getTextureByIndex(2)));

        if (hasOverlay) {
            textureMap.put(ModTextureKey.BOTTOM_OVERLAY, createBlockIdentifier(sr.getOverlayTextureByIndex(0)))
                    .put(ModTextureKey.TOP_OVERLAY, createBlockIdentifier(sr.getOverlayTextureByIndex(1)))
                    .put(ModTextureKey.SIDE_OVERLAY, createBlockIdentifier(sr.getOverlayTextureByIndex(2)));
        }

        String parentPath = getParentPath(variant, isTinted, hasOverlay);
        Identifier modelId = getModelId(variant, setIdx, sr);

        Model model = new Model(
                Optional.of(Identifier.of(WesterosBlocks.MOD_ID, parentPath)),
                Optional.empty(),
                TextureKey.BOTTOM, TextureKey.TOP, TextureKey.SIDE, TextureKey.PARTICLE
        );

        model.upload(modelId, textureMap, generator.modelCollector);
    }

    private String getParentPath(String variant, boolean isTinted, boolean hasOverlay) {
        String basePath = isTinted ? "tinted/" : "untinted/";
        String modelName = "template_wall_" + variant;
        if (hasOverlay) modelName += "_overlay";
        return "block/" + basePath + modelName;
    }

    private Identifier getModelId(String variant, int setIdx, WesterosBlockStateRecord sr) {
        String path = String.format("%s%s/%s-v%d",
                GENERATED_PATH,
                def.getBlockName(),
                variant,
                setIdx + 1
        );
        if (sr.stateID != null) {
            path = path + "/" + sr.stateID;
        }
        return Identifier.of(WesterosBlocks.MOD_ID, path);
    }

    private record ModelPart(
            String modExt,
            When.PropertyCondition condition,
            Boolean uvlock,
            Integer y
    ) {
        public static ModelPart of(String modExt, String up, String north, String south, String east, String west, Boolean uvlock, Integer y) {
            When.PropertyCondition condition = When.create();
            if (up != null) condition.set(Properties.UP, Boolean.parseBoolean(up));
            if (north != null) condition.set(Properties.NORTH_WALL_SHAPE, WallShape.valueOf(north.toUpperCase()));
            if (south != null) condition.set(Properties.SOUTH_WALL_SHAPE, WallShape.valueOf(south.toUpperCase()));
            if (east != null) condition.set(Properties.EAST_WALL_SHAPE, WallShape.valueOf(east.toUpperCase()));
            if (west != null) condition.set(Properties.WEST_WALL_SHAPE, WallShape.valueOf(west.toUpperCase()));
            return new ModelPart(modExt, condition, uvlock, y);
        }
    }

    public static void generateItemModels(ItemModelGenerator itemModelGenerator, Block block, WesterosBlockDef blockDefinition) {
        WesterosBlockStateRecord firstState = blockDefinition.states.getFirst();
        WesterosBlockDef.RandomTextureSet firstSet = firstState.getRandomTextureSet(0);

        TextureMap textureMap = new TextureMap()
                .put(TextureKey.WALL, createBlockIdentifier(firstSet.getTextureByIndex(0)));

        Models.WALL_INVENTORY.upload(
                ModelIds.getItemModelId(block.asItem()),
                textureMap,
                itemModelGenerator.writer
        );
    }
}