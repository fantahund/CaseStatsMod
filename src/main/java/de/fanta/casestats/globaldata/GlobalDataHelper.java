package de.fanta.casestats.globaldata;

import de.cubeside.connection.ConnectionAPI;
import de.cubeside.connection.GlobalPlayer;
import de.cubeside.connection.GlobalServer;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface GlobalDataHelper<T extends Enum<T>> extends ConnectionAPI {

    public String getChannel();

    public String getThisServerName();

    public boolean isReal(GlobalServer server);

    public boolean isReal(String serverName);

    public List<GlobalServer> getServers(UUID playerId);

    public List<GlobalServer> getServers(UUID playerId, boolean includeNonReals);

    public List<GlobalServer> getServers(String playerName);

    public List<GlobalServer> getServers(String playerName, boolean includeNonReals);

    public List<GlobalServer> getServers(GlobalPlayer gPlayer);

    public List<GlobalServer> getServers(GlobalPlayer gPlayer, boolean includeNonReals);

    public boolean isOnAnyServer(UUID playerId);

    public boolean isOnAnyServer(UUID playerId, boolean includeNonReals);

    public boolean isOnAnyServer(String playerName);

    public boolean isOnAnyServer(String playerName, boolean includeNonReals);

    public boolean isOnAnyServer(GlobalPlayer gPlayer);

    public boolean isOnAnyServer(GlobalPlayer gPlayer, boolean includeNonReals);

    public Collection<GlobalPlayer> getOnlinePlayers();

    public Collection<GlobalPlayer> getOnlinePlayers(boolean includeNonReals);

    public Set<String> getOnlinePlayerNames();

    public Set<String> getOnlinePlayerNames(boolean includeNonReals);

    // Equivalent to broadcastData(true, messageType, data);
    public void sendData(T messageType, Object... data);

    public void sendData(boolean sendToRestricted, T messageType, Object... data);

    public void sendData(GlobalServer server, T messageType, Object... data);

    public void sendData(Collection<GlobalServer> servers, T messageType, Object... data);

}