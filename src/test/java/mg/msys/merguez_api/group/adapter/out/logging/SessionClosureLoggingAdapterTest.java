package mg.msys.merguez_api.group.adapter.out.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionClosureResult;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

class SessionClosureLoggingAdapterTest {

    @Test
    void logsOneClosureMessageForEachMemberIncludingZeroScore() {
        UserId winner = new UserId(UUID.randomUUID());
        UserId loser = new UserId(UUID.randomUUID());
        UserId inactive = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", winner, List.of(loser, inactive));
        Session session = group.createSession(winner, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(winner, session);
        Combat combat = new Combat(List.of(winner, loser), Optional.empty());
        Round round = group.addRound(winner, occurrence, List.of(combat));
        group.closeRound(winner, round, List.of(new Combat(combat.participants(), Optional.of(winner))));
        SessionClosureResult result = group.closeSession(winner, occurrence);
        assertEquals(Map.of(winner, 1, loser, -1, inactive, 0), result.netScores());

        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(LogSessionClosurePort.class));
        try (var context = new AnnotationConfigApplicationContext()) {
            int index = 0;
            for (var candidate : scanner.findCandidateComponents("mg.msys.merguez_api.group.adapter.out")) {
                context.registerBeanDefinition("closureLogger" + index++, candidate);
            }
            context.refresh();
            var adapters = context.getBeansOfType(LogSessionClosurePort.class);
            assertEquals(1, adapters.size(), "A production closure logging adapter must be available");
            LogSessionClosurePort adapter = adapters.values().iterator().next();

            Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            Level previousLevel = root.getLevel();
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            root.addAppender(appender);
            root.setLevel(Level.INFO);
            try {
                result.netScores().keySet().forEach(member -> adapter.log(member, result));

                List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
                assertEquals(result.netScores().size(), messages.size());
                for (UserId member : result.netScores().keySet()) {
                    List<String> memberMessages = messages.stream()
                            .filter(message -> message.contains(member.value().toString()))
                            .toList();
                    assertEquals(1, memberMessages.size(), "Expected one log for " + member);
                    String message = memberMessages.getFirst().toLowerCase(Locale.ROOT);
                    assertTrue(message.contains("session"), "The log must identify a session closure");
                    assertTrue(message.contains("clos"), "The log must describe the closure");
                }
            } finally {
                root.detachAppender(appender);
                appender.stop();
                root.setLevel(previousLevel);
            }
        }
    }
}
