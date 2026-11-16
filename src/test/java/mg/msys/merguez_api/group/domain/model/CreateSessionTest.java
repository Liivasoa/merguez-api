package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.user.domain.model.UserId;

class CreateSessionTest {

    @Test
    @DisplayName("A group member starts a scheduled session of their group")
    void groupMemberStartsScheduledSessionOfTheirGroup() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(member));
        var session = group.createSession(member, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        var occurrence = group.startSession(member, session);

        assertNotNull(occurrence);
        assertNotSame(session, occurrence);
        assertTrue(occurrence.isStarted());
        assertEquals(group.id(), occurrence.groupId());
        assertEquals(session.id(), occurrence.scheduleId());
    }

    @Test
    @DisplayName("Starting a scheduled session creates a timestamped occurrence without changing the schedule")
    void startingSessionCreatesTimestampedOccurrenceWithoutChangingSchedule() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(member));
        var session = group.createSession(member, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        var scheduledGroupId = session.groupId();
        var scheduledName = session.name();
        var scheduledDay = session.day();
        var scheduledTime = session.startTime();
        var scheduledId = session.id();

        var occurrence = group.startSession(member, session);

        assertNotSame(session, occurrence);
        assertEquals(scheduledId, occurrence.scheduleId());
        assertNotNull(occurrence.startedAt());
        assertEquals(scheduledGroupId, session.groupId());
        assertEquals(scheduledName, session.name());
        assertEquals(scheduledDay, session.day());
        assertEquals(scheduledTime, session.startTime());
        assertEquals(scheduledId, session.id());
        assertTrue(group.sessions().contains(session));
    }

    @Test
    @DisplayName("A group rejects a second started session and keeps the first as its only active occurrence")
    void groupRejectsSecondStartedSessionAndKeepsFirstAsOnlyActiveOccurrence() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(member));
        var firstSession = group.createSession(member, "Morning training", DayOfWeek.TUESDAY, LocalTime.of(9, 0));
        var secondSession =
                group.createSession(member, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(18, 0));
        var firstOccurrence = group.startSession(member, firstSession);

        assertThrows(IllegalStateException.class, () -> group.startSession(member, secondSession));

        assertEquals(List.of(firstOccurrence), group.activeOccurrences());
    }

    @Test
    @DisplayName("A group admin starts a scheduled session of their group")
    void groupAdminStartsScheduledSessionOfTheirGroup() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(member));
        var session = group.createSession(member, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        var occurrence = group.startSession(admin, session);

        assertNotNull(occurrence);
        assertNotSame(session, occurrence);
        assertTrue(occurrence.isStarted());
        assertEquals(group.id(), occurrence.groupId());
        assertEquals(session.id(), occurrence.scheduleId());
    }

    @Test
    @DisplayName("A group member creates a named session for their group")
    void groupMemberCreatesNamedSessionForTheirGroup() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", admin, List.of(member));

        var session = group.createSession(
                member, "Entraînement du soir", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        assertEquals(group.id(), session.groupId());
        assertEquals("Entraînement du soir", session.name());
    }

    @Test
    @DisplayName("A group admin creates a named session for their group")
    void groupAdminCreatesNamedSessionForTheirGroup() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", admin, List.of(member));

        var session = group.createSession(
                admin, "Entraînement du soir", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        assertEquals(group.id(), session.groupId());
        assertEquals("Entraînement du soir", session.name());
    }

    @Test
    @DisplayName("A session without a supplied name gets a generated name")
    void groupMemberCreatesSessionWithoutNameAndGetsGeneratedName() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", admin, List.of(member));

        var session = group.createSession(member, null, DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        var sessionWithDifferentDay =
                group.createSession(member, null, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0));
        var sessionWithDifferentTime =
                group.createSession(member, null, DayOfWeek.TUESDAY, LocalTime.of(15, 0));

        assertEquals(group.id(), session.groupId());
        assertNotNull(session.name());
        assertFalse(session.name().isBlank());
        assertNotNull(sessionWithDifferentDay.name());
        assertFalse(sessionWithDifferentDay.name().isBlank());
        assertNotNull(sessionWithDifferentTime.name());
        assertFalse(sessionWithDifferentTime.name().isBlank());
        assertNotEquals(session.name(), sessionWithDifferentDay.name());
        assertNotEquals(session.name(), sessionWithDifferentTime.name());
    }

    @Test
    @DisplayName("A session with an empty name gets a generated name based on its day and time")
    void groupMemberCreatesSessionWithEmptyNameAndGetsGeneratedName() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", admin, List.of(member));

        var session = group.createSession(member, "", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        var sessionWithDifferentDay =
                group.createSession(member, "", DayOfWeek.WEDNESDAY, LocalTime.of(14, 0));
        var sessionWithDifferentTime =
                group.createSession(member, "", DayOfWeek.TUESDAY, LocalTime.of(15, 0));

        assertEquals(group.id(), session.groupId());
        assertFalse(session.name().isEmpty());
        assertNotEquals(session.name(), sessionWithDifferentDay.name());
        assertNotEquals(session.name(), sessionWithDifferentTime.name());
    }

    @Test
    @DisplayName("Session creation is refused when the day is missing")
    void groupMemberCannotCreateSessionWithoutDay() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", admin, List.of(member));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> group.createSession(member, "Evening training", null, LocalTime.of(14, 0)));

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase(Locale.ROOT).contains("day"));
    }

    @Test
    @DisplayName("Session creation is refused when the start time is missing")
    void groupMemberCannotCreateSessionWithoutStartTime() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", admin, List.of(member));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> group.createSession(member, "Evening training", DayOfWeek.TUESDAY, null));

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase(Locale.ROOT).contains("time"));
    }

    @Test
    @DisplayName("A group rejects a second session with the same day and time regardless of name")
    void groupRejectsSecondSessionWithSameDayAndTimeEvenWhenNameDiffers() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", admin, List.of(member));
        group.createSession(member, "Morning training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        assertThrows(
                RuntimeException.class,
                () -> group.createSession(
                        member, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0)));
    }

    @Test
    @DisplayName("Rejecting a duplicate session leaves the original as the group's only session")
    void rejectingDuplicateSessionLeavesOriginalSessionUnchanged() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", admin, List.of(member));
        var originalSession =
                group.createSession(member, "Morning training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        assertThrows(
                IllegalStateException.class,
                () -> group.createSession(
                        member, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0)));

        assertEquals(List.of(originalSession), group.sessions());
    }

    @Test
    @DisplayName("Different groups can create sessions at the same day and time")
    void differentGroupsCanCreateSessionsAtSameDayAndTime() {
        UserId firstAdmin = new UserId(UUID.randomUUID());
        UserId firstMember = new UserId(UUID.randomUUID());
        Group firstGroup = Group.create("Dojo du mardi", firstAdmin, List.of(firstMember));
        var firstSession = firstGroup.createSession(
                firstMember, "First group training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        UserId secondAdmin = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        Group secondGroup = Group.create("Dojo du jeudi", secondAdmin, List.of(secondMember));
        var secondSession = secondGroup.createSession(
                secondMember, "Second group training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        assertNotEquals(firstGroup.id(), secondGroup.id());
        assertEquals(firstGroup.id(), firstSession.groupId());
        assertEquals(secondGroup.id(), secondSession.groupId());
        assertEquals("Second group training", secondSession.name());
        assertEquals(DayOfWeek.TUESDAY, secondSession.day());
        assertEquals(LocalTime.of(14, 0), secondSession.startTime());
    }

    @Test
    @DisplayName("A created session appears in its group's sessions")
    void createdSessionAppearsInGroupSessions() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", admin, List.of(member));
        var session = group.createSession(
                member, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        assertTrue(group.sessions().contains(session));
    }
}
