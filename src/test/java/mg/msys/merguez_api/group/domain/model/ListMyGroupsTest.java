package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }
}
