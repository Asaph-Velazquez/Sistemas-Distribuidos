package com.profile.service;

import com.profile.entity.UserProfile;
import com.profile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    private final UserProfileRepository profileRepository;

    public ProfileService(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public UserProfile createProfile(Long userId, String displayName) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setDisplayName(displayName);
        profile.setAvatar("default");
        return profileRepository.save(profile);
    }

    public UserProfile getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
            .orElseGet(() -> createProfile(userId, "Player-" + userId));
    }

    public UserProfile updateProfile(Long userId, String displayName, String avatar) {
        UserProfile profile = profileRepository.findByUserId(userId)
            .orElseGet(() -> createProfile(userId, displayName));
        if (displayName != null) profile.setDisplayName(displayName);
        if (avatar != null) profile.setAvatar(avatar);
        return profileRepository.save(profile);
    }

    public void updateStats(Long userId, boolean win) {
        UserProfile profile = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found"));
        profile.setTotalGames(profile.getTotalGames() + 1);
        if (win) profile.setWins(profile.getWins() + 1);
        else profile.setLosses(profile.getLosses() + 1);
        profileRepository.save(profile);
    }
}