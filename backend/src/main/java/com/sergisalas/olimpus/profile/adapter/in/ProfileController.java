package com.sergisalas.olimpus.profile.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.profile.application.GetProfile;
import com.sergisalas.olimpus.profile.application.SaveProfile;
import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Intent;
import com.sergisalas.olimpus.profile.domain.InterestCatalog;
import com.sergisalas.olimpus.profile.domain.LanguageSkill;
import com.sergisalas.olimpus.profile.domain.Location;
import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.profile.domain.ProfileNotFoundException;
import com.sergisalas.olimpus.shared.adapter.Messages;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProfileController {

    /** What the phone sends when the sign-up is finished. */
    public record ProfileRequest(
            String nickname,
            String bio,
            LocalDate birthDate,
            Gender gender,
            Set<Gender> seeking,
            int ageMin,
            int ageMax,
            int maxDistanceKm,
            double latitude,
            double longitude,
            List<LanguageRequest> languages,
            int sociability,
            int conversationDepth,
            Intent intent,
            Set<String> interests) {}

    public record LanguageRequest(String code, LanguageSkill.Level level) {}

    public record ProfileResponse(
            String nickname,
            String bio,
            LocalDate birthDate,
            int age,
            Gender gender,
            Set<Gender> seeking,
            int ageMin,
            int ageMax,
            int maxDistanceKm,
            double latitude,
            double longitude,
            List<LanguageRequest> languages,
            int sociability,
            int conversationDepth,
            Intent intent,
            Set<String> interests) {}

    /** An interest with its label in the user's language, ready to show in the app. */
    public record InterestResponse(String name, String label) {}

    private final SaveProfile saveProfile;
    private final GetProfile getProfile;
    private final Messages messages;
    private final Clock clock;

    public ProfileController(
            SaveProfile saveProfile, GetProfile getProfile, Messages messages, Clock clock) {
        this.saveProfile = saveProfile;
        this.getProfile = getProfile;
        this.messages = messages;
        this.clock = clock;
    }

    @GetMapping("/interests")
    public List<InterestResponse> interests() {
        return InterestCatalog.ENTRIES.stream()
                .map(entry -> new InterestResponse(entry.name(), messages.interestLabel(entry.name())))
                .toList();
    }

    @GetMapping("/profile")
    public ProfileResponse profile(@CurrentAccount Account account) {
        return getProfile
                .execute(account.id())
                .map(this::toResponse)
                .orElseThrow(ProfileNotFoundException::new);
    }

    @PutMapping("/profile")
    public ProfileResponse save(@CurrentAccount Account account, @RequestBody ProfileRequest body) {
        Profile profile =
                new Profile(
                        account.id(),
                        body.nickname(),
                        body.bio(),
                        body.birthDate(),
                        body.gender(),
                        body.seeking(),
                        body.ageMin(),
                        body.ageMax(),
                        body.maxDistanceKm(),
                        // Always rounded: the backend does not store the exact point
                        // even if the phone sends it.
                        Location.rounded(body.latitude(), body.longitude()),
                        body.languages() == null
                                ? List.of()
                                : body.languages().stream()
                                        .map(l -> new LanguageSkill(l.code(), l.level()))
                                        .toList(),
                        body.sociability(),
                        body.conversationDepth(),
                        body.intent(),
                        body.interests());

        return toResponse(saveProfile.execute(profile));
    }

    private ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.nickname(),
                profile.bio(),
                profile.birthDate(),
                profile.ageOn(LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC)),
                profile.gender(),
                profile.seeking(),
                profile.ageMin(),
                profile.ageMax(),
                profile.maxDistanceKm(),
                profile.location().latitude(),
                profile.location().longitude(),
                profile.languages().stream()
                        .map(l -> new LanguageRequest(l.code(), l.level()))
                        .toList(),
                profile.sociability(),
                profile.conversationDepth(),
                profile.intent(),
                profile.interests());
    }
}
