package backend.dto;

public class JoinGameRequestDTO {
    private String playerName;

    // No-args constructor
    public JoinGameRequestDTO() {
    }

    // All-args constructor
    public JoinGameRequestDTO(String playerName) {
        this.playerName = playerName;
    }

    // Getters and Setters
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
