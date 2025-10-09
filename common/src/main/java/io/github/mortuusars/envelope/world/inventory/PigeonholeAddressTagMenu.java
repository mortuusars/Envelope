package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.core.address.AllAddresses;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public class PigeonholeAddressTagMenu extends AbstractContainerMenu {
    public static final int MAX_NAME_LENGTH = 22;
    public static final int APPLY_BUTTON_ID = 0;

    protected final Inventory playerInventory;
    protected final Player player;
    protected final InteractionHand hand;
    protected final BlockPos pos;
    protected final AllAddresses addresses;
    protected final @Nullable Address.Pigeonhole currentAddress;

    protected final DataSlot dataAddressValidation = DataSlot.standalone();

    protected String address = "";

    protected PigeonholeAddressTagMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                                       InteractionHand hand, BlockPos pos, AllAddresses addresses,
                                       @Nullable Address.Pigeonhole currentAddress) {
        super(type, containerId);
        this.playerInventory = playerInventory;
        this.player = playerInventory.player;
        this.hand = hand;
        this.pos = pos;
        this.addresses = addresses;
        this.currentAddress = currentAddress;
        this.address = getInitialAddressValue();

        addDataSlot(dataAddressValidation);
    }

    public PigeonholeAddressTagMenu(int containerId, Inventory playerInventory,
                                    InteractionHand hand, BlockPos pos, AllAddresses addresses,
                                    @Nullable Address.Pigeonhole currentAddress) {
        this(Envelope.MenuTypes.PIGEONHOLE_ADDRESS.get(), containerId,
                playerInventory, hand, pos, addresses, currentAddress);
    }

    public static PigeonholeAddressTagMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        return new PigeonholeAddressTagMenu(containerId, playerInventory,
                buffer.readEnum(InteractionHand.class),
                buffer.readBlockPos(),
                AllAddresses.STREAM_CODEC.decode(buffer),
                Address.Pigeonhole.STREAM_CODEC.apply(ByteBufCodecs::optional).decode(buffer).orElse(null));
    }

    protected String getInitialAddressValue() {
        return Optional.ofNullable(currentAddress)
                .map(Address::id)
                .orElseGet(() ->
                        Optional.ofNullable(player.getItemInHand(hand).get(Envelope.DataComponents.ADDRESS))
                                .map(Address::getDisplayName)
                                .map(Component::getString)
                                .orElse(""));
    }

    public Player getPlayer() {
        return player;
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isRenaming() {
        return currentAddress != null;
    }

    public boolean isSameAsCurrentAddress(String address) {
        return currentAddress != null && currentAddress.id().equals(address);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setAddressAndUpdateValidationState(ServerLevel level, String address) {
        setAddress(address);
        dataAddressValidation.set(validateAddress(address.trim()).id());
    }

    protected AddressValidation validateAddress(String address) {
        if (address.isBlank()) {
            return AddressValidation.ERR_EMPTY;
        } else if (address.length() > MAX_NAME_LENGTH) {
            return AddressValidation.ERR_TOO_LONG;
        } else if (!StringUtil.filterText(address).equals(address)) {
            return AddressValidation.ERR_CONTAINS_INVALID_CHARS;
        } else if (addresses.isKnown(address)) {
            if (!isSameAsCurrentAddress(address)) {
                return AddressValidation.ERR_TAKEN;
            }
            // This branch also skips xp check, if address is the same
        } else if (!player.isCreative() && player.experienceLevel < Config.Server.Pigeonhole.ADDRESS_EXPERIENCE_LEVELS_COST.get()) {
            return AddressValidation.ERR_NOT_ENOUGH_XP;
        }
        return AddressValidation.VALID;
    }

    public AddressValidation getValidationState() {
        return AddressValidation.of(dataAddressValidation.get());
    }

    public boolean canConfirm() {
        return getValidationState() == AddressValidation.VALID;
    }

    // --

    @Override
    public boolean clickMenuButton(Player player, int id) {
        BlockState state = player.level().getBlockState(pos);
        if (id == APPLY_BUTTON_ID && !player.level().isClientSide && state.getBlock() instanceof PigeonholeBlock pigeonhole) {
            String address = this.address.trim();
            AddressValidation validation = validateAddress(address);

            if (validation == AddressValidation.VALID) {
                pigeonhole.applyAddress(player, state, pos, hand, address);
            } else {
                player.displayClientMessage(validation.translate(), true);
                player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1, 1);
            }

            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof AddressTagItem
                && player.level().getBlockState(pos).getBlock() instanceof PigeonholeBlock
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0D;
    }

    // --

    public enum AddressValidation {
        UNKNOWN(0),
        VALID(1),
        ERR_EMPTY(2),
        ERR_TOO_LONG(3),
        ERR_CONTAINS_INVALID_CHARS(4),
        ERR_TAKEN(5),
        ERR_NOT_ENOUGH_XP(6);

        private final int id;

        AddressValidation(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static AddressValidation of(int id) {
            for (AddressValidation value : values()) {
                if (value.id == id) {
                    return value;
                }
            }
            return UNKNOWN;
        }

        public MutableComponent translate() {
            return Component.translatable("gui.envelope.pigeonhole_address.validation." + name().toLowerCase());
        }
    }
}
