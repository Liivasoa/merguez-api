package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.user.domain.model.UserId;

class RestoreGroupOccurrenceInvariantsTest {

    @Test
    @DisplayName("Restoration rejects an active occurrence from another group")
    void restorationRejectsActiveOccurrenceFromAnotherGroup() {
        Fixture fixture = new Fixture();
        SessionOccurrence foreignOccurrence =
                new SessionOccurrence(GroupId.random(), fixture.session().id(), Instant.now());

        assertThrows(IllegalArgumentException.class, () -> fixture.restore(foreignOccurrence, List.of()));
    }

    @Test
    @DisplayName("Restoration rejects an active occurrence without a restored scheduled session")
    void restorationRejectsActiveOccurrenceWithoutRestoredScheduledSession() {
        Fixture fixture = new Fixture();
        SessionOccurrence unscheduledOccurrence =
                new SessionOccurrence(fixture.groupId(), UUID.randomUUID(), Instant.now());

        assertThrows(IllegalArgumentException.class, () -> fixture.restore(unscheduledOccurrence, List.of()));
    }

    @Test
    @DisplayName("Restoration rejects rounds associated with another occurrence")
    void restorationRejectsRoundsAssociatedWithAnotherOccurrence() {
        Fixture fixture = new Fixture();
        SessionOccurrence anotherOccurrence =
                new SessionOccurrence(fixture.groupId(), UUID.randomUUID(), Instant.now());

        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.restore(
                        fixture.occurrence(),
                        List.of(fixture.round(anotherOccurrence, 1))));
    }

    @Test
    @DisplayName("Restoration rejects rounds that start at number two")
    void restorationRejectsRoundsThatStartAtNumberTwo() {
        Fixture fixture = new Fixture();

        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.restore(
                        fixture.occurrence(),
                        List.of(fixture.round(fixture.occurrence(), 2))));
    }

    @Test
    @DisplayName("Restoration rejects gaps in round numbering")
    void restorationRejectsGapsInRoundNumbering() {
        Fixture fixture = new Fixture();

        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.restore(
                        fixture.occurrence(),
                        List.of(
                                fixture.round(fixture.occurrence(), 1),
                                fixture.round(fixture.occurrence(), 3))));
    }

    @Test
    @DisplayName("Restoration rejects rounds that are out of numerical order")
    void restorationRejectsRoundsThatAreOutOfNumericalOrder() {
        Fixture fixture = new Fixture();

        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.restore(
                        fixture.occurrence(),
                        List.of(
                                fixture.round(fixture.occurrence(), 2),
                                fixture.round(fixture.occurrence(), 1))));
    }

    private static final class Fixture {
        private final GroupId groupId = GroupId.random();
        private final UserId admin = new UserId(UUID.randomUUID());
        private final UserId firstMember = new UserId(UUID.randomUUID());
        private final UserId secondMember = new UserId(UUID.randomUUID());
        private final Session session = new Session(
                groupId, UUID.randomUUID(), "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        private final SessionOccurrence occurrence =
                new SessionOccurrence(groupId, session.id(), Instant.now());

        private Group restore(SessionOccurrence activeOccurrence, List<Round> rounds) {
            return Group.restore(
                    groupId,
                    "Tuesday dojo",
                    null,
                    admin,
                    List.of(firstMember, secondMember),
                    List.of(session),
                    activeOccurrence,
                    rounds);
        }

        private Round round(SessionOccurrence roundOccurrence, int number) {
            return new Round(
                    roundOccurrence,
                    number,
                    List.of(new Combat(List.of(firstMember, secondMember), Optional.empty())));
        }

        private GroupId groupId() {
            return groupId;
        }

        private Session session() {
            return session;
        }

        private SessionOccurrence occurrence() {
            return occurrence;
        }
    }
}
