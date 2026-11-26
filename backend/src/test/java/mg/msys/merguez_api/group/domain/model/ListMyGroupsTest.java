package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.user.domain.model.UserId;

class ListMyGroupsTest {

    @Test
    void includesGroupsWhereUserIsMemberAndExcludesUnrelatedGroups() {
        UserId adminA = userId();
        UserId adminB = userId();
        UserId adminC = userId();
        UserId member = userId();
        Group firstGroup = Group.create("First", adminA, List.of(member));
        Group secondGroup = Group.create("Second", adminB, List.of(member));
        Group unrelatedGroup = Group.create("Unrelated", adminC, List.of(userId()));

        assertTrue(firstGroup.summaryFor(member).isPresent());
        assertTrue(secondGroup.summaryFor(member).isPresent());
        assertFalse(unrelatedGroup.summaryFor(member).isPresent());
    }

    @Test
    void includesGroupWhereUserIsAdmin() {
        UserId admin = userId();
        Group group = Group.create("Admin group", admin, List.of(userId()));

        assertTrue(group.summaryFor(admin).isPresent());
        assertTrue(group.summaryFor(admin).orElseThrow().admin());
    }

    @Test
    void providesRequestedGroupSummaryDetails() {
        UserId admin = userId();
        Group group = Group.create("Weekend group", "A group description", admin,
                List.of(userId(), userId(), userId()));
        group.createSession(admin, "Saturday", DayOfWeek.SATURDAY, LocalTime.of(10, 0));
        group.createSession(admin, "Sunday", DayOfWeek.SUNDAY, LocalTime.of(10, 0));

        GroupSummary summary = group.summaryFor(admin).orElseThrow();

        assertEquals("Weekend group", summary.name());
        assertEquals(Optional.of("A group description"), summary.description());
        assertEquals(4, summary.memberCount());
        assertEquals(2, summary.scheduledSessionCount());
        assertTrue(summary.admin());
    }

    @Test
    void returnsNoSummaryForUserWithoutGroupAssociation() {
        UserId user = userId();
        List<Group> groups = List.of(
                Group.create("First", userId(), List.of(userId())),
                Group.create("Second", userId(), List.of(userId())));

        List<GroupSummary> summaries = groups.stream()
                .map(group -> group.summaryFor(user))
                .flatMap(Optional::stream)
                .toList();

        assertTrue(summaries.isEmpty());
    }

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }
}
