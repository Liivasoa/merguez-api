package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
