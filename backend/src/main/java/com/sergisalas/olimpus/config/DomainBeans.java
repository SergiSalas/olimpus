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
import com.sergisalas.olimpus.matching.application.GetTodaysConversation;
import com.sergisalas.olimpus.matching.application.RunDailyRound;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.MatchContextFactory;
import com.sergisalas.olimpus.matching.domain.ProfileDirectory;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.profile.application.GetProfile;
import com.sergisalas.olimpus.profile.application.SaveProfile;
import com.sergisalas.olimpus.profile.domain.ProfileRepository;
import com.sergisalas.olimpus.shared.domain.Hasher;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aqui se montan a mano los casos de uso. Es el unico sitio donde Spring
 * y el dominio se tocan: asi el dominio sigue sin depender de nada.
 */
@Configuration
public class DomainBeans {

    /** Todo el proyecto trabaja en UTC. Un solo reloj, y en los tests se sustituye. */
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

    /** La zona de la comunidad. Con ella se calculan las 4:00, las 14:00 y las 22:00. */
    @Bean
    RoundSchedule roundSchedule(@Value("${olimpus.zona:Europe/Madrid}") String zona) {
        return RoundSchedule.of(ZoneId.of(zona));
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
            ProfileDirectory profiles,
            RoundSchedule schedule,
            Clock clock) {
        return new GetTodaysConversation(conversations, profiles, schedule, clock);
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
