package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.user.domain.model.UserId;

class CreateSessionTest {

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
}
