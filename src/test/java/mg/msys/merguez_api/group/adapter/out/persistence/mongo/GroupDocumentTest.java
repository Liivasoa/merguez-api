package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupMember;
import mg.msys.merguez_api.group.domain.model.Role;
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
}
