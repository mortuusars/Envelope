package io.github.mortuusars.envelope.neoforge.datagen.client;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.world.block.PaperBoxBlock;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import io.github.mortuusars.envelope.world.item.component.PaybackDuration;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
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

        itemModels().simpleBlockItem(Envelope.Blocks.MAILBOX.get())
              .transforms()
              .transform(ItemDisplayContext.GUI)
              .rotation(30, -135, 0)
              .scale(0.75f)
              .translation(0, 0.75f, 0)
              .end();

        horizontalBlock(Envelope.Blocks.PACKAGE.get(), models().getExistingFile(modLoc("block/package")));
        horizontalBlock(Envelope.Blocks.SEALED_PACKAGE.get(), models().getExistingFile(modLoc("block/sealed_package")));

        getVariantBuilder(Envelope.Blocks.PAPER_BOX.get()).forAllStates(state -> {
            String[] boxes = {"one", "two", "three", "four"};
            ModelFile.ExistingModelFile model = models().getExistingFile(
                  modLoc("block/paper_box_" + boxes[state.getValue(PaperBoxBlock.BOXES) - 1]));
            return ConfiguredModel.builder()
                  .modelFile(model)
                  .rotationY(state.getValue(PaperBoxBlock.AXIS) == Direction.Axis.X ? 0 : 90)
                  .build();
        });

        itemModels().basicItem(Envelope.Items.ADDRESS_TAG.get());
        itemModels().basicItem(Envelope.Items.SEAL_STAMP.get());

        itemModels().basicItem(Envelope.Items.LETTER_AND_QUILL.get())
              .override()
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_CONTENT, 1)
              .model(model(Envelope.resource("letter_and_quill_content")))
              .end();
        letterTatteredUnfoldedContent(Envelope.Items.LETTER.get());

        itemModels().getBuilder(Envelope.resource("sealed_letter").toString())
              .parent(new ModelFile.UncheckedModelFile("item/generated"))
              .texture("layer0", Envelope.resource("item/letter"))
              .texture("layer1", Envelope.resource("item/letter_seal_overlay"))
              .override()
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_TATTERED, 1)
              .model(itemModels().getBuilder(Envelope.resource("sealed_letter_tattered").toString())
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", Envelope.resource("item/letter_tattered"))
                    .texture("layer1", Envelope.resource("item/letter_seal_overlay")))
              .end();

        itemModels().getBuilder(Envelope.resource("sealed_package").toString())
              .parent(new ModelFile.UncheckedModelFile("item/generated"))
              .texture("layer0", Envelope.resource("item/package"))
              .texture("layer1", Envelope.resource("item/package_seal_overlay"));

        itemModels().basicItem(Envelope.Items.PAPER_BOX.get());
        itemModels().basicItem(Envelope.Items.PACKAGE.get());

        // -- Payback Tag
        {
            ItemModelBuilder paybackTagShort = itemModels().getBuilder("envelope:payback_tag_short")
                  .parent(new ModelFile.UncheckedModelFile("item/generated"))
                  .texture("layer0", Envelope.resource("item/payback_tag_short"));

            ItemModelBuilder paybackTagMedium = itemModels().getBuilder("envelope:payback_tag_medium")
                  .parent(new ModelFile.UncheckedModelFile("item/generated"))
                  .texture("layer0", Envelope.resource("item/payback_tag_medium"));

            ItemModelBuilder paybackTagLong = itemModels().getBuilder("envelope:payback_tag_long")
                  .parent(new ModelFile.UncheckedModelFile("item/generated"))
                  .texture("layer0", Envelope.resource("item/payback_tag_long"));

            itemModels().getBuilder("envelope:payback_tag")
                  .parent(new ModelFile.UncheckedModelFile("envelope:payback_tag_medium"))
                  .override()
                  .predicate(EnvelopeClient.ItemModelOverrides.PAYBACK_TAG_DURATION, PaybackDuration.SHORT.ordinal() / 10f)
                  .model(paybackTagShort)
                  .end()
                  .override()
                  .predicate(EnvelopeClient.ItemModelOverrides.PAYBACK_TAG_DURATION, PaybackDuration.MEDIUM.ordinal() / 10f)
                  .model(paybackTagMedium)
                  .end()
                  .override()
                  .predicate(EnvelopeClient.ItemModelOverrides.PAYBACK_TAG_DURATION, PaybackDuration.LONG.ordinal() / 10f)
                  .model(paybackTagLong)
                  .end();
        }

        itemModels().basicItem(Envelope.Items.PAYBACK_BOX.get());
        itemModels().basicItem(Envelope.Items.PAYBACK_PACKAGE.get());

        itemModels().spawnEggItem(Envelope.Items.PIGEON_SPAWN_EGG.get());
    }

    @SuppressWarnings("UnusedReturnValue")
    protected ItemModelBuilder letterTatteredUnfoldedContent(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return itemModels().basicItem(id)
              .override()
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_TATTERED, 0)
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_UNFOLDED, 1)
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_CONTENT, 0)
              .model(model(id.withSuffix("_unfolded")))
              .end()
              .override()
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_TATTERED, 0)
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_UNFOLDED, 1)
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_CONTENT, 1)
              .model(model(id.withSuffix("_unfolded_content")))
              .end()
              .override()
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_TATTERED, 1)
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_UNFOLDED, 0)
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_CONTENT, 0)
              .model(model(id.withSuffix("_tattered")))
              .end()
              .override()
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_TATTERED, 1)
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_UNFOLDED, 1)
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_CONTENT, 0)
              .model(model(id.withSuffix("_tattered_unfolded")))
              .end()
              .override()
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_TATTERED, 1)
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_UNFOLDED, 1)
              .predicate(EnvelopeClient.ItemModelOverrides.LETTER_CONTENT, 1)
              .model(model(id.withSuffix("_tattered_unfolded_content")))
              .end();
    }

    protected ItemModelBuilder model(ResourceLocation path) {
        return itemModels().getBuilder(path.toString())
              .parent(new ModelFile.UncheckedModelFile("item/generated"))
              .texture("layer0", ResourceLocation.fromNamespaceAndPath(path.getNamespace(), "item/" + path.getPath()));
    }

    protected void pigeonhole(ResourceLocation id, Block block) {
        String baseName = id.getPath();

        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(PigeonholeBlock.FACING);
            int waste = state.getValue(PigeonholeBlock.WASTE_LEVEL);

            String suffix = (waste >= PigeonholeBlock.MAX_WASTE_LEVEL ? "_waste" : "");

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
