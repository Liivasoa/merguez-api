package mg.msys.merguez_api.infrastructure.group;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import mg.msys.merguez_api.domain.group.Group;
import mg.msys.merguez_api.domain.group.GroupMember;
import mg.msys.merguez_api.domain.group.GroupRepository;
import mg.msys.merguez_api.domain.group.Role;
import mg.msys.merguez_api.domain.user.UserId;

@DataMongoTest
@Testcontainers
@Import(MongoGroupRepository.class)
class MongoGroupRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    GroupRepository repository;

    @Test
    @DisplayName("A saved group with a description is found by its id with its roles")
    void savedGroupWithDescriptionCanBeFoundById() {
        // Given a group with a description, created by Alice with Bob as a participant
        UserId alice = new UserId(UUID.randomUUID());
        UserId bob = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", "Weekly practice", alice, List.of(bob));

        // When the group is saved
        repository.save(group);

        // Then it is found by its id, equal to the saved one, with the same roles
        Optional<Group> found = repository.findById(group.id());
        assertEquals(Optional.of(group), found);
        assertEquals(Optional.of("Weekly practice"), found.get().description());
        assertEquals(
                List.of(new GroupMember(alice, Role.ADMIN), new GroupMember(bob, Role.MEMBER)),
                found.get().members());
    }

    @Test
    @DisplayName("A saved group without a description is found by its id with an empty description")
    void savedGroupWithoutDescriptionCanBeFoundById() {
        // Given a group without a description
        UserId alice = new UserId(UUID.randomUUID());
        UserId bob = new UserId(UUID.randomUUID());
        Group group = Group.create("Dojo du mardi", alice, List.of(bob));

        // When the group is saved
        repository.save(group);

        // Then it is found by its id with an empty description
        Optional<Group> found = repository.findById(group.id());
        assertEquals(Optional.of(group), found);
        assertEquals(Optional.empty(), found.get().description());
    }
}
