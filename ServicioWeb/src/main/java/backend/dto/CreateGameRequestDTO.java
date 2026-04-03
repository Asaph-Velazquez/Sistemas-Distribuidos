package backend.dto;

public class CreateGameRequestDTO {
    private String username;
    private String gameName;
    private int maxPlayers;

    // No-args constructor
    public CreateGameRequestDTO() {
    }

    // All-args constructor
    public CreateGameRequestDTO(String username, String gameName, int maxPlayers) {
        this.username = username;
        this.gameName = gameName;
        this.maxPlayers = maxPlayers;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}
