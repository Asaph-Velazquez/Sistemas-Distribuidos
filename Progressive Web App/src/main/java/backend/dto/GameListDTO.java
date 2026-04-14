package backend.dto;

import java.util.List;

public class GameListDTO {
    private List<GameResponseDTO> games;

    // No-args constructor
    public GameListDTO() {
    }

    // All-args constructor
    public GameListDTO(List<GameResponseDTO> games) {
        this.games = games;
    }

    // Getters and Setters
    public List<GameResponseDTO> getGames() {
        return games;
    }

    public void setGames(List<GameResponseDTO> games) {
        this.games = games;
    }
}
