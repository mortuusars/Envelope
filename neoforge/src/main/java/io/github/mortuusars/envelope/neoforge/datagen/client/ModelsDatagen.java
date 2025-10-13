package io.github.mortuusars.envelope.neoforge.datagen.client;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.PaperBoxBlock;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModelsDatagen extends BlockStateProvider {
    public ModelsDatagen(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Envelope.ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        Envelope.Blocks.PIGEONHOLES.forEach((id, block) -> {
            pigeonhole(id, block.get());
            itemModels().simpleBlockItem(block.get());
        });

        horizontalBlock(Envelope.Blocks.PACKAGE.get(), models().getExistingFile(modLoc("block/package")));

        getVariantBuilder(Envelope.Blocks.PAPER_BOX.get()).forAllStates(state -> {
            String[] boxes = {"one", "two", "three", "four"};
            ModelFile.ExistingModelFile model = models().getExistingFile(
                modLoc("block/paper_box_" + boxes[state.getValue(PaperBoxBlock.BOXES) - 1]));
            return ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(state.getValue(PaperBoxBlock.AXIS) == Direction.Axis.X ? 0 : 90)
                .build();
        });

        itemModels().basicItem(Envelope.Items.LETTER.get());
        itemModels().basicItem(Envelope.Items.PAPER_BOX.get());
        itemModels().basicItem(Envelope.Items.PACKAGE.get());
        itemModels().basicItem(Envelope.Items.ADDRESS_TAG.get());

        itemModels().spawnEggItem(Envelope.Items.PIGEON_SPAWN_EGG.get());
    }

    protected void pigeonhole(ResourceLocation id, Block block) {
        String baseName = id.getPath();

        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(PigeonholeBlock.FACING);
            int waste = state.getValue(PigeonholeBlock.WASTE_LEVEL);
            boolean hasAddress = state.getValue(PigeonholeBlock.HAS_ADDRESS);
            boolean hasMail = state.getValue(PigeonholeBlock.HAS_MAIL);

            String suffix = (waste >= PigeonholeBlock.MAX_WASTE_LEVEL ? "_waste" : "")
                + (hasAddress ? "_address" : "")
                + (hasMail && hasAddress ? "_mail" : "");

            ModelFile model = models().orientableWithBottom(baseName + suffix,
                Envelope.resource("block/" + baseName + "_side"),
                Envelope.resource("block/" + baseName + "_front" + suffix),
                Envelope.resource("block/" + baseName + "_end"),
                Envelope.resource("block/" + baseName + "_end"));

            return ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(((int) facing.toYRot() + 180) % 360)
                .build();
        });
    }
}
