package de.fanta.casestats;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.cubeside.connection.GlobalConnectionFabricClient;
import de.fanta.casestats.data.CaseItem;
import de.fanta.casestats.data.CaseStat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

public class CaseStatsChannelHandler implements ClientPlayNetworking.PlayChannelHandler {

    public static final Identifier CHANNEL_IDENTIFIER = new Identifier("casestats", "data");

    private final CaseStats caseStats;

    public CaseStatsChannelHandler(CaseStats caseStats) {
        this.caseStats = caseStats;
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_IDENTIFIER, this);
    }

    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler networkHandler, PacketByteBuf packet, PacketSender sender) {
        int caseOpenDataChannelID = 0;
        int loginGlobalClientChannelID = 1;
        try {
            int caseStatsDateChannel = packet.readByte();
            int caseStatsDateChannelVersion = packet.readByte();
            if (caseStatsDateChannel == caseOpenDataChannelID && caseStatsDateChannelVersion == 0) {
                String caseId = packet.readString();
                Item cItem = Registries.ITEM.get(Identifier.tryParse(packet.readString()));
                String caseItemString = packet.readString();

                CaseStat stat = caseStats.stats().caseStatOf(caseId).orElseGet(() -> {
                    ItemStack caseStack = createItemStack(cItem, 1, caseItemString);
                    CaseStat caseStat = new CaseStat(caseId, caseStack);
                    caseStats.stats().add(caseStat);
                    return caseStat;
                });
                //CaseStats.LOGGER.info("Got CaseStat: " + caseId + " = " + stat.icon() + " " + stat.icon().getNbt());

                int slots = packet.readInt();
                for (int i = 0; i < slots; i++) {
                    Item item = Registries.ITEM.get(Identifier.tryParse(packet.readString()));
                    int amount = packet.readInt();
                    String itemString = packet.readString();

                    ItemStack stack = createItemStack(item, amount, itemString);
                    CaseItem caseItem1 = new CaseItem(stack, item, amount, (MutableText) stack.getName(), stack.hasEnchantments());

                    stat.addItemOccurrence(client.player.getUuid(), caseItem1);
                }
            }

            if (caseStatsDateChannel == loginGlobalClientChannelID) {
                String uuid = packet.readString();
                String passwort = packet.readString();
                String ipString = packet.readString();
                int port = packet.readInt();
                CaseStats.LOGGER.info("UUID: " + uuid + " Passwort: " + passwort + " IP: " + ipString + " Port: " + port);
                GlobalConnectionFabricClient.getInstance().loginClientAndWriteConfig(ipString, port, uuid, passwort);
            }
        } catch (Exception e) {
            CaseStats.LOGGER.warn("Unable to read CaseStats data", e);
        }
    }

    private static ItemStack createItemStack(Item item, int amount, String itemString) {
        ItemStack stack = new ItemStack(item, amount);
        NbtCompound nbtCompound = null;
        try {
            nbtCompound = StringNbtReader.parse(itemString);
        } catch (CommandSyntaxException e) {
            CaseStats.LOGGER.error("nbtCompound could not be parse.", e);
        }

        if (nbtCompound != null) {
            stack.setNbt(nbtCompound);
        }
        return stack;
    }
}

