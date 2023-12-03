package de.fanta.casestats;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.cubeside.connection.GlobalServer;
import de.fanta.casestats.globaldata.GlobalDataHelperFabric;
import de.fanta.casestats.globaldata.StringSerializable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CaseStatsGlobalDataHelper extends GlobalDataHelperFabric<CaseStatsMessageType> {

    public static final String CHANNEL = "CaseStats";

    public CaseStatsGlobalDataHelper() {
        super(CaseStatsMessageType.class, CHANNEL);
    }

    public void sendCaseData(byte[] data) {
        sendData(CHANNEL, data);
    }

    @Override
    protected void handleMessage(CaseStatsMessageType messageType, GlobalServer source, DataInputStream data) throws IOException {

    }

}
