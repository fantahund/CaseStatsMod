package de.fanta.casestats;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.cubeside.connection.GlobalServer;
import de.fanta.casestats.data.CaseItem;
import de.fanta.casestats.data.CaseStat;
import de.fanta.casestats.data.PlayerCaseItemStat;
import de.fanta.casestats.globaldata.GlobalDataRequestManagerFabric;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseStatsGlobalDataRequestManager extends GlobalDataRequestManagerFabric<CaseStatsGlobalDataRequestType> {

    public static final String CHANNEL = "CaseStats-Requests";

    public CaseStatsGlobalDataRequestManager() {
        super(CaseStatsGlobalDataRequestType.class, CHANNEL);
    }

    @Override
    protected void respondToRequest(CaseStatsGlobalDataRequestType messageType, GlobalServer globalServer, DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
        /*UUID playerUUID = readUUID(dataInputStream);
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || item.getAmount() < 1) {
            this.sendMsgParts(dataOutputStream, 0);
            return;
        }*/

        /*switch (messageType) {
            case GET_ITEM_INFO:
                List<String> itemInfo = List.of("Hallo", "fanta");
                Object[] message = new Object[itemInfo.size() + 1];
                message[0] = itemInfo.size();
                System.arraycopy(itemInfo.toArray(), 0, message, 1, itemInfo.size());
                this.sendMsgParts(dataOutputStream, message);
                break;
        }*/
    }

    @Override
    protected Object handleResponse(CaseStatsGlobalDataRequestType messageType, GlobalServer globalServer, DataInputStream dataInputStream) throws IOException {
        switch (messageType) {
            case GET_CASE_STATS-> {
                List<PlayerCaseItemStat> playerCaseItemStats = new ArrayList<>();
                int size = dataInputStream.readInt();
                for (int i = 0; i < size; i++) {
                    String uuid = dataInputStream.readUTF();
                    String itemId = dataInputStream.readUTF();
                    Item item = Registries.ITEM.get(Identifier.tryParse(dataInputStream.readUTF()));
                    String itemNBT = dataInputStream.readUTF();
                    int amount = dataInputStream.readInt();
                    int count = dataInputStream.readInt();
                    CaseItem caseItem = new CaseItem(itemId, createItemStack(item, amount, itemNBT));
                    playerCaseItemStats.add(new PlayerCaseItemStat(UUID.fromString(uuid), caseItem, count));
                }
                return playerCaseItemStats;
            }
            case GET_CASES -> {
                int size = dataInputStream.readInt();
                List<CaseStat> caseStats = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    String id = dataInputStream.readUTF();

                    Item item = Registries.ITEM.get(Identifier.tryParse(dataInputStream.readUTF()));
                    String itemNBT = dataInputStream.readUTF();

                    CaseStat caseStat = new CaseStat(id, createItemStack(item, 1, itemNBT));
                    caseStats.add(caseStat);
                }
                return caseStats;
            }
            default-> throw new AssertionError("unknown message type " + messageType);
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
