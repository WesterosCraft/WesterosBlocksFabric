package com.westerosblocks.datagen.models;

import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.block.ModBlock;
import com.westerosblocks.datagen.ModelExport;
import net.minecraft.block.Block;
import net.minecraft.data.client.*;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TrapDoorBlockExport extends ModelExport {
    private final BlockStateModelGenerator generator;
    private final Block block;
    private final ModBlock def;

    private static final ModelEntry[] MODEL_ENTRIES = {
            new ModelEntry("facing=north,half=bottom,open=false", "bottom", 0),
            new ModelEntry("facing=south,half=bottom,open=false", "bottom", 0),
            new ModelEntry("facing=east,half=bottom,open=false", "bottom", 0),
            new ModelEntry("facing=west,half=bottom,open=false", "bottom", 0),
            new ModelEntry("facing=north,half=top,open=false", "top", 0),
            new ModelEntry("facing=south,half=top,open=false", "top", 0),
            new ModelEntry("facing=east,half=top,open=false", "top", 0),
            new ModelEntry("facing=west,half=top,open=false", "top", 0),
            new ModelEntry("facing=north,half=bottom,open=true", "open", 0),
            new ModelEntry("facing=south,half=bottom,open=true", "open", 180),
            new ModelEntry("facing=east,half=bottom,open=true", "open", 90),
            new ModelEntry("facing=west,half=bottom,open=true", "open", 270),
            new ModelEntry("facing=north,half=top,open=true", "open", 0),
            new ModelEntry("facing=south,half=top,open=true", "open", 180),
            new ModelEntry("facing=east,half=top,open=true", "open", 90),
            new ModelEntry("facing=west,half=top,open=true", "open", 270)
    };

    private record ModelEntry(String condition, String modelType, int yRotation) {
    }

    public TrapDoorBlockExport(BlockStateModelGenerator generator, Block block, ModBlock def) {
        super(generator, block, def);
        this.generator = generator;
        this.block = block;
        this.def = def;
    }

    public void generateBlockStateModels() {
        BlockStateBuilder blockStateBuilder = new BlockStateBuilder(block);
        final Map<String, List<BlockStateVariant>> variants = blockStateBuilder.getVariants();

        for (ModelEntry entry : MODEL_ENTRIES) {
            for (int setIdx = 0; setIdx < def.getRandomTextureSetCount(); setIdx++) {
                ModBlock.RandomTextureSet set = def.getRandomTextureSet(setIdx);

                BlockStateVariant variant = BlockStateVariant.create();
                Identifier modelId = getModelId(entry.modelType, setIdx);
                variant.put(VariantSettings.MODEL, modelId);

                if (set.weight != null) {
                    variant.put(VariantSettings.WEIGHT, set.weight);
                }

                if (entry.yRotation != 0) {
                    variant.put(VariantSettings.Y, getRotation(entry.yRotation));
                }

                blockStateBuilder.addVariant(entry.condition, variant, null, variants);
            }
        }

        generateBlockStateFiles(generator, block, variants);


        for (int setIdx = 0; setIdx < def.getRandomTextureSetCount(); setIdx++) {
            ModBlock.RandomTextureSet set = def.getRandomTextureSet(setIdx);
            generateTrapdoorModels(generator, set, setIdx);
        }

    }

    private void generateTrapdoorModels(BlockStateModelGenerator generator, ModBlock.RandomTextureSet set, int setIdx) {
        TextureMap textureMap = new TextureMap()
                .put(TextureKey.TEXTURE, createBlockIdentifier(set.getTextureByIndex(0)));

        Models.TEMPLATE_TRAPDOOR_BOTTOM.upload(getModelId("bottom", setIdx), textureMap, generator.modelCollector);
        Models.TEMPLATE_TRAPDOOR_TOP.upload(getModelId("top", setIdx), textureMap, generator.modelCollector);
        Models.TEMPLATE_TRAPDOOR_OPEN.upload(getModelId("open", setIdx), textureMap, generator.modelCollector);
    }

    private Identifier getModelId(String type, int setIdx) {
        return Identifier.of(WesterosBlocks.MOD_ID,
                String.format("%s%s/%s_v%d", GENERATED_PATH, def.getBlockName(), type, setIdx + 1));
    }

    public static void generateItemModels(ItemModelGenerator itemModelGenerator, Block block, ModBlock blockDefinition) {
        String path = String.format("%s%s/bottom_v1", GENERATED_PATH, blockDefinition.blockName);

        itemModelGenerator.register(
                block.asItem(),
                new Model(Optional.of(WesterosBlocks.id(path)), Optional.empty())
        );
    }
}