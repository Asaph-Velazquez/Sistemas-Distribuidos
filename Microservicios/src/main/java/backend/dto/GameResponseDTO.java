package backend.dto;

public class GameResponseDTO {
    private int port;
    private String gameName;
    private String hostName;
    private int maxPlayers;
    private int currentPlayers;

    // No-args constructor
    public GameResponseDTO() {
    }

    // All-args constructor
    public GameResponseDTO(int port, String gameName, String hostName, int maxPlayers, int currentPlayers) {
        this.port = port;
        this.gameName = gameName;
        this.hostName = hostName;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = currentPlayers;
    }

    // Getters and Setters
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }
}
