package de.fanta.casestats.globaldata;

import de.cubeside.connection.GlobalServer;

import java.util.concurrent.Future;

public interface GlobalDataRequestManager<T extends Enum<T>> {

    public <V> Future<V> makeRequest(T requestType, GlobalServer server, Object... data);

}