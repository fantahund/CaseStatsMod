package de.fanta.casestats;

import de.cubeside.connection.GlobalServer;
import de.fanta.casestats.globaldata.GlobalDataRequestManagerFabric;

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
        }

        switch (messageType) {
            case GET_ITEM_INFO:
                List<String> itemInfo = GlobalItemInfoProvider.getItemInfo(item);
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
            case GET_ITEM_INFO:
                int count = dataInputStream.readInt();
                List<String> itemInfo = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    itemInfo.add(dataInputStream.readUTF());
                }
                return itemInfo;
        }
        return null;
    }
}
