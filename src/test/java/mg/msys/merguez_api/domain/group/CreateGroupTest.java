package mg.msys.merguez_api.domain.group;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
