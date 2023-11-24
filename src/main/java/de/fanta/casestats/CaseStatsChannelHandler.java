package de.fanta.casestats;

import de.fanta.casestats.data.CaseItem;
import de.fanta.casestats.data.CaseStat;
import de.fanta.cubeside.CubesideClientFabric;
import de.fanta.cubeside.util.ChatInfo;
import de.fanta.cubeside.util.FlashColorScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CaseStatsChannelHandler implements ClientPlayNetworking.PlayChannelHandler {

    public static final Identifier CHANNEL_IDENTIFIER = new Identifier("casestats", "data");

    public CaseStatsChannelHandler() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_IDENTIFIER, this);
    }

    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler networkHandler, PacketByteBuf packet, PacketSender sender) {
        int globalChatDataChannelID = 0;
        try {
            int caseStatsDateChannel = packet.readByte();
            int caseStatsDateChannelVersion = packet.readByte();
            if (caseStatsDateChannel == globalChatDataChannelID && caseStatsDateChannelVersion == 0) {

                String caseId = packet.readString();
                Item caseItem = Registries.ITEM.get(Identifier.tryParse(packet.readString()));
                MutableText caseName = Text.empty();
                try {
                    caseName = (MutableText) packet.readText();
                } catch (IndexOutOfBoundsException ignored) {
                }
                boolean caseEnchantGlint = packet.readBoolean();
                // TODO: Read head value

                // TODO: Check for existing stat

                ItemStack caseStack = new ItemStack(caseItem);
                caseStack.setCustomName(caseName);
                if (caseEnchantGlint) {
                    caseStack.addEnchantment(Enchantments.UNBREAKING, 0);
                }
                // TODO: Apply head value

                CaseStat stat = new CaseStat(caseId, caseStack);

                int slots = packet.readInt();
                for (int i = 0; i < slots; i++) {
                    Item item = Registries.ITEM.get(Identifier.tryParse(packet.readString()));
                    int amount = packet.readInt();
                    MutableText name = Text.empty();
                    try {
                        name = (MutableText) packet.readText();
                    } catch (IndexOutOfBoundsException ignored) {
                    }
                    boolean enchanted = packet.readBoolean();

                    CaseItem caseItem1 = new CaseItem(item, amount, name, enchanted);

                    // TODO: Read head value

                    ItemStack stack = new ItemStack(item);
                    stack.setCustomName(name);
                    if (enchanted) {
                        stack.addEnchantment(Enchantments.UNBREAKING, 0);
                    }
                    // TODO: Apply head value

                    stat.addItemOccurrence(caseItem1);
                }

            }

        } catch (Exception e) {
            CaseStats.LOGGER.warn("Unable to read CaseStats data", e);
        }
    }

    private void sendCaseInfoToPlayer() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player p = getPlayer();
            if (p == null || !isListeningToCaseStatsChannel()) {
                return;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MinecraftDataOutputStream dataOut = new MinecraftDataOutputStream(out);
            int caseStatsDateChannel = 0;
            int caseStatsDateChannelVersion = 0;
            String caseID = getId();
            String caseItem = getCaseItem().getType().getKey().toString();
            String caseName = getCaseItem().getItemMeta().getDisplayName();
            boolean caseHasEnchantment = getCaseItem().getItemMeta().hasEnchants();
            try {
                dataOut.writeByte(caseStatsDateChannel);
                dataOut.writeByte(caseStatsDateChannelVersion);
                dataOut.writeString(caseID);
                dataOut.writeString(caseItem);
                dataOut.writeString(caseName);
                dataOut.writeBoolean(caseHasEnchantment);
                dataOut.writeString(getOptionalHeadValue(getCaseItem()).orElse(""));

                dataOut.writeInt(getSlots().size());
                for (ItemStack stack : inv.getContents()) {
                    if (stack != null && !stack.getType().isAir()) {
                        String itemType = stack.getType().getKey().toString();
                        String itemName = stack.getItemMeta().getDisplayName();
                        boolean itemHasEnchantment = stack.getItemMeta().hasEnchants();
                        dataOut.writeString(itemType);
                        dataOut.writeString(itemName);
                        dataOut.writeBoolean(itemHasEnchantment);
                        dataOut.writeString(getOptionalHeadValue(stack).orElse(""));
                    }
                }

                dataOut.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            player.sendPluginMessage(plugin, plugin.getCaseStatsMod_ModChannel(), out.toByteArray());
        }, 1L);
    }
}

