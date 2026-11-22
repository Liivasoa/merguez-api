package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.group.domain.exception.DuplicateSessionScheduleException;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupMember;
import mg.msys.merguez_api.group.domain.model.Role;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.user.domain.model.UserId;

class GroupDocumentTest {

    @Test
    @DisplayName("A saved group document retains its session")
    void groupDocumentRetainsSessionWithGroupAndSchedule() {
        UserId creator = new UserId(UUID.randomUUID());
        UserId participant = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", "Weekly practice", creator, List.of(participant));
        var session = group.createSession(creator, null, DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        Group restored = GroupDocument.from(group).toGroup();

        assertAll(
                () -> assertEquals(group.id(), restored.id()),
                () -> assertEquals(group.name(), restored.name()),
                () -> assertEquals(group.description(), restored.description()),
                () -> assertEquals(
                        List.of(new GroupMember(creator, Role.ADMIN), new GroupMember(participant, Role.MEMBER)),
                        restored.members()),
                () -> assertEquals(List.of(session), restored.sessions()));
    }

    @Test
    @DisplayName("A scheduled session ID is persisted and restored")
    void groupDocumentPersistsAndRestoresScheduledSessionId() {
        UserId creator = new UserId(UUID.randomUUID());
        UserId participant = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", "Weekly practice", creator, List.of(participant));
        UUID sessionId = UUID.randomUUID();
        Session session = new Session(
                group.id(), sessionId, "Tuesday practice", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        Group groupWithSession = Group.restore(
                group.id(),
                group.name(),
                group.description().orElse(null),
                creator,
                List.of(participant),
                List.of(session));

        GroupDocument document = GroupDocument.from(groupWithSession);
        Group restored = document.toGroup();

        assertAll(
                () -> assertEquals(sessionId.toString(), document.sessions().getFirst().id()),
                () -> assertEquals(sessionId, restored.sessions().getFirst().id()));
    }

    @Test
    @DisplayName("A session time is unique within its group, not across groups")
    void groupDocumentKeepsSessionUniquenessScopedToGroup() {
        UserId creator = new UserId(UUID.randomUUID());
        UserId participant = new UserId(UUID.randomUUID());
        Group firstGroup = Group.create("Tuesday dojo", creator, List.of(participant));
        Group secondGroup = Group.create("Tuesday board games", creator, List.of(participant));
        var firstSession = firstGroup.createSession(
                creator, "Tuesday dojo practice", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        var secondSession = secondGroup.createSession(
                creator, "Tuesday board games", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        Group restoredFirstGroup = GroupDocument.from(firstGroup).toGroup();
        Group restoredSecondGroup = GroupDocument.from(secondGroup).toGroup();

        assertNotEquals(restoredFirstGroup.id(), restoredSecondGroup.id());
        assertThrows(
                DuplicateSessionScheduleException.class,
                () -> restoredFirstGroup.createSession(
                        creator, "Duplicate Tuesday dojo practice", DayOfWeek.TUESDAY, LocalTime.of(14, 0)));

        Group persistedFirstGroup = GroupDocument.from(restoredFirstGroup).toGroup();
        Group persistedSecondGroup = GroupDocument.from(restoredSecondGroup).toGroup();

        assertAll(
                () -> assertEquals(firstGroup.id(), persistedFirstGroup.id()),
                () -> assertEquals("Tuesday dojo", persistedFirstGroup.name()),
                () -> assertEquals(List.of(firstSession), persistedFirstGroup.sessions()),
                () -> assertEquals(secondGroup.id(), persistedSecondGroup.id()),
                () -> assertEquals("Tuesday board games", persistedSecondGroup.name()),
                () -> assertEquals(List.of(secondSession), persistedSecondGroup.sessions()));
    }
}
