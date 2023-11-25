package de.fanta.casestats;

import de.fanta.casestats.data.CaseItem;
import de.fanta.casestats.data.CaseStat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
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
import java.util.UUID;

public class CaseStatsChannelHandler implements ClientPlayNetworking.PlayChannelHandler {

    public static final Identifier CHANNEL_IDENTIFIER = new Identifier("casestats", "data");

    private CaseStats caseStats;

    public CaseStatsChannelHandler(CaseStats caseStats) {
        this.caseStats = caseStats;
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
                Item cItem = Registries.ITEM.get(Identifier.tryParse(packet.readString()));
                final MutableText caseName = readText(packet);
                boolean caseEnchantGlint = packet.readBoolean();
                String caseHeadTex = packet.readString();

                CaseStat stat = caseStats.stats().caseStatOf(caseId).orElseGet(() -> {
                    ItemStack caseStack = createItemStack(cItem, 1, caseName, caseEnchantGlint, caseHeadTex);
                    CaseStat caseStat = new CaseStat(caseId, caseStack);
                    caseStats.stats().add(caseStat);
                    return caseStat;
                });
                CaseStats.LOGGER.info("Got CaseStat: " + caseId + " = " + stat.icon() + " " + stat.icon().getNbt());

                int slots = packet.readInt();
                for (int i = 0; i < slots; i++) {
                    Item item = Registries.ITEM.get(Identifier.tryParse(packet.readString()));
                    int amount = packet.readInt();
                    MutableText name = readText(packet);
                    boolean enchanted = packet.readBoolean();

                    String headTex = packet.readString();
                    ItemStack stack = createItemStack(item, amount, name, enchanted, headTex);
                    CaseItem caseItem1 = new CaseItem(stack, item, amount, name, enchanted);

                    CaseStats.LOGGER.info("  - " + stack + " " + stack.getNbt());
                    stat.addItemOccurrence(client.player.getUuid(), caseItem1);
                }

            }
        } catch (Exception e) {
            CaseStats.LOGGER.warn("Unable to read CaseStats data", e);
        }
    }

    private static MutableText readText(PacketByteBuf packet) {
        try {
            return (MutableText) packet.readText();
        } catch (IndexOutOfBoundsException ignored) {
        }
        return Text.empty();
    }

    private static ItemStack createItemStack(Item item, int amount, Text name, boolean enchantGlint, String headTexture) {
        ItemStack stack = new ItemStack(item, amount);
        if (headTexture != null && !headTexture.isEmpty() && !headTexture.isBlank()) {
            stack.setNbt(nbtFromTextureValue(headTexture));
        }
        stack.setCustomName(name);
        if (enchantGlint) {
            stack.addEnchantment(Enchantments.UNBREAKING, 0);
        }
        return stack;
    }

    public static NbtCompound nbtFromTextureValue(String texturevalue) {
        NbtCompound nbtCompound = new NbtCompound();
        NbtCompound skullownertag = new NbtCompound();
        NbtCompound texturetag = new NbtCompound();
        NbtList texturelist = new NbtList();
        NbtCompound valuetag = new NbtCompound();

        valuetag.putString("Value", texturevalue);
        texturelist.add(valuetag);
        texturetag.put("textures", texturelist);
        skullownertag.put("Properties", texturetag);
        skullownertag.putUuid("Id", UUID.randomUUID());
        nbtCompound.put("SkullOwner", skullownertag);

        return nbtCompound;
    }

}

