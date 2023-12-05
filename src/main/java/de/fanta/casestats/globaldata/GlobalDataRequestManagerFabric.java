package de.fanta.casestats.globaldata;

import de.cubeside.connection.GlobalServer;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class GlobalDataRequestManagerFabric<T extends Enum<T>> extends GlobalDataRequestManagerImpl<T> {

    private static <T extends Enum<T>> Pair<GlobalDataHelperImpl<T>, Delegator<T>> createHelper(Class<T> messageTypeClass, String channel) {
        Delegator<T> delegator = new Delegator<>();
        GlobalDataHelperFabric<T> helper = new GlobalDataHelperFabric<>(messageTypeClass, channel) {

            @Override
            protected void handleMessage(T messageType, GlobalServer source, DataInputStream data) throws IOException {
                delegator.handleMessage(messageType, source, data);
            }

        };

        return new Pair<>(helper, delegator);
    }

    public GlobalDataRequestManagerFabric(Class<T> messageTypeClass, String channel) {
        super(createHelper(messageTypeClass, channel));
    }

    @Override
    protected GlobalDataHelperFabric<T> getHelper() {
        return (GlobalDataHelperFabric<T>) super.getHelper();
    }
}
