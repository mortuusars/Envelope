package io.github.mortuusars.envelope.integration.wood_good;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import net.mehvahdjukaar.every_compat.api.SimpleEntrySet;
import net.mehvahdjukaar.every_compat.api.TabAddMode;
import net.mehvahdjukaar.every_compat.modules.EveryCompatModule;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.moonlight.api.resources.SimpleTagBuilder;
import net.mehvahdjukaar.moonlight.api.resources.pack.ResourceGenTask;
import net.mehvahdjukaar.moonlight.api.set.wood.VanillaWoodTypes;
import net.mehvahdjukaar.moonlight.api.set.wood.WoodType;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Consumer;

public class EnvelopeModule extends EveryCompatModule {
    SimpleEntrySet<WoodType, PigeonholeBlock> pigeonholes;

    public EnvelopeModule() {
        super(Envelope.ID, "env", Envelope.ID);

        pigeonholes = SimpleEntrySet.builder(
                        WoodType.class, "pigeonhole",
                        Envelope.Blocks.OAK_PIGEONHOLE, () -> VanillaWoodTypes.OAK,
                        (w) -> new PigeonholeBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BEEHIVE)
                                .strength(2f)
                                .mapColor(w.getColor()))
                )
                .addTexture(Envelope.resource("block/oak_pigeonhole_end"))
                .addTexture(Envelope.resource("block/oak_pigeonhole_front"))
                .addTextureM(
                        Envelope.resource("block/oak_pigeonhole_front_waste"),
                        Envelope.resource("block/mask_pigeonhole_front_waste")
                )
                .addTexture(Envelope.resource("block/oak_pigeonhole_side"))
                .addTag(BlockTags.MINEABLE_WITH_AXE, Registries.BLOCK)
                .addTag(BlockTags.DOES_NOT_BLOCK_HOPPERS, Registries.BLOCK)
                .addTag(Envelope.Tags.Blocks.PIGEONHOLES, Registries.BLOCK)
                .addTag(Envelope.Tags.Items.PIGEONHOLES, Registries.ITEM)
                .addTile(Envelope.BlockEntityTypes.PIGEONHOLE)
                .setTab(getTab(CreativeModeTabs.FUNCTIONAL_BLOCKS))
                .setTabMode(TabAddMode.AFTER_SAME_TYPE)
                .defaultRecipe()
                .build();

        this.addEntry(pigeonholes);
    }

    @Override
    public void onModInit() {
        super.onModInit();
        RegHelper.addExtraBEBlockStatesRegistration(event -> {
            pigeonholes.blocks.forEach((w, block) -> {
                event.addBlocks(Envelope.BlockEntityTypes.PIGEONHOLE.get(), block);
            });
        });
        RegHelper.addExtraPOIStatesRegistration(event -> {
            pigeonholes.blocks.forEach((w, block) -> {
                event.addBlock(Envelope.PoiTypes.PIGEONHOLE, block);
            });
        });
    }

    @Override
    public void addDynamicServerResources(Consumer<ResourceGenTask> executor) {
        super.addDynamicServerResources(executor);
        executor.accept((manager, sink) -> {
            pigeonholes.blocks.forEach((w, block) -> {
                if(!w.canBurn()) return;
                sink.addTag(
                        SimpleTagBuilder.of(Envelope.Tags.Blocks.PIGEONHOLES_THAT_BURN)
                                .addEntry(block), Registries.BLOCK);
            });
        });
    }
}
