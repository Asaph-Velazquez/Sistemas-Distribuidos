package com.profile.controller;

import com.profile.dto.ProfileDTO;
import com.profile.entity.UserProfile;
import com.profile.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileDTO> getProfile(@PathVariable Long userId) {
        UserProfile profile = profileService.getProfile(userId);
        return ResponseEntity.ok(new ProfileDTO(
            profile.getUserId(),
            profile.getDisplayName(),
            profile.getAvatar(),
            profile.getTotalGames(),
            profile.getWins(),
            profile.getLosses()
        ));
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ProfileDTO> updateProfile(
            @PathVariable Long userId,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String avatar) {
        UserProfile profile = profileService.updateProfile(userId, displayName, avatar);
        return ResponseEntity.ok(new ProfileDTO(
            profile.getUserId(),
            profile.getDisplayName(),
            profile.getAvatar(),
            profile.getTotalGames(),
            profile.getWins(),
            profile.getLosses()
        ));
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<ProfileDTO> getStats(@PathVariable Long userId) {
        UserProfile profile = profileService.getProfile(userId);
        return ResponseEntity.ok(new ProfileDTO(
            profile.getUserId(),
            profile.getDisplayName(),
            profile.getAvatar(),
            profile.getTotalGames(),
            profile.getWins(),
            profile.getLosses()
        ));
    }
}