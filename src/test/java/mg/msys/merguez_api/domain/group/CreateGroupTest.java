package mg.msys.merguez_api.domain.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import mg.msys.merguez_api.domain.user.User;

class CreateGroupTest {

    @Test
    @DisplayName("A user creates a group with a name and a participant")
    void userCreatesGroupWithNameAndParticipant() {
        // Given a user "Alice"
        User alice = new User("Alice");
        // And another user "Bob"
        User bob = new User("Bob");

        // When Alice creates a group named "Dojo du mardi" with Bob as a participant
        Group group = Group.create("Dojo du mardi", alice, List.of(bob));

        // Then the group "Dojo du mardi" is created
        assertEquals("Dojo du mardi", group.name());
    }

    @Test
    @DisplayName("A group is created with a description")
    void groupIsCreatedWithDescription() {
        // Given a user "Alice"
        User alice = new User("Alice");
        // And another user "Bob"
        User bob = new User("Bob");

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
        User alice = new User("Alice");
        // And another user "Bob"
        User bob = new User("Bob");

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
        User alice = new User("Alice");
        // And another user "Bob"
        User bob = new User("Bob");

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
        User alice = new User("Alice");
        // And another user "Bob"
        User bob = new User("Bob");

        // When Alice creates a group whose name is empty or made only of spaces and with Bob as a participant
        // Then the creation is refused because the name is required
        // And no group is created
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Group.create(blankName, alice, List.of(bob)));
        assertEquals("Group name is required", exception.getMessage());
    }
}
