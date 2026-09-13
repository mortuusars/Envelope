package io.github.mortuusars.envelope.world.mail.service.cloud_depository;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.PersistentData;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class CloudDepositoryData extends PersistentData {
    public static final Codec<CloudDepositoryData> CODEC = RecordCodecBuilder.create(i -> i.group(
          Codec.unboundedMap(Codec.STRING, Account.CODEC)
                .optionalFieldOf("accounts", Map.of())
                .forGetter(CloudDepositoryData::accounts)
    ).apply(i, CloudDepositoryData::new));

    public static final Type<CloudDepositoryData> TYPE = new Type<>(ServiceAddress.CLOUD_DEPOSITORY.location(), CloudDepositoryData::new, CODEC);

    private final Map<String, Account> accounts;

    public CloudDepositoryData(Map<String, Account> accounts) {
        this.accounts = new HashMap<>(accounts);
    }

    public CloudDepositoryData() {
        this(Map.of());
    }

    public Map<String, Account> accounts() {
        return accounts;
    }

    public Account ofAccount(String account) {
        return accounts().computeIfAbsent(account, c -> new CloudDepositoryData.Account(
              Config.Server.SERVICE_CLOUD_DEPOSITORY_ACCOUNT_STORAGE_STARTING_CAPACITY.get(),
              Collections.emptyList()));
    }

    public static class Account {
        public static final Codec<Account> CODEC = RecordCodecBuilder.create(i -> i.group(
              Codec.INT.optionalFieldOf("capacity", 8).forGetter(Account::getCapacity),
              ItemStack.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(Account::getItems)
        ).apply(i, Account::new));

        protected int capacity = 8;
        protected List<ItemStack> items;

        public Account(int capacity, List<ItemStack> items) {
            setCapacity(capacity);
            this.items = new ArrayList<>(items);
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            Preconditions.checkArgument(capacity > 0, "Capacity must be larger than 0. Got: " + capacity);
            this.capacity = capacity;
        }

        public List<ItemStack> getItems() {
            return items;
        }

        public int getTotal() {
            return getItems().size();
        }

        @Override
        public String toString() {
            return "{items=" + items + '}';
        }
    }
}
