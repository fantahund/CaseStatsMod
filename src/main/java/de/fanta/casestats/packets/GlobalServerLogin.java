package de.fanta.casestats.packets;

public record GlobalServerLogin(String uuid, String passwort, String ipString, int port) {
}
