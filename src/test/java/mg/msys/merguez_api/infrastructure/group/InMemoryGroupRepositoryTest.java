package mg.msys.merguez_api.infrastructure.group;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.domain.group.Group;
import mg.msys.merguez_api.domain.user.UserId;

class InMemoryGroupRepositoryTest {

    @Test
    @DisplayName("A saved group can be found by its id")
    void savedGroupCanBeFoundById() {
        // Given a group "Dojo du mardi" created by Alice with Bob as a participant
        UserId alice = new UserId(UUID.randomUUID());
        UserId bob = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", alice, List.of(bob));
        InMemoryGroupRepository repository = new InMemoryGroupRepository();

        // When the group is saved
        repository.save(group);

        // Then the group can be found by its id
        assertEquals(Optional.of(group), repository.findById(group.id()));
    }
}
