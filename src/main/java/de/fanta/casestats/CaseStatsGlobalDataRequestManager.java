package de.fanta.casestats;

import de.cubeside.connection.GlobalServer;
import de.fanta.casestats.globaldata.GlobalDataRequestManagerFabric;
import org.yaml.snakeyaml.Yaml;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                String yamlString = dataInputStream.readUTF();
                Yaml yaml = new Yaml();
                Map<String, Object> config = yaml.load(yamlString);
                return config.get("result");
            }
            default-> throw new AssertionError("unknown message type " + messageType);
        }
        return null;
    }
}
