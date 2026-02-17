package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.slot.PreviewSlot;
import io.github.mortuusars.envelope.world.item.component.PaybackSubject;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PaybackPackageMenu extends PackageMenu {
    protected PaybackSubject subject;

    protected PaybackPackageMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, InteractionHand hand) {
        super(menuType, containerId, inventory, hand);
    }

    public PaybackPackageMenu(int containerId, Inventory inventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PAYBACK_PACKAGE.get(), containerId, inventory, hand);
    }

    public static PaybackPackageMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PaybackPackageMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    public PaybackSubject getPaybackSubject() {
        return subject;
    }

    @Override
    protected void init() {
        this.subject = Objects.requireNonNull(getItemInHand().get(Envelope.DataComponents.PAYBACK_SUBJECT),
              "Item in hand does not have 'envelope:payback_subject' component.");
        super.init();
        addSlot(new PreviewSlot(subject.mail(), 0, 21, 42));
    }

    @Override
    protected void onDestroyed(@NotNull Player player, ItemStack stack) {
        Level level = player.level();
        Vec3 pos = player.position();
        if (!level.isClientSide() && level.getRandom().nextDouble() < getBoxReturnChance()) {
            Containers.dropItemStack(level, pos.x(), pos.y(), pos.z(), createBoxReturnItem());
        }
    }

    protected double getBoxReturnChance() {
        return Config.Server.PAYBACK_PACKAGE_BOX_RETURN_CHANCE.get();
    }

    protected ItemStack createBoxReturnItem() {
        return Mail.createPaybackBox(getPaybackSubject()).get();
    }
}