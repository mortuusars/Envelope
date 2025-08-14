package io.github.mortuusars.envelope.neoforge.datagen.client;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class BlockStatesDatagen extends BlockStateProvider {
    public BlockStatesDatagen(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Envelope.ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        Envelope.Blocks.PIGEONHOLES.forEach((id, block) -> {
            horizontalFaceBlock(block.get(), models().orientableWithBottom(id.getPath(),
                    Envelope.resource("block/" + id.getPath() + "_side"),
                    Envelope.resource("block/" + id.getPath() + "_front"),
                    Envelope.resource("block/" + id.getPath() + "_end"),
                    Envelope.resource("block/" + id.getPath() + "_end")));
        });
    }
}
