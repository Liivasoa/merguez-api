package mg.msys.merguez_api.domain.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import mg.msys.merguez_api.domain.user.UserId;

class CreateGroupTest {

    @Test
    @DisplayName("A user creates a group with a name and a participant")
    void userCreatesGroupWithNameAndParticipant() {
        // Given a user "Alice"
        UserId alice = userId();
        // And another user "Bob"
        UserId bob = userId();

        // When Alice creates a group named "Dojo du mardi" with Bob as a participant
        Group group = Group.create("Dojo du mardi", alice, List.of(bob));

        // Then the group "Dojo du mardi" is created
        assertEquals("Dojo du mardi", group.name());
    }

    @Test
    @DisplayName("A group is created with a description")
    void groupIsCreatedWithDescription() {
        // Given a user "Alice"
        UserId alice = userId();
        // And another user "Bob"
        UserId bob = userId();

        // When Alice creates a group named "Dojo du mardi" with the description "Séances entre amis" and Bob as a participant
        Group group = Group.create("Dojo du mardi", "Séances entre amis", alice, List.of(bob));

        // Then the group is created
        assertEquals("Dojo du mardi", group.name());
        // And the group description is "Séances entre amis"
        assertEquals(Optional.of("Séances entre amis"), group.description());
    }

    @Test
    @DisplayName("A group is created without a description")
    void groupIsCreatedWithoutDescription() {
        // Given a user "Alice"
        UserId alice = userId();
        // And another user "Bob"
        UserId bob = userId();

        // When Alice creates a group named "Dojo du mardi" without a description and with Bob as a participant
        Group group = Group.create("Dojo du mardi", alice, List.of(bob));

        // Then the group is created
        assertEquals("Dojo du mardi", group.name());
        // And the group has no description
        assertEquals(Optional.empty(), group.description());
    }

    @Test
    @DisplayName("Group creation is refused when the name is missing")
    void groupCreationIsRefusedWhenNameIsMissing() {
        // Given a user "Alice"
        UserId alice = userId();
        // And another user "Bob"
        UserId bob = userId();

        // When Alice creates a group without a name and with Bob as a participant
        // Then the creation is refused because the name is required
        // And no group is created
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Group.create(null, alice, List.of(bob)));
        assertEquals("Group name is required", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("Group creation is refused when the name is empty or blank")
    void groupCreationIsRefusedWhenNameIsEmptyOrBlank(String blankName) {
        // Given a user "Alice"
        UserId alice = userId();
        // And another user "Bob"
        UserId bob = userId();

        // When Alice creates a group whose name is empty or made only of spaces and with Bob as a participant
        // Then the creation is refused because the name is required
        // And no group is created
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Group.create(blankName, alice, List.of(bob)));
        assertEquals("Group name is required", exception.getMessage());
    }

    @Test
    @DisplayName("Group creation is refused when no participant is added")
    void groupCreationIsRefusedWhenNoParticipantIsAdded() {
        // Given a user "Alice"
        UserId alice = userId();

        // When Alice creates a group named "Dojo du mardi" without any participant
        // Then the creation is refused because at least one participant other than the creator is required
        // And no group is created
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Group.create("Dojo du mardi", alice, List.of()));
        assertEquals("At least one participant other than the creator is required", exception.getMessage());
    }

    @Test
    @DisplayName("Group creation is refused when the creator is the only participant")
    void groupCreationIsRefusedWhenCreatorIsTheOnlyParticipant() {
        // Given a user "Alice"
        UserId alice = userId();

        // When Alice creates a group named "Dojo du mardi" with only herself as a participant
        // Then the creation is refused because at least one participant other than the creator is required
        // And no group is created
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Group.create("Dojo du mardi", alice, List.of(alice)));
        assertEquals("At least one participant other than the creator is required", exception.getMessage());
    }

    @Test
    @DisplayName("The group creator is an admin of the group")
    void groupCreatorIsAdmin() {
        // Given a user "Alice"
        UserId alice = userId();
        // And another user "Bob"
        UserId bob = userId();

        // When Alice creates a group named "Dojo du mardi" with Bob as a participant
        Group group = Group.create("Dojo du mardi", alice, List.of(bob));

        // Then Alice is an admin of the group
        assertTrue(group.isAdmin(alice));
    }

    @Test
    @DisplayName("The added users are members of the group and are not admins")
    void addedUsersAreMembersAndNotAdmins() {
        // Given a user "Alice"
        UserId alice = userId();
        // And two other users "Bob" and "Chloé"
        UserId bob = userId();
        UserId chloe = userId();

        // When Alice creates a group named "Dojo du mardi" with Bob and Chloé as participants
        Group group = Group.create("Dojo du mardi", alice, List.of(bob, chloe));

        // Then Bob is a member of the group
        assertTrue(group.isMember(bob));
        // And Chloé is a member of the group
        assertTrue(group.isMember(chloe));
        // And Bob and Chloé are not admins of the group
        assertFalse(group.isAdmin(bob));
        assertFalse(group.isAdmin(chloe));
    }

    @Test
    @DisplayName("The group exposes its members with their roles")
    void groupExposesMembersWithTheirRoles() {
        // Given a user "Alice"
        UserId alice = userId();
        // And two other users "Bob" and "Chloé"
        UserId bob = userId();
        UserId chloe = userId();

        // When Alice creates a group named "Dojo du mardi" with Bob and Chloé as participants
        Group group = Group.create("Dojo du mardi", alice, List.of(bob, chloe));

        // Then the group exposes Alice as an admin, followed by Bob and Chloé as members
        assertEquals(
                List.of(
                        new GroupMember(alice, Role.ADMIN),
                        new GroupMember(bob, Role.MEMBER),
                        new GroupMember(chloe, Role.MEMBER)),
                group.members());
    }

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }
}
