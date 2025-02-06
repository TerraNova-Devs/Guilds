package de.mcterranova.guilds.common.config;

public class DatabaseConfig {
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    public DatabaseConfig(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public String getUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&characterEncoding=UTF-8";
    }

    public String getUser() {return user;}
    public String getPassword() {return password;}
}