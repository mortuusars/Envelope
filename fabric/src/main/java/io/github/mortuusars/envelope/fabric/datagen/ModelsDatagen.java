package io.github.mortuusars.envelope.fabric.datagen;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.function.Function;

public class ModelsDatagen extends FabricModelProvider {
    public ModelsDatagen(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        Envelope.Blocks.PIGEONHOLES.forEach((id, block) -> {
            createPigeonhole(generators, block.get(), TextureMapping::orientableCubeSameEnds);
        });
    }

    public void createPigeonhole(BlockModelGenerators generators, Block block, Function<Block, TextureMapping> mapping) {
        TextureMapping baseMapping = mapping.apply(block).copyForced(TextureSlot.SIDE, TextureSlot.PARTICLE);
        TextureMapping wasteMapping = baseMapping.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front_waste"));
        TextureMapping addressMapping = baseMapping.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front_address"));
        TextureMapping addressMailMapping = baseMapping.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front_address_mail"));
        TextureMapping wasteAddressMapping = baseMapping.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front_waste_address"));
        TextureMapping wasteAddressMailMapping = baseMapping.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front_waste_address_mail"));

        ResourceLocation base = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.create(block, baseMapping, generators.modelOutput);
        ResourceLocation waste = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM
                .createWithSuffix(block, "_waste", wasteMapping, generators.modelOutput);
        ResourceLocation address = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM
                .createWithSuffix(block, "_address", addressMapping, generators.modelOutput);
        ResourceLocation addressMail = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM
                .createWithSuffix(block, "_address_mail", addressMailMapping, generators.modelOutput);
        ResourceLocation wasteAddress = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM
                .createWithSuffix(block, "_waste_address", wasteAddressMapping, generators.modelOutput);
        ResourceLocation wasteAddressMail = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM
                .createWithSuffix(block, "_waste_address_mail", wasteAddressMailMapping, generators.modelOutput);

        Map<String, ResourceLocation> models = Map.of(
                "", base,
                "_waste", waste,
                "_waste_mail", waste, // mail is only valid when address is present
                "_address", address,
                "_address_mail", addressMail,
                "_mail", base, // mail is only valid when address is present
                "_waste_address", wasteAddress,
                "_waste_address_mail", wasteAddressMail
        );

        generators.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block)
                .with(BlockModelGenerators.createHorizontalFacingDispatch())
                .with(PropertyDispatch.properties(PigeonholeBlock.WASTE_LEVEL, PigeonholeBlock.HAS_ADDRESS, PigeonholeBlock.HAS_MAIL)
                        .generate((wasteLevel, hasAddress, hasMail) -> {
                            boolean hasWaste = wasteLevel >= PigeonholeBlock.MAX_WASTE_LEVEL;

                            StringBuilder modelName = new StringBuilder();
                            if (hasWaste) modelName.append("_waste");
                            if (hasAddress) modelName.append("_address");
                            if (hasMail) modelName.append("_mail");
                            ResourceLocation model = models.get(modelName.toString());

                            return Variant.variant().with(VariantProperties.MODEL, model);
                        })));
    }

    @Override
    public void generateItemModels(ItemModelGenerators generators) {
    }
}
