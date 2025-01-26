package com.westerosblocks.datagen.models;

import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.block.ModBlocks;
import com.westerosblocks.block.WesterosBlockDef;
import com.westerosblocks.block.WesterosBlockLifecycle;
import com.westerosblocks.block.custom.WCTorchBlock;
import com.westerosblocks.block.custom.WCWallTorchBlock;
import com.westerosblocks.datagen.ModelExport;
import net.minecraft.block.Block;
import net.minecraft.data.client.*;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TorchBlockExport extends ModelExport {
    private final BlockStateModelGenerator generator;
    private final Block block;
    private final WesterosBlockDef def;

    private static final String[] FACING_DIRECTIONS = {
            "facing=east", "facing=south", "facing=west", "facing=north"
    };
    private static final int[] ROTATIONS = {0, 90, 180, 270};

    public TorchBlockExport(BlockStateModelGenerator generator, Block block, WesterosBlockDef def) {
        super(generator, block, def);
        this.generator = generator;
        this.block = block;
        this.def = def;
    }

    public void generateBlockStateModels() {
        for (int setIdx = 0; setIdx < def.getRandomTextureSetCount(); setIdx++) {
            WesterosBlockDef.RandomTextureSet set = def.getRandomTextureSet(setIdx);
            if (!def.isCustomModel()) {
                generateTorchModels(generator, set, setIdx);
            }
        }

        generateWallTorchBlockState();
        generateStandingTorchBlockState();
    }

    private void generateStandingTorchBlockState() {
        BlockStateBuilder blockStateBuilder = new BlockStateBuilder(block);
        final Map<String, List<BlockStateVariant>> variants = blockStateBuilder.getVariants();

        for (int setIdx = 0; setIdx < def.getRandomTextureSetCount(); setIdx++) {
            WesterosBlockDef.RandomTextureSet set = def.getRandomTextureSet(setIdx);

            BlockStateVariant standingVariant = BlockStateVariant.create();
            Identifier standingModelId = getModelId("base", setIdx);
            standingVariant.put(VariantSettings.MODEL, standingModelId);

            if (set.weight != null) {
                standingVariant.put(VariantSettings.WEIGHT, set.weight);
            }

            blockStateBuilder.addVariant("", standingVariant, null, variants);
        }

        generateBlockStateFiles(generator, block, variants);
    }

    private void generateWallTorchBlockState() {
        WCWallTorchBlock wallBlock = (WCWallTorchBlock) ModBlocks.getCustomBlocksByName().get("wall_" + def.blockName);
        BlockStateBuilder blockStateBuilder = new BlockStateBuilder(wallBlock);
        final Map<String, List<BlockStateVariant>> variants = blockStateBuilder.getVariants();

        for (int i = 0; i < FACING_DIRECTIONS.length; i++) {
            for (int setIdx = 0; setIdx < def.getRandomTextureSetCount(); setIdx++) {
                WesterosBlockDef.RandomTextureSet set = def.getRandomTextureSet(setIdx);

                BlockStateVariant variant = BlockStateVariant.create();
                Identifier modelId = getModelId("wall", setIdx);
                variant.put(VariantSettings.MODEL, modelId);

                if (set.weight != null) {
                    variant.put(VariantSettings.WEIGHT, set.weight);
                }

                variant.put(VariantSettings.Y, getRotation(ROTATIONS[i]));
                blockStateBuilder.addVariant(FACING_DIRECTIONS[i], variant, null, variants);
            }
        }

        generateBlockStateFiles(generator, wallBlock, variants);
    }

    private void generateTorchModels(BlockStateModelGenerator generator, WesterosBlockDef.RandomTextureSet set, int setIdx) {
        TextureMap torchTextureMap = new TextureMap()
                .put(TextureKey.TORCH, createBlockIdentifier(set.getTextureByIndex(0)));
        // wall torch model
        Identifier wallModelId = getModelId("wall", setIdx);
        Model wallTorchModel = new Model(
                Optional.of(WesterosBlocks.id("block/untinted/template_torch_wall")),
                Optional.empty(),
                TextureKey.TORCH
        );
        wallTorchModel.upload(wallModelId, torchTextureMap, generator.modelCollector);
        // standing torch model
        Identifier torchModelId = getModelId("base", setIdx);
        Model torchModel = new Model(
                Optional.of(WesterosBlocks.id("block/untinted/template_torch")),
                Optional.empty(),
                TextureKey.TORCH
        );
        torchModel.upload(torchModelId, torchTextureMap, generator.modelCollector);

    }

    private Identifier getModelId(String type, int setIdx) {
        return WesterosBlocks.id(String.format("%s%s/%s_v%d", GENERATED_PATH, def.blockName, type, setIdx + 1));
    }

    public static void generateItemModels(ItemModelGenerator itemModelGenerator, Block currentBlock, WesterosBlockDef blockDefinition) {
        if (currentBlock instanceof WCTorchBlock) {
            WesterosBlockDef.RandomTextureSet firstSet = blockDefinition.getRandomTextureSet(0);
            String texturePath = String.format("block/%s", firstSet.getTextureByIndex(0));

            Models.GENERATED.upload(
                    ModelIds.getItemModelId(currentBlock.asItem()),
                    TextureMap.layer0(WesterosBlocks.id(texturePath)),
                    itemModelGenerator.writer
            );
        }

    }
}