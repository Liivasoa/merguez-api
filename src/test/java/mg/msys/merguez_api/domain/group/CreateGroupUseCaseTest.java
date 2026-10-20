package mg.msys.merguez_api.domain.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.domain.user.UserId;

class CreateGroupUseCaseTest {

    @Test
    @DisplayName("The use case creates the group and saves it through the repository")
    void useCaseCreatesAndSavesGroup() {
        // Given a creator "Alice" and a participant "Bob"
        UserId creatorId = userId();
        UserId participantId = userId();
        // And a group repository
        InMemoryGroupRepository repository = new InMemoryGroupRepository();
        CreateGroupUseCase useCase = new CreateGroupUseCase(repository);
        // And a command to create the group "Dojo du mardi" with a description and Bob as a participant
        CreateGroupCommand command = new CreateGroupCommand(
                creatorId, "Dojo du mardi", "Séances entre amis", List.of(participantId));

        // When the CreateGroup use case is executed
        Group result = useCase.execute(command);

        // Then the returned group has a generated id
        assertNotNull(result.id());
        // And the name "Dojo du mardi"
        assertEquals("Dojo du mardi", result.name());
        // And the description "Séances entre amis"
        assertEquals(Optional.of("Séances entre amis"), result.description());
        // And Alice is admin
        assertTrue(result.isAdmin(creatorId));
        // And Bob is a member
        assertTrue(result.isMember(participantId));
        // And the group is saved through the repository
        assertEquals(List.of(result), repository.saved);
    }

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }

    private static class InMemoryGroupRepository implements GroupRepository {

        private final List<Group> saved = new ArrayList<>();

        @Override
        public void save(Group group) {
            saved.add(group);
        }
    }
}
