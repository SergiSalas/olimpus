package com.sergisalas.olimpus.config;

import com.sergisalas.olimpus.auth.application.AuthenticateSession;
import com.sergisalas.olimpus.auth.application.RequestLoginCode;
import com.sergisalas.olimpus.auth.application.VerifyLoginCode;
import com.sergisalas.olimpus.auth.domain.AccountRepository;
import com.sergisalas.olimpus.auth.domain.CodeSender;
import com.sergisalas.olimpus.auth.domain.LoginCodeRepository;
import com.sergisalas.olimpus.auth.domain.Secrets;
import com.sergisalas.olimpus.auth.domain.SessionRepository;
import com.sergisalas.olimpus.chat.application.CloseFinishedConversations;
import com.sergisalas.olimpus.chat.application.GetChat;
import com.sergisalas.olimpus.chat.application.SendMessage;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.health.application.CheckHealth;
import com.sergisalas.olimpus.health.domain.DatabaseInfo;
import com.sergisalas.olimpus.matching.application.AskToSeePhoto;
import com.sergisalas.olimpus.matching.application.GetTodaysConversation;
import com.sergisalas.olimpus.matching.application.ViewPartnerPhoto;
import com.sergisalas.olimpus.matching.application.RunDailyRound;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.MatchContextFactory;
import com.sergisalas.olimpus.matching.domain.ProfileDirectory;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.profile.application.GetProfile;
import com.sergisalas.olimpus.profile.application.SaveProfile;
import com.sergisalas.olimpus.profile.application.UploadPhoto;
import com.sergisalas.olimpus.profile.application.ViewPhoto;
import com.sergisalas.olimpus.profile.domain.PhotoModerator;
import com.sergisalas.olimpus.profile.domain.PhotoRepository;
import com.sergisalas.olimpus.profile.domain.PhotoStorage;
import com.sergisalas.olimpus.profile.domain.ProfileRepository;
import com.sergisalas.olimpus.shared.domain.Hasher;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Use cases are wired by hand here. It is the only place where Spring and the
 * domain touch: that way the domain keeps depending on nothing.
 */
@Configuration
public class DomainBeans {

    /** The whole project works in UTC. One single clock, replaced in tests. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CheckHealth checkHealth(DatabaseInfo databaseInfo) {
        return new CheckHealth(databaseInfo);
    }

    @Bean
    RequestLoginCode requestLoginCode(
            LoginCodeRepository codes,
            Secrets secrets,
            Hasher hasher,
            CodeSender sender,
            Clock clock) {
        return new RequestLoginCode(codes, secrets, hasher, sender, clock);
    }

    @Bean
    VerifyLoginCode verifyLoginCode(
            LoginCodeRepository codes,
            AccountRepository accounts,
            SessionRepository sessions,
            Secrets secrets,
            Hasher hasher,
            Clock clock) {
        return new VerifyLoginCode(codes, accounts, sessions, secrets, hasher, clock);
    }

    @Bean
    SaveProfile saveProfile(ProfileRepository profiles, Clock clock) {
        return new SaveProfile(profiles, clock);
    }

    @Bean
    GetProfile getProfile(ProfileRepository profiles) {
        return new GetProfile(profiles);
    }

    @Bean
    UploadPhoto uploadPhoto(
            PhotoRepository photos, PhotoStorage storage, PhotoModerator moderator, Clock clock) {
        return new UploadPhoto(photos, storage, moderator, clock);
    }

    @Bean
    ViewPhoto viewPhoto(PhotoRepository photos, PhotoStorage storage) {
        return new ViewPhoto(photos, storage);
    }

    @Bean
    AskToSeePhoto askToSeePhoto(
            ConversationRepository conversations, MessageRepository messages, Clock clock) {
        return new AskToSeePhoto(conversations, messages, clock);
    }

    @Bean
    ViewPartnerPhoto viewPartnerPhoto(
            ConversationRepository conversations,
            MessageRepository messages,
            ViewPhoto photos,
            Clock clock) {
        return new ViewPartnerPhoto(conversations, messages, photos, clock);
    }

    /** The community's time zone. The 4:00, 14:00 and 22:00 are computed in it. */
    @Bean
    RoundSchedule roundSchedule(@Value("${olimpus.zone:Europe/Madrid}") String zone) {
        return RoundSchedule.of(ZoneId.of(zone));
    }

    @Bean
    RunDailyRound runDailyRound(
            ProfileDirectory profiles,
            ConversationRepository conversations,
            MatchContextFactory contexts,
            RoundSchedule schedule) {
        return new RunDailyRound(profiles, conversations, contexts, schedule);
    }

    @Bean
    GetTodaysConversation getTodaysConversation(
            ConversationRepository conversations,
            MessageRepository messages,
            ProfileDirectory profiles,
            RoundSchedule schedule,
            Clock clock) {
        return new GetTodaysConversation(conversations, messages, profiles, schedule, clock);
    }

    @Bean
    SendMessage sendMessage(
            ConversationRepository conversations, MessageRepository messages, Clock clock) {
        return new SendMessage(conversations, messages, clock);
    }

    @Bean
    GetChat getChat(
            ConversationRepository conversations,
            MessageRepository messages,
            ProfileDirectory profiles,
            RoundSchedule schedule,
            Clock clock) {
        return new GetChat(conversations, messages, profiles, schedule, clock);
    }

    @Bean
    CloseFinishedConversations closeFinishedConversations(
            ConversationRepository conversations, RoundSchedule schedule, Clock clock) {
        return new CloseFinishedConversations(conversations, schedule, clock);
    }

    @Bean
    AuthenticateSession authenticateSession(
            SessionRepository sessions, AccountRepository accounts, Hasher hasher, Clock clock) {
        return new AuthenticateSession(sessions, accounts, hasher, clock);
    }
}
